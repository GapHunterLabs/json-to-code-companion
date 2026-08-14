package dev.gaphunter.jsontocodecompanion.infer

import junit.framework.TestCase

class IdentifierSanitizerTest : TestCase() {

    fun testLeavesAnAlreadyValidCamelCaseKeyAlone() {
        assertEquals("firstName", IdentifierSanitizer.toFieldName("firstName"))
    }

    fun testSanitizesAHyphenatedKey() {
        assertEquals("firstName", IdentifierSanitizer.toFieldName("first-name"))
    }

    fun testSanitizesASpaceSeparatedKey() {
        assertEquals("firstName", IdentifierSanitizer.toFieldName("first name"))
    }

    fun testPrefixesAKeyThatStartsWithADigit() {
        val result = IdentifierSanitizer.toFieldName("123abc")
        assertFalse(result.first().isDigit())
        assertTrue(result.contains("123"))
    }

    fun testFallsBackToFieldForAKeyWithNoUsableCharacters() {
        assertEquals("field", IdentifierSanitizer.toFieldName("---"))
    }

    fun testClassNameIsCapitalized() {
        assertEquals("FirstName", IdentifierSanitizer.toClassName("first-name"))
    }
}
