package io.jterm.completion;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SpellcheckDictionary}.
 */
class SpellcheckDictionaryTest {

    @Test
    void isValidReturnsTrueForKnownWord() {
        var dict = new SpellcheckDictionary(Set.of("hello", "world"));
        assertTrue(dict.isValid("hello"));
    }

    @Test
    void isValidIsCaseInsensitive() {
        var dict = new SpellcheckDictionary(Set.of("hello"));
        assertTrue(dict.isValid("HELLO"));
        assertTrue(dict.isValid("Hello"));
        assertTrue(dict.isValid("hElLo"));
    }

    @Test
    void isValidReturnsFalseForUnknownWord() {
        var dict = new SpellcheckDictionary(Set.of("hello", "world"));
        assertFalse(dict.isValid("xyzqwf"));
    }

    @Test
    void sizeReturnsCorrectCount() {
        var dict = new SpellcheckDictionary(Set.of("hello", "world", "foo"));
        assertEquals(3, dict.size());
    }

    @Test
    void isValidReturnsFalseForEmptyString() {
        var dict = new SpellcheckDictionary(Set.of("hello"));
        assertFalse(dict.isValid(""));
    }

    @Test
    void isValidReturnsFalseForNull() {
        var dict = new SpellcheckDictionary(Set.of("hello"));
        assertFalse(dict.isValid(null));
    }

    @Test
    void isFallbackFalseByDefault() {
        var dict = new SpellcheckDictionary(Set.of("hello"));
        assertFalse(dict.isFallback());
    }
}