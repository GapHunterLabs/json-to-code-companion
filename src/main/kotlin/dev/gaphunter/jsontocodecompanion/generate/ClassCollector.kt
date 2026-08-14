package dev.gaphunter.jsontocodecompanion.generate

import dev.gaphunter.jsontocodecompanion.model.InferredType

/** Flattens the type tree into one entry per distinct object shape, root first, in encounter order -- each becomes its own generated class. */
object ClassCollector {

    fun collect(root: InferredType.ObjectType): List<InferredType.ObjectType> {
        val result = mutableListOf<InferredType.ObjectType>()
        fun visit(type: InferredType) {
            when (type) {
                is InferredType.ObjectType -> {
                    result += type
                    type.fields.forEach { visit(it.type) }
                }
                is InferredType.ListType -> visit(type.elementType)
                else -> {}
            }
        }
        visit(root)
        return result
    }
}
