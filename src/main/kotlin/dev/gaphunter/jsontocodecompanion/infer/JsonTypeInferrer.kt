package dev.gaphunter.jsontocodecompanion.infer

import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonBooleanLiteral
import com.intellij.json.psi.JsonNullLiteral
import com.intellij.json.psi.JsonNumberLiteral
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.JsonValue
import dev.gaphunter.jsontocodecompanion.model.FieldSpec
import dev.gaphunter.jsontocodecompanion.model.InferredType

/**
 * Walks real JSON PSI (never a hand-rolled JSON parser -- the same
 * "trust the platform's own parser" discipline as json-schema-
 * companion) and infers a type tree. Deliberately conservative: a
 * `null` value, or an array that's empty or whose elements don't all
 * infer to the same type, becomes [InferredType.Unknown] rather than
 * a guess -- the renderer turns that into an honest `Object`/`Any`
 * field with a `TODO` comment, never a type that might be wrong.
 */
object JsonTypeInferrer {

    fun infer(value: JsonValue?, suggestedName: String): InferredType = when (value) {
        null -> InferredType.Unknown
        is JsonStringLiteral -> InferredType.StringType
        is JsonBooleanLiteral -> InferredType.BooleanType
        is JsonNullLiteral -> InferredType.Unknown
        is JsonNumberLiteral -> inferNumber(value.text)
        is JsonArray -> inferArray(value, suggestedName)
        is JsonObject -> inferObject(value, suggestedName)
        else -> InferredType.Unknown
    }

    private fun inferNumber(text: String): InferredType {
        if (text.contains('.') || text.contains('e', ignoreCase = true)) return InferredType.DoubleType
        val asLong = text.toLongOrNull() ?: return InferredType.DoubleType
        return if (asLong in Int.MIN_VALUE..Int.MAX_VALUE) InferredType.IntType else InferredType.LongType
    }

    private fun inferArray(array: JsonArray, suggestedName: String): InferredType {
        val elements = array.valueList
        if (elements.isEmpty()) return InferredType.ListType(InferredType.Unknown)

        val elementTypes = elements.map { infer(it, singularize(suggestedName)) }
        val first = elementTypes.first()
        // "Same type" for objects means "same shape" (same field names+types),
        // not merely "both objects" -- two JSON objects with different keys
        // are NOT safely the same generated class.
        val allSameShape = elementTypes.all { sameShape(it, first) }
        return InferredType.ListType(if (allSameShape) first else InferredType.Unknown)
    }

    private fun sameShape(a: InferredType, b: InferredType): Boolean = when {
        a is InferredType.ObjectType && b is InferredType.ObjectType ->
            a.fields.map { it.safeName to it.type::class } == b.fields.map { it.safeName to it.type::class }
        else -> a::class == b::class
    }

    private fun inferObject(obj: JsonObject, suggestedName: String): InferredType {
        val fields = obj.propertyList.map { property ->
            val safeName = IdentifierSanitizer.toFieldName(property.name)
            val fieldType = infer(property.value, IdentifierSanitizer.toClassName(property.name))
            FieldSpec(property.name, safeName, fieldType)
        }
        return InferredType.ObjectType(suggestedName, fields)
    }

    /** Best-effort: strips a trailing "s" so a list field named "items" suggests element class name "Item". Never load-bearing -- just a nicer default name. */
    private fun singularize(name: String): String =
        if (name.length > 1 && name.endsWith("s") && !name.endsWith("ss")) name.dropLast(1) else name
}
