package io.jterm.completion;

import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.style.TextStyleResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link TextStyleResolver} that highlights misspelled words in yellow.
 *
 * <p>Because {@link TextStyleResolver#resolveStyle(int, char, TextCell)}
 * only receives the character index (not the full text), the resolver must
 * be given the current text via {@link #setText(String)} before each
 * render. The widget's {@code drawComponent} method is responsible for
 * calling {@code setText()} with the text being rendered before calling
 * {@code drawStyledString}.
 *
 * <p>The resolver pre-computes which character ranges correspond to
 * misspelled words in {@code setText()}, then {@code resolveStyle()}
 * simply checks if the character index falls within one of those ranges.
 *
 * <p>Word boundaries are defined as sequences of ASCII letters (a–z,
 * A–Z). Apostrophes, hyphens, digits, and other non-letter characters
 * break words. Single-character "words" are always treated as valid to
 * avoid flagging individual letters and the fragments left behind by
 * apostrophe splitting (e.g. the "t" in "don't").
 *
 * @since 0.1.0
 */
public class SpellcheckResolver implements TextStyleResolver {

    /** The dictionary used for word validation. */
    private final SpellcheckDictionary dictionary;

    /** Whether spellcheck highlighting is enabled. */
    private volatile boolean enabled = true;

    /** Pre-computed misspelled ranges as [start, end) pairs. */
    private volatile List<int[]> misspelledRanges = List.of();

    /**
     * Creates a resolver backed by the given dictionary.
     *
     * @param dictionary the dictionary to use for word validation
     */
    public SpellcheckResolver(SpellcheckDictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * Returns whether spellcheck highlighting is currently enabled.
     *
     * @return {@code true} if highlighting is active
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables spellcheck highlighting. When disabled,
     * {@link #resolveStyle} always returns {@code null}.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Must be called before each render with the current text. The resolver
     * pre-computes which character ranges are misspelled so that
     * {@link #resolveStyle} can answer in O(1) per character.
     *
     * <p>Words are defined as sequences of ASCII letters (a–z, A–Z).
     * Single-character words are always treated as valid. Non-letter
     * characters (spaces, digits, apostrophes, hyphens, etc.) break words.
     *
     * @param text the text to spellcheck; {@code null} or empty clears all
     *             misspelled ranges
     */
    public void setText(String text) {
        if (text == null || text.isEmpty()) {
            misspelledRanges = List.of();
            return;
        }
        List<int[]> ranges = new ArrayList<>();
        int i = 0;
        int len = text.length();
        while (i < len) {
            // Skip non-letter characters
            if (!isLetter(text.charAt(i))) {
                i++;
                continue;
            }
            // Find the end of the word (sequence of letters)
            int start = i;
            while (i < len && isLetter(text.charAt(i))) {
                i++;
            }
            int end = i;
            int wordLen = end - start;
            // Single-character words are always valid — skip
            if (wordLen <= 1) {
                continue;
            }
            String word = text.substring(start, end);
            if (!dictionary.isValid(word)) {
                ranges.add(new int[]{start, end});
            }
        }
        misspelledRanges = List.copyOf(ranges);
    }

    /**
     * Resolves the style for a single character. If the character index
     * falls within a misspelled word range and highlighting is enabled,
     * returns a {@link TextCell} with yellow foreground. Otherwise returns
     * {@code null} (use default style).
     *
     * @param charIndex    the index of the character within the string
     * @param c            the character at that index
     * @param defaultStyle the default cell style
     * @return a {@link TextCell} with yellow foreground for misspelled
     *         characters, or {@code null} for the default style
     */
    @Override
    public TextCell resolveStyle(int charIndex, char c, TextCell defaultStyle) {
        if (!enabled) {
            return null;
        }
        if (isMisspelled(charIndex)) {
            return defaultStyle.withForeground(AnsiColor.YELLOW);
        }
        return null;
    }

    /**
     * Returns {@code true} if the given character index is within a
     * misspelled word range.
     *
     * @param charIndex the character index to check
     * @return {@code true} if the character is part of a misspelled word
     */
    public boolean isMisspelled(int charIndex) {
        for (int[] range : misspelledRanges) {
            if (charIndex >= range[0] && charIndex < range[1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the list of misspelled word ranges. Each entry is a
     * {@code [start, end)} pair of character indices.
     *
     * @return an unmodifiable list of misspelled ranges (never {@code null})
     */
    public List<int[]> getMisspelledRanges() {
        return misspelledRanges;
    }

    /**
     * Checks if a character is an ASCII letter (a–z or A–Z).
     *
     * @param c the character to check
     * @return {@code true} if the character is an ASCII letter
     */
    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}