package dev.gaphunter.jsontocodecompanion.generate

import com.intellij.json.psi.JsonObject
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.gaphunter.jsontocodecompanion.infer.JsonTypeInferrer
import dev.gaphunter.jsontocodecompanion.model.InferredType

/**
 * The actual product claim under test: every rendered scenario below
 * is fed straight into [InMemoryValidator] and must come back clean --
 * same discipline as every other code-generating plugin in this
 * catalog (Bean Copy Companion's `CopierWriterTest`, Test Scaffold
 * Companion's validator test).
 */
class CodeRendererTest : BasePlatformTestCase() {

    private fun rootType(json: String, className: String): InferredType.ObjectType {
        val file = myFixture.configureByText("sample.json", json)
        val root = file.firstChild as JsonObject
        return JsonTypeInferrer.infer(root, className) as InferredType.ObjectType
    }

    fun testJavaOutputIsValidForANestedShapeWithAListAndAnUnknownField() {
        val type = rootType(
            """
            {
                "name": "Acme",
                "count": 3,
                "active": true,
                "middleName": null,
                "tags": ["a", "b"],
                "address": { "city": "Springfield" }
            }
            """.trimIndent(),
            "Person",
        )
        val text = CodeRenderer.renderJava(type, "com.example")
        val error = InMemoryValidator.findFirstSyntaxError(project, text, "Person.java")

        assertNull("expected no syntax error, got: $error\n\n$text", error)
        assertTrue(text.contains("public class Person"))
        assertTrue(text.contains("public static class Address"))
        assertTrue(text.contains("List<String> tags"))
        assertTrue(text.contains("Object middleName"))
        assertTrue(text.contains("TODO(json-to-code)"))
    }

    fun testKotlinOutputIsValidForANestedShapeWithAListAndAnUnknownField() {
        val type = rootType(
            """
            {
                "name": "Acme",
                "count": 3,
                "tags": ["a", "b"],
                "address": { "city": "Springfield" }
            }
            """.trimIndent(),
            "Person",
        )
        val text = CodeRenderer.renderKotlin(type, "com.example")
        val error = InMemoryValidator.findFirstSyntaxError(project, text, "Person.kt")

        assertNull("expected no syntax error, got: $error\n\n$text", error)
        assertTrue(text.contains("data class Person("))
        assertTrue(text.contains("data class Address("))
        assertTrue(text.contains("List<String>"))
    }

    fun testArrayOfObjectsProducesAValidListOfAGeneratedElementClass() {
        val type = rootType(
            """{ "items": [ { "id": 1, "label": "a" }, { "id": 2, "label": "b" } ] }""",
            "Root",
        )
        val javaText = CodeRenderer.renderJava(type, null)
        val kotlinText = CodeRenderer.renderKotlin(type, null)

        assertNull(InMemoryValidator.findFirstSyntaxError(project, javaText, "Root.java"))
        assertNull(InMemoryValidator.findFirstSyntaxError(project, kotlinText, "Root.kt"))
    }

    fun testSanitizedFieldNameKeepsTheOriginalKeyInAComment() {
        val type = rootType("""{ "first-name": "Ana" }""", "Root")
        val text = CodeRenderer.renderJava(type, null)

        assertNull(InMemoryValidator.findFirstSyntaxError(project, text, "Root.java"))
        assertTrue(text.contains("firstName"))
        assertTrue(text.contains("JSON key: \"first-name\""))
    }
}
