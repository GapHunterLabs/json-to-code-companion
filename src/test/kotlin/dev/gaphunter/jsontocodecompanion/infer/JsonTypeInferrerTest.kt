package dev.gaphunter.jsontocodecompanion.infer

import com.intellij.json.psi.JsonObject
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.gaphunter.jsontocodecompanion.model.InferredType

class JsonTypeInferrerTest : BasePlatformTestCase() {

    private fun parseRoot(json: String): JsonObject {
        val file = myFixture.configureByText("sample.json", json)
        return file.firstChild as JsonObject
    }

    fun testInfersPrimitiveTypes() {
        val root = parseRoot(
            """
            {
                "name": "Acme",
                "count": 3,
                "big": 5000000000,
                "price": 19.99,
                "active": true
            }
            """.trimIndent(),
        )
        val type = JsonTypeInferrer.infer(root, "Root") as InferredType.ObjectType

        assertEquals(InferredType.StringType, fieldType(type, "name"))
        assertEquals(InferredType.IntType, fieldType(type, "count"))
        assertEquals(InferredType.LongType, fieldType(type, "big"))
        assertEquals(InferredType.DoubleType, fieldType(type, "price"))
        assertEquals(InferredType.BooleanType, fieldType(type, "active"))
    }

    fun testNullBecomesUnknownNotAGuess() {
        val root = parseRoot("""{ "middleName": null }""")
        val type = JsonTypeInferrer.infer(root, "Root") as InferredType.ObjectType

        assertEquals(InferredType.Unknown, fieldType(type, "middleName"))
    }

    fun testNestedObjectBecomesItsOwnObjectType() {
        val root = parseRoot(
            """
            {
                "address": { "city": "Springfield", "zip": "12345" }
            }
            """.trimIndent(),
        )
        val type = JsonTypeInferrer.infer(root, "Root") as InferredType.ObjectType
        val nested = fieldType(type, "address") as InferredType.ObjectType

        assertEquals(2, nested.fields.size)
        assertEquals(InferredType.StringType, nested.fields.first { it.safeName == "city" }.type)
    }

    fun testArrayOfPrimitivesInfersElementType() {
        val root = parseRoot("""{ "tags": ["a", "b", "c"] }""")
        val type = JsonTypeInferrer.infer(root, "Root") as InferredType.ObjectType
        val list = fieldType(type, "tags") as InferredType.ListType

        assertEquals(InferredType.StringType, list.elementType)
    }

    fun testArrayOfSameShapeObjectsInfersASharedElementType() {
        val root = parseRoot(
            """
            { "items": [ { "id": 1, "label": "a" }, { "id": 2, "label": "b" } ] }
            """.trimIndent(),
        )
        val type = JsonTypeInferrer.infer(root, "Root") as InferredType.ObjectType
        val list = fieldType(type, "items") as InferredType.ListType

        assertTrue(list.elementType is InferredType.ObjectType)
        assertEquals(2, (list.elementType as InferredType.ObjectType).fields.size)
    }

    fun testArrayOfDifferentShapeObjectsBecomesUnknownNotAWrongGuess() {
        val root = parseRoot(
            """
            { "items": [ { "id": 1 }, { "name": "b", "extra": true } ] }
            """.trimIndent(),
        )
        val type = JsonTypeInferrer.infer(root, "Root") as InferredType.ObjectType
        val list = fieldType(type, "items") as InferredType.ListType

        assertEquals(InferredType.Unknown, list.elementType)
    }

    fun testEmptyArrayBecomesUnknownElementType() {
        val root = parseRoot("""{ "items": [] }""")
        val type = JsonTypeInferrer.infer(root, "Root") as InferredType.ObjectType
        val list = fieldType(type, "items") as InferredType.ListType

        assertEquals(InferredType.Unknown, list.elementType)
    }

    fun testKeyThatNeedsSanitizingKeepsTheOriginalKeyOnTheFieldSpec() {
        val root = parseRoot("""{ "first-name": "Ana" }""")
        val type = JsonTypeInferrer.infer(root, "Root") as InferredType.ObjectType
        val field = type.fields.first()

        assertEquals("first-name", field.originalKey)
        assertEquals("firstName", field.safeName)
    }

    private fun fieldType(type: InferredType.ObjectType, name: String): InferredType =
        type.fields.first { it.safeName == name }.type
}
