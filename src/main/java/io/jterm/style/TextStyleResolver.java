package io.jterm.style;

/**
 * Functional interface for resolving per-character styling overrides.
 *
 * <p>For each character in a string being drawn, the resolver is called
 * with the character index, the character itself, and the default cell
 * style. If the resolver returns a non-null {@link TextCell}, that cell
 * (with the character replaced) is used for rendering. If it returns
 * {@code null}, the default style is used.
 *
 * <p>This enables use-cases such as syntax highlighting, search-result
 * highlighting, and per-character colorization without modifying the
 * underlying widget model.
 */
@FunctionalInterface
public interface TextStyleResolver {

    /**
     * Resolve the style for a single character.
     *
     * @param charIndex    the index of the character within the string
     * @param c            the character at that index
     * @param defaultStyle the default cell style that would be used if no
     *                     override is returned
     * @return a {@link TextCell} with the desired styling (character will be
     *         replaced with {@code c}), or {@code null} to use the default
     *         style
     */
    TextCell resolveStyle(int charIndex, char c, TextCell defaultStyle);
}