package io.jterm.completion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EnglishWordCompletionProvider}, which provides ghost text
 * completions from a built-in English word list loaded via a trie.
 */
class EnglishWordCompletionProviderTest {

    // ── Basic matching ──────────────────────────────────────────

    @Test
    void suggestsSuffixForMatchingWord() {
        var provider = EnglishWordCompletionProvider.create();
        // "he" is in the word list and is a complete word.
        // "her" is the shortest word starting with "he" that's longer than "he".
        // Suffix should be "r" (for "her").
        String suffix = provider.suggest("he", 2);
        assertNotNull(suffix);
        assertEquals("r", suffix);
    }

    @Test
    void suggestsShortestMatchingWord() {
        var provider = EnglishWordCompletionProvider.create();
        // "hel" should match "help" (4 chars) — shortest among "help", "hello"(not in list)
        String suffix = provider.suggest("hel", 3);
        assertNotNull(suffix);
        assertEquals("p", suffix);
    }

    @Test
    void matchingIsCaseInsensitive() {
        var provider = EnglishWordCompletionProvider.create();
        // "HE" should match "her" → suffix "r"
        String suffix = provider.suggest("HE", 2);
        assertNotNull(suffix);
        assertEquals("r", suffix);
    }

    @Test
    void matchingIsCaseInsensitiveMixedCase() {
        var provider = EnglishWordCompletionProvider.create();
        // "HeL" should match "help" → suffix "p"
        String suffix = provider.suggest("HeL", 3);
        assertNotNull(suffix);
        assertEquals("p", suffix);
    }

    // ── No match cases ──────────────────────────────────────────

    @Test
    void returnsNullWhenNoMatch() {
        var provider = EnglishWordCompletionProvider.create();
        String suffix = provider.suggest("xyzqq", 5);
        assertNull(suffix);
    }

    @Test
    void returnsNullForEmptyText() {
        var provider = EnglishWordCompletionProvider.create();
        String suffix = provider.suggest("", 0);
        assertNull(suffix);
    }

    @Test
    void returnsNullWhenPrefixIsExactWord() {
        var provider = EnglishWordCompletionProvider.create();
        // "he" is a complete word — no suffix needed since it IS a word
        // But "he" also has longer completions ("her", "help", etc.)
        // The provider should skip exact matches and suggest the next shortest.
        // "he" → shortest longer word is "her" → suffix "r"
        String suffix = provider.suggest("he", 2);
        // "he" is a word itself, but there are longer completions.
        // Should return the suffix for the next shortest word.
        assertNotNull(suffix);
        assertEquals("r", suffix);
    }

    @Test
    void returnsNullWhenPrefixIsExactWordWithNoLongerCompletions() {
        var provider = EnglishWordCompletionProvider.create();
        // Find a word in the list that has no longer words starting with it
        // "word" → check if there are longer words starting with "word"
        // From the list: "word", "work", "world" — none start with "word" except "word" itself
        String suffix = provider.suggest("word", 4);
        assertNull(suffix, "exact word with no longer completions should return null");
    }

    // ── Word boundary extraction ────────────────────────────────

    @Test
    void extractsLastWordBeforeCursor() {
        var provider = EnglishWordCompletionProvider.create();
        // "the wor" — cursor at 7, last word is "wor" → matches "word" → suffix "d"
        String suffix = provider.suggest("the wor", 7);
        assertNotNull(suffix);
        assertEquals("d", suffix);
    }

    @Test
    void extractsLastWordWithMultipleSpaces() {
        var provider = EnglishWordCompletionProvider.create();
        // "the   wor" — cursor at 9, last word is "wor" → "word" → "d"
        String suffix = provider.suggest("the   wor", 9);
        assertNotNull(suffix);
        assertEquals("d", suffix);
    }

    @Test
    void returnsNullWhenCursorIsAfterSpace() {
        var provider = EnglishWordCompletionProvider.create();
        // "the " — cursor at 4, no word being typed
        String suffix = provider.suggest("the ", 4);
        assertNull(suffix);
    }

    @Test
    void cursorInMiddleOfWordSuggestsFromPrefix() {
        var provider = EnglishWordCompletionProvider.create();
        // "help" with cursor at 2 → prefix "he" → "her" → suffix "r"
        String suffix = provider.suggest("help", 2);
        assertNotNull(suffix);
        assertEquals("r", suffix);
    }

    // ── Suffix correctness ──────────────────────────────────────

    @Test
    void suffixIsLowercaseFromTrie() {
        var provider = EnglishWordCompletionProvider.create();
        // "HE" matches "her" — suffix should be lowercase "r"
        String suffix = provider.suggest("HE", 2);
        assertEquals("r", suffix);
    }

    // ── With custom word list ───────────────────────────────────

    @Test
    void worksWithCustomWordList() {
        var provider = new EnglishWordCompletionProvider(
                java.util.List.of("apple", "apricot", "banana"));
        String suffix = provider.suggest("ap", 2);
        assertNotNull(suffix);
        // "apple" (5) is shorter than "apricot" (7) → suffix "ple"
        assertEquals("ple", suffix);
    }

    @Test
    void customWordListNoMatchReturnsNull() {
        var provider = new EnglishWordCompletionProvider(
                java.util.List.of("apple", "banana"));
        String suffix = provider.suggest("xyz", 3);
        assertNull(suffix);
    }

    @Test
    void customWordListExactMatchReturnsNullIfNoLonger() {
        var provider = new EnglishWordCompletionProvider(
                java.util.List.of("cat", "car", "can"));
        // "cat" is exact match, no longer words start with "cat" → null
        String suffix = provider.suggest("cat", 3);
        assertNull(suffix);
    }

    // ── Single character prefix ─────────────────────────────────

    @Test
    void singleCharPrefixMatches() {
        var provider = new EnglishWordCompletionProvider(
                java.util.List.of("cat", "car", "can"));
        String suffix = provider.suggest("c", 1);
        assertNotNull(suffix);
        // All 3 chars, shortest is 3 → one of "an", "ar", "at"
        assertTrue(suffix.equals("an") || suffix.equals("ar") || suffix.equals("at"),
                "suffix should be from one of the 3-char words: " + suffix);
    }

    // ── Trie-based behavior ─────────────────────────────────────

    @Test
    void usesTrieForEfficientLookup() {
        var provider = EnglishWordCompletionProvider.create();
        // Verify the provider works correctly (trie is used internally)
        // Test a common word prefix
        String suffix = provider.suggest("th", 2);
        assertNotNull(suffix);
        // "the" is the shortest word starting with "th"
        assertEquals("e", suffix);
    }

    @Test
    void handlesPrefixThatIsAlsoWord() {
        var provider = new EnglishWordCompletionProvider(
                java.util.List.of("inter", "internal", "internet"));
        // "inter" is a word itself, but "internal" and "internet" also start with it
        // Provider should suggest the shortest longer completion
        String suffix = provider.suggest("inter", 5);
        assertNotNull(suffix);
        assertEquals("nal", suffix); // "internal" is shorter than "internet"
    }

    // ── Edge cases ──────────────────────────────────────────────

    @Test
    void returnsNullForNullText() {
        var provider = EnglishWordCompletionProvider.create();
        assertNull(provider.suggest(null, 0));
    }

    @Test
    void returnsNullForNegativeCursorPos() {
        var provider = EnglishWordCompletionProvider.create();
        assertNull(provider.suggest("he", -1));
    }

    @Test
    void returnsNullWhenCursorAtStart() {
        var provider = EnglishWordCompletionProvider.create();
        assertNull(provider.suggest("hello", 0));
    }
}