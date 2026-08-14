package dev.gaphunter.jsontocodecompanion.model

/**
 * What a single JSON value's type maps to -- never invented past what
 * the JSON actually shows. [Unknown] covers exactly the two cases
 * where there's genuinely no type information to infer from:
 * `null`, and an empty or mixed-element-type array.
 */
sealed class InferredType {
    data object StringType : InferredType()
    data object IntType : InferredType()
    data object LongType : InferredType()
    data object DoubleType : InferredType()
    data object BooleanType : InferredType()
    data object Unknown : InferredType()
    data class ListType(val elementType: InferredType) : InferredType()
    data class ObjectType(val className: String, val fields: List<FieldSpec>) : InferredType()
}

/** [originalKey] is the raw JSON key exactly as written; [safeName] is a sanitized valid Java/Kotlin identifier derived from it. */
data class FieldSpec(val originalKey: String, val safeName: String, val type: InferredType)
