package io.jterm.completion;

import java.util.Set;

/**
 * Holds the spellcheck word list and provides O(1) case-insensitive lookup.
 *
 * <p>Instances are created by {@link DictionaryLoader} and are immutable
 * after construction. The word set is normalized to lowercase at load time,
 * so {@link #isValid(String)} simply lowercases the query and checks
 * containment.
 *
 * <p>The {@link #isFallback()} flag indicates whether this dictionary was
 * loaded from the small bundled resource ({@code true}) or from the full
 * system dictionary ({@code false}). This is useful for logging at startup
 * to inform the user which dictionary is in use.
 *
 * @since 0.1.0
 */
public class SpellcheckDictionary {

    /** The set of valid words, all lowercase. */
    private final Set<String> words;

    /** Whether this dictionary was loaded from the fallback bundled resource. */
    private final boolean fallback;

    /**
     * Package-private constructor, used by {@link DictionaryLoader}.
     * Defaults {@code fallback} to {@code false}.
     *
     * @param words the set of valid words (will be stored as-is; should
     *              already be lowercased)
     */
    SpellcheckDictionary(Set<String> words) {
        this(words, false);
    }

    /**
     * Creates a dictionary from the given word set. Words should already
     * be lowercased for correct case-insensitive lookup. The resulting
     * dictionary is not marked as fallback.
     *
     * @param words the set of valid words
     * @return a new {@link SpellcheckDictionary}
     */
    public static SpellcheckDictionary of(Set<String> words) {
        return new SpellcheckDictionary(words, false);
    }

    /**
     * Package-private constructor with explicit fallback flag, used by
     * {@link DictionaryLoader}.
     *
     * @param words    the set of valid words (will be stored as-is; should
     *                 already be lowercased)
     * @param fallback whether this is the fallback (small) dictionary
     */
    SpellcheckDictionary(Set<String> words, boolean fallback) {
        this.words = words;
        this.fallback = fallback;
    }

    /**
     * Check if a word is in the dictionary. Lookup is case-insensitive.
     *
     * @param word the word to check; {@code null} or empty returns {@code false}
     * @return {@code true} if the word is in the dictionary
     */
    public boolean isValid(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        return words.contains(word.toLowerCase());
    }

    /**
     * Returns the number of words loaded into this dictionary.
     *
     * @return the word count
     */
    public int size() {
        return words.size();
    }

    /**
     * Returns whether this dictionary was loaded from the fallback bundled
     * resource rather than the full system dictionary.
     *
     * @return {@code true} if this is the fallback (small) dictionary,
     *         {@code false} if it is the full system dictionary
     */
    public boolean isFallback() {
        return fallback;
    }
}