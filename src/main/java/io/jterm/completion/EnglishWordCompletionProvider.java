package io.jterm.completion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * A {@link CompletionProvider} backed by a {@link Trie} of English words.
 * Provides inline ghost-text completions by matching the word being typed
 * before the cursor against the trie.
 *
 * <p>Matching is case-insensitive. When multiple words match the prefix,
 * the shortest matching word is selected and its suffix is returned as the
 * suggestion. If the typed text exactly matches a word in the list and no
 * longer words share that prefix, no suggestion is returned ({@code null})
 * since the word is already complete.
 *
 * <p>The default instance, obtained via {@link #create()}, loads common
 * English words from the resource file
 * {@code /io/jterm/completion/english_words.txt}. A custom word list can be
 * provided via the constructor. Internally, a {@link Trie} is used for
 * O(k) prefix lookups (where k = prefix length), making this provider
 * efficient even with 50k+ words.
 *
 * @since 0.1.0
 */
public class EnglishWordCompletionProvider implements CompletionProvider {

    /** The trie storing all words for efficient prefix lookup. */
    private final Trie trie;

    /**
     * Creates a provider backed by the given collection of words. Words are
     * inserted into a {@link Trie} for efficient prefix-based lookup.
     *
     * @param wordList the words to use for completions
     */
    public EnglishWordCompletionProvider(Collection<String> wordList) {
        this.trie = new Trie();
        if (wordList != null) {
            for (String word : wordList) {
                if (word != null && !word.isEmpty()) {
                    trie.insert(word);
                }
            }
        }
    }

    /**
     * Creates a provider with the built-in common English word list loaded
     * from the resource file
     * {@code /io/jterm/completion/english_words.txt}.
     *
     * @return a new EnglishWordCompletionProvider with common English words
     * @throws IllegalStateException if the word list resource cannot be loaded
     */
    public static EnglishWordCompletionProvider create() {
        Trie trie = loadTrieFromResource("/io/jterm/completion/english_words.txt");
        return new EnglishWordCompletionProvider(trie);
    }

    /**
     * Internal constructor that accepts a pre-populated trie.
     *
     * @param trie the pre-populated trie to use for completions
     */
    private EnglishWordCompletionProvider(Trie trie) {
        this.trie = trie;
    }

    /**
     * Suggests a completion suffix for the word being typed at the cursor
     * position. Extracts the word prefix before the cursor, uses the trie
     * to find the shortest matching word, and returns its suffix.
     *
     * <p>If the cursor is positioned after a space (no word in progress),
     * or if no word matches, or if the prefix is already a complete word
     * with no longer completions, {@code null} is returned.
     *
     * @param text      the current text content
     * @param cursorPos the zero-based cursor position within the text
     * @return the suffix to append at the cursor, or {@code null}
     */
    @Override
    public String suggest(String text, int cursorPos) {
        if (text == null || text.isEmpty() || cursorPos <= 0) {
            return null;
        }

        // Extract the word prefix before the cursor
        int wordStart = cursorPos;
        while (wordStart > 0 && !Character.isWhitespace(text.charAt(wordStart - 1))) {
            wordStart--;
        }

        String prefix = text.substring(wordStart, cursorPos);
        if (prefix.isEmpty()) {
            return null;
        }

        String lowerPrefix = prefix.toLowerCase();

        // Use the trie to find the shortest word starting with the prefix
        String completion = trie.shortestCompletion(lowerPrefix);
        if (completion == null) {
            return null;
        }

        // If the shortest completion equals the prefix, the prefix is already
        // a complete word. Look for the next shortest (longer) completion.
        if (completion.length() == prefix.length()) {
            // Find all words with this prefix, skip exact matches, pick shortest
            List<String> all = trie.getAllWithPrefix(lowerPrefix);
            String nextShortest = null;
            for (String word : all) {
                if (word.length() > prefix.length()) {
                    if (nextShortest == null || word.length() < nextShortest.length()) {
                        nextShortest = word;
                    }
                }
            }
            if (nextShortest == null) {
                return null; // exact word, no longer completions
            }
            completion = nextShortest;
        }

        // Return the suffix after the prefix
        return completion.substring(lowerPrefix.length());
    }

    /**
     * Returns the number of words stored in this provider's trie.
     *
     * @return the word count
     */
    public int wordCount() {
        return trie.size();
    }

    // ── Resource loading ──────────────────────────────────────────

    /**
     * Loads words from a classpath resource into a trie.
     *
     * @param resourcePath the classpath resource path
     * @return a trie populated with the words from the resource
     * @throws IllegalStateException if the resource cannot be read
     */
    private static Trie loadTrieFromResource(String resourcePath) {
        Trie trie = new Trie();
        try (InputStream is = EnglishWordCompletionProvider.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Word list resource not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8));
                 Stream<String> lines = reader.lines()) {
                lines.filter(line -> !line.isBlank())
                     .forEach(trie::insert);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load word list: " + resourcePath, e);
        }
        return trie;
    }
}