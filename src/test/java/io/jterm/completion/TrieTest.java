package io.jterm.completion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for the {@link Trie} data structure.
 *
 * <p>Covers: empty trie behavior, insert/contains, case insensitivity,
 * prefix validation, shortest completion (BFS correctness, exact-word
 * preference, null cases), getAllWithPrefix, size/dedup, and edge cases.
 */
class TrieTest {

    private Trie trie;

    @BeforeEach
    void setUp() {
        trie = new Trie();
    }

    // ── Empty trie ────────────────────────────────────────────────

    @Test
    void emptyTrieHasSizeZero() {
        assertEquals(0, trie.size());
    }

    @Test
    void emptyTrieContainsReturnsFalse() {
        assertFalse(trie.contains("hello"));
    }

    @Test
    void emptyTrieIsValidPrefixReturnsFalseForNonEmptyPrefix() {
        assertFalse(trie.isValidPrefix("abc"));
    }

    @Test
    void emptyTrieIsValidPrefixReturnsFalseForEmptyPrefix() {
        // Empty prefix on empty trie should be false since no words exist
        assertFalse(trie.isValidPrefix(""));
    }

    @Test
    void emptyTrieShortestCompletionReturnsNull() {
        assertNull(trie.shortestCompletion("abc"));
    }

    @Test
    void emptyTrieGetAllWithPrefixReturnsEmptyList() {
        List<String> results = trie.getAllWithPrefix("abc");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ── Insert and contains ──────────────────────────────────────

    @Test
    void insertSingleWordThenContainsReturnsTrue() {
        trie.insert("hello");
        assertTrue(trie.contains("hello"));
    }

    @Test
    void insertSingleWordDoesNotContainDifferentWord() {
        trie.insert("hello");
        assertFalse(trie.contains("world"));
    }

    @Test
    void insertMultipleWordsAllContained() {
        trie.insert("apple");
        trie.insert("banana");
        trie.insert("cherry");
        assertTrue(trie.contains("apple"));
        assertTrue(trie.contains("banana"));
        assertTrue(trie.contains("cherry"));
    }

    @Test
    void containsReturnsFalseForPrefixThatIsNotACompleteWord() {
        trie.insert("hello");
        assertFalse(trie.contains("hel"));
    }

    @Test
    void containsReturnsFalseForWordNotInsertedButPrefixMatches() {
        trie.insert("hello");
        assertFalse(trie.contains("hell"));
    }

    // ── Case insensitivity ───────────────────────────────────────

    @Test
    void insertUppercaseContainsLowercase() {
        trie.insert("HELLO");
        assertTrue(trie.contains("hello"));
    }

    @Test
    void insertLowercaseContainsUppercase() {
        trie.insert("hello");
        assertTrue(trie.contains("HELLO"));
    }

    @Test
    void insertMixedCaseContainsAllCases() {
        trie.insert("HeLLo");
        assertTrue(trie.contains("hello"));
        assertTrue(trie.contains("HELLO"));
        assertTrue(trie.contains("HeLLo"));
    }

    @Test
    void insertSameWordDifferentCasesIsDeduplicated() {
        trie.insert("hello");
        trie.insert("HELLO");
        trie.insert("Hello");
        assertEquals(1, trie.size());
    }

    // ── Prefix validation (isValidPrefix) ───────────────────────

    @Test
    void isValidPrefixReturnsTrueForExactWord() {
        trie.insert("hello");
        assertTrue(trie.isValidPrefix("hello"));
    }

    @Test
    void isValidPrefixReturnsTrueForPartialPrefix() {
        trie.insert("hello");
        assertTrue(trie.isValidPrefix("hel"));
    }

    @Test
    void isValidPrefixReturnsTrueForSingleCharacterPrefix() {
        trie.insert("hello");
        assertTrue(trie.isValidPrefix("h"));
    }

    @Test
    void isValidPrefixReturnsFalseForNonExistentPrefix() {
        trie.insert("hello");
        assertFalse(trie.isValidPrefix("xyz"));
    }

    @Test
    void isValidPrefixEmptyStringReturnsTrueWhenTrieNotEmpty() {
        trie.insert("hello");
        assertTrue(trie.isValidPrefix(""));
    }

    @Test
    void isValidPrefixIsCaseInsensitive() {
        trie.insert("hello");
        assertTrue(trie.isValidPrefix("HEL"));
        assertTrue(trie.isValidPrefix("Hel"));
    }

    // ── shortestCompletion (BFS correctness) ────────────────────

    @Test
    void shortestCompletionReturnsShortestMatchingWord() {
        trie.insert("apple");
        trie.insert("application");
        trie.insert("apricot");
        // Shortest word starting with "ap" is "apple" (5 chars)
        // apricot is 7, application is 11
        assertEquals("apple", trie.shortestCompletion("ap"));
    }

    @Test
    void shortestCompletionBFSFindsShortestAtGreaterDepth() {
        // "ab" → "abc" (3 chars) is shorter than "abcd" (4 chars)
        trie.insert("abcd");
        trie.insert("abc");
        assertEquals("abc", trie.shortestCompletion("ab"));
    }

    @Test
    void shortestCompletionPrefersExactWordMatch() {
        trie.insert("hello");
        trie.insert("help");
        // "hel" matches prefix, "hello" is a word but shorter is "help" (4) vs "hello" (5)
        // But "hel" itself is not a word
        // BFS: at depth 3 (prefix "hel"), not terminal. Children: 'l'→"hell", 'p'→"help"
        // "help" is terminal at depth 4, "hell" is not terminal (unless we insert it)
        // So shortest completion should be "help"
        String result = trie.shortestCompletion("hel");
        assertNotNull(result);
        assertEquals("help", result);
    }

    @Test
    void shortestCompletionReturnsExactWordWhenPrefixIsCompleteWord() {
        trie.insert("cat");
        trie.insert("category");
        // "cat" is a complete word and is the shortest starting with "cat"
        assertEquals("cat", trie.shortestCompletion("cat"));
    }

    @Test
    void shortestCompletionReturnsNullForNonExistentPrefix() {
        trie.insert("hello");
        assertNull(trie.shortestCompletion("xyz"));
    }

    @Test
    void shortestCompletionReturnsNullForNullPrefix() {
        trie.insert("hello");
        assertNull(trie.shortestCompletion(null));
    }

    @Test
    void shortestCompletionIsCaseInsensitive() {
        trie.insert("apple");
        trie.insert("application");
        assertEquals("apple", trie.shortestCompletion("AP"));
        assertEquals("apple", trie.shortestCompletion("Ap"));
    }

    @Test
    void shortestCompletionReturnsLowercase() {
        trie.insert("HELLO");
        assertEquals("hello", trie.shortestCompletion("he"));
    }

    @Test
    void shortestCompletionForEmptyPrefixReturnsShortestWord() {
        trie.insert("cat");
        trie.insert("elephant");
        trie.insert("be");
        // Empty prefix → BFS from root → "be" is shortest (2 chars)
        assertEquals("be", trie.shortestCompletion(""));
    }

    @Test
    void shortestCompletionFindsSingleWord() {
        trie.insert("unique");
        assertEquals("unique", trie.shortestCompletion("uni"));
    }

    @Test
    void shortestCompletionWithBranchedTrieFindsShallowestTerminal() {
        // Root → 'a' → 'b' → terminal ("ab")
        //              → 'c' → 'd' → 'e' → terminal ("acde")
        trie.insert("acde");
        trie.insert("ab");
        assertEquals("ab", trie.shortestCompletion("a"));
    }

    // ── getAllWithPrefix ─────────────────────────────────────────

    @Test
    void getAllWithPrefixReturnsAllMatchingWords() {
        trie.insert("apple");
        trie.insert("application");
        trie.insert("apricot");
        List<String> results = trie.getAllWithPrefix("ap");
        assertEquals(3, results.size());
        assertTrue(results.contains("apple"));
        assertTrue(results.contains("application"));
        assertTrue(results.contains("apricot"));
    }

    @Test
    void getAllWithPrefixReturnsExactMatch() {
        trie.insert("hello");
        trie.insert("help");
        List<String> results = trie.getAllWithPrefix("hello");
        assertEquals(1, results.size());
        assertTrue(results.contains("hello"));
    }

    @Test
    void getAllWithPrefixReturnsEmptyForNonExistentPrefix() {
        trie.insert("hello");
        List<String> results = trie.getAllWithPrefix("xyz");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getAllWithPrefixEmptyStringReturnsAllWords() {
        trie.insert("apple");
        trie.insert("banana");
        trie.insert("cherry");
        List<String> results = trie.getAllWithPrefix("");
        assertEquals(3, results.size());
        assertTrue(results.contains("apple"));
        assertTrue(results.contains("banana"));
        assertTrue(results.contains("cherry"));
    }

    @Test
    void getAllWithPrefixIsCaseInsensitive() {
        trie.insert("apple");
        trie.insert("application");
        List<String> results = trie.getAllWithPrefix("AP");
        assertEquals(2, results.size());
        assertTrue(results.contains("apple"));
    }

    @Test
    void getAllWithPrefixReturnsLowercaseWords() {
        trie.insert("HELLO");
        trie.insert("HELP");
        List<String> results = trie.getAllWithPrefix("he");
        assertTrue(results.contains("hello"));
        assertTrue(results.contains("help"));
    }

    @Test
    void getAllWithPrefixForNullReturnsEmptyList() {
        trie.insert("hello");
        List<String> results = trie.getAllWithPrefix(null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getAllWithPrefixIncludesPrefixItselfIfTerminal() {
        trie.insert("app");
        trie.insert("apple");
        List<String> results = trie.getAllWithPrefix("app");
        assertEquals(2, results.size());
        assertTrue(results.contains("app"));
        assertTrue(results.contains("apple"));
    }

    // ── size and dedup ───────────────────────────────────────────

    @Test
    void sizeReflectsNumberOfUniqueWords() {
        trie.insert("apple");
        trie.insert("banana");
        trie.insert("cherry");
        assertEquals(3, trie.size());
    }

    @Test
    void sizeDoesNotCountDuplicateInsertions() {
        trie.insert("apple");
        trie.insert("apple");
        trie.insert("apple");
        assertEquals(1, trie.size());
    }

    @Test
    void sizeDoesNotCountCaseVariantsAsDuplicates() {
        trie.insert("apple");
        trie.insert("APPLE");
        assertEquals(1, trie.size());
    }

    @Test
    void sizeZeroForEmptyTrie() {
        assertEquals(0, trie.size());
    }

    @Test
    void sizeGrowsWithNewWords() {
        assertEquals(0, trie.size());
        trie.insert("a");
        assertEquals(1, trie.size());
        trie.insert("b");
        assertEquals(2, trie.size());
        trie.insert("c");
        assertEquals(3, trie.size());
    }

    // ── Edge cases ───────────────────────────────────────────────

    @Test
    void insertNullDoesNothing() {
        trie.insert(null);
        assertEquals(0, trie.size());
        assertFalse(trie.contains(null));
    }

    @Test
    void insertEmptyString() {
        trie.insert("");
        assertEquals(1, trie.size());
        assertTrue(trie.contains(""));
    }

    @Test
    void containsNullReturnsFalse() {
        trie.insert("hello");
        assertFalse(trie.contains(null));
    }

    @Test
    void isValidPrefixNullReturnsFalse() {
        trie.insert("hello");
        assertFalse(trie.isValidPrefix(null));
    }

    @Test
    void shortestCompletionForPrefixWithNoTerminalDescendantsReturnsNull() {
        // Insert "abc" but look for prefix "abcd" — path doesn't exist
        trie.insert("abc");
        assertNull(trie.shortestCompletion("abcd"));
    }

    @Test
    void insertSingleCharacterWords() {
        trie.insert("a");
        trie.insert("b");
        trie.insert("c");
        assertTrue(trie.contains("a"));
        assertTrue(trie.contains("b"));
        assertTrue(trie.contains("c"));
        assertEquals(3, trie.size());
    }

    @Test
    void shortestCompletionForSingleCharacterWord() {
        trie.insert("a");
        trie.insert("ab");
        assertEquals("a", trie.shortestCompletion("a"));
    }
}