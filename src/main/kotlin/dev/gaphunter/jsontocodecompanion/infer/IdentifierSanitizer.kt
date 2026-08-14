package dev.gaphunter.jsontocodecompanion.infer

/**
 * Turns an arbitrary JSON key (`"first-name"`, `"123 count"`, `"OK"`)
 * into a valid camelCase Java/Kotlin identifier. Never returns an
 * empty or invalid identifier -- an input with no usable characters
 * at all falls back to `"field"`.
 */
object IdentifierSanitizer {

    // Zero-width boundary between a lowercase/digit and a following
    // uppercase letter -- splits an ALREADY camelCase key ("firstName")
    // into its real words too, not just keys with explicit separators.
    // Real bug found live: without this, "firstName" was treated as one
    // word (no separator character to split on) and fully lowercased to
    // "firstname", silently losing the internal capitalization.
    private val CAMEL_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])")

    fun toFieldName(key: String): String {
        val roughWords = key.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
        val words = roughWords.flatMap { it.split(CAMEL_BOUNDARY) }.filter { it.isNotEmpty() }
        if (words.isEmpty()) return "field"

        val camel = buildString {
            append(words.first().lowercase())
            for (word in words.drop(1)) {
                append(word.lowercase().replaceFirstChar { it.uppercaseChar() })
            }
        }
        return if (camel.first().isDigit()) "field$camel" else camel
    }

    fun toClassName(key: String): String {
        val field = toFieldName(key)
        return field.replaceFirstChar { it.uppercaseChar() }
    }
}
