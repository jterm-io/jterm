package io.jterm.completion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EnglishWordCompletionProvider}, which provides ghost text
 * completions from a built-in English word list loaded via a trie.
 *
 * <p>The built-in word list contains 466k+ words. Test prefixes are chosen so
 * that the shortest completion is unique at its length — the trie's BFS uses
 * {@code HashMap} iteration order, so ties between same-length words would be
 * non-deterministic across JVM runs.
 */
class EnglishWordCompletionProviderTest {

    // ── Basic matching ──────────────────────────────────────────

    @Test
    void suggestsSuffixForMatchingWord() {
        var provider = EnglishWordCompletionProvider.create();
        // "qad" is not a word itself. "qadi" (4 chars) is the unique shortest
        // word starting with "qad". Suffix should be "i".
        String suffix = provider.suggest("qad", 3);
        assertNotNull(suffix);
        assertEquals("i", suffix);
    }

    @Test
    void suggestsShortestMatchingWord() {
        var provider = EnglishWordCompletionProvider.create();
        // "progr" — "program" (7 chars) is the unique shortest word starting
        // with "progr". Suffix should be "am".
        String suffix = provider.suggest("progr", 5);
        assertNotNull(suffix);
        assertEquals("am", suffix);
    }

    @Test
    void matchingIsCaseInsensitive() {
        var provider = EnglishWordCompletionProvider.create();
        // "QAD" should match "qadi" → suffix "i"
        String suffix = provider.suggest("QAD", 3);
        assertNotNull(suffix);
        assertEquals("i", suffix);
    }

    @Test
    void matchingIsCaseInsensitiveMixedCase() {
        var provider = EnglishWordCompletionProvider.create();
        // "PrOgR" should match "program" → suffix "am"
        String suffix = provider.suggest("PrOgR", 5);
        assertNotNull(suffix);
        assertEquals("am", suffix);
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
        // "hello" is a complete word — it has longer completions.
        // The provider should skip the exact match and suggest the next shortest.
        // "hello" → shortest longer word is "hellos" (unique at length 6) → suffix "s"
        String suffix = provider.suggest("hello", 5);
        // "hello" is a word itself, but there are longer completions.
        // Should return the suffix for the next shortest word.
        assertNotNull(suffix);
        assertEquals("s", suffix);
    }

    @Test
    void returnsNullWhenPrefixIsExactWordWithNoLongerCompletions() {
        var provider = EnglishWordCompletionProvider.create();
        // "quica" is a complete word in the list and has no longer words
        // starting with it — the provider should return null.
        String suffix = provider.suggest("quica", 5);
        assertNull(suffix, "exact word with no longer completions should return null");
    }

    // ── Word boundary extraction ────────────────────────────────

    @Test
    void extractsLastWordBeforeCursor() {
        var provider = EnglishWordCompletionProvider.create();
        // "the progr" — cursor at 9, last word is "progr" → matches "program" → suffix "am"
        String suffix = provider.suggest("the progr", 9);
        assertNotNull(suffix);
        assertEquals("am", suffix);
    }

    @Test
    void extractsLastWordWithMultipleSpaces() {
        var provider = EnglishWordCompletionProvider.create();
        // "the   progr" — cursor at 11, last word is "progr" → "program" → "am"
        String suffix = provider.suggest("the   progr", 11);
        assertNotNull(suffix);
        assertEquals("am", suffix);
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
        // "qadi" with cursor at 3 → prefix "qad" → "qadi" → suffix "i"
        String suffix = provider.suggest("qadi", 3);
        assertNotNull(suffix);
        assertEquals("i", suffix);
    }

    // ── Suffix correctness ──────────────────────────────────────

    @Test
    void suffixIsLowercaseFromTrie() {
        var provider = EnglishWordCompletionProvider.create();
        // "QAD" matches "qadi" — suffix should be lowercase "i"
        String suffix = provider.suggest("QAD", 3);
        assertEquals("i", suffix);
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
        // "progr" has a unique shortest completion "program" → suffix "am"
        String suffix = provider.suggest("progr", 5);
        assertNotNull(suffix);
        assertEquals("am", suffix);
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