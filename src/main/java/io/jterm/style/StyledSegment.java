package io.jterm.style;

import java.util.EnumSet;

/**
 * A record representing a styled range of text within a string.
 * The range is half-open: {@code [start, end)} — start is inclusive,
 * end is exclusive.
 *
 * @param start     the start index (inclusive, ≥ 0)
 * @param end       the end index (exclusive, > start)
 * @param fg        the foreground color
 * @param bg        the background color
 * @param modifiers the SGR modifiers
 */
public record StyledSegment(int start, int end, Color fg, Color bg, EnumSet<SGR> modifiers) {

    /**
     * Canonical constructor with validation.
     *
     * @param start     the start index (inclusive, must be ≥ 0)
     * @param end       the end index (exclusive, must be > start)
     * @param fg        the foreground color
     * @param bg        the background color
     * @param modifiers the SGR modifiers (defensively copied)
     * @throws IllegalArgumentException if start < 0 or end ≤ start
     */
    public StyledSegment {
        if (start < 0)
            throw new IllegalArgumentException("start must be >= 0");
        if (end <= start)
            throw new IllegalArgumentException("end must be > start");
        modifiers = modifiers == null ? EnumSet.noneOf(SGR.class) : EnumSet.copyOf(modifiers);
    }

    /**
     * Convenience constructor with default background ({@link AnsiColor#DEFAULT})
     * and no modifiers.
     *
     * @param start the start index (inclusive)
     * @param end   the end index (exclusive)
     * @param fg    the foreground color
     */
    public StyledSegment(int start, int end, Color fg) {
        this(start, end, fg, AnsiColor.DEFAULT, EnumSet.noneOf(SGR.class));
    }

    /**
     * Convenience constructor with explicit foreground and background and
     * varargs SGR modifiers.
     *
     * @param start     the start index (inclusive)
     * @param end       the end index (exclusive)
     * @param fg        the foreground color
     * @param bg        the background color
     * @param modifiers the SGR modifiers (varargs)
     */
    public StyledSegment(int start, int end, Color fg, Color bg, SGR... modifiers) {
        this(start, end, fg, bg,
                modifiers.length == 0 ? EnumSet.noneOf(SGR.class) : EnumSet.of(modifiers[0], modifiers));
    }

    /**
     * Check whether the given character index falls within this segment.
     *
     * @param index the character index to test
     * @return true if {@code start ≤ index < end}
     */
    public boolean contains(int index) {
        return index >= start && index < end;
    }

    /**
     * Apply this segment's styling to a single character, returning a
     * {@link TextCell}. If this segment's background is
     * {@link AnsiColor#DEFAULT}, the provided {@code defaultBg} is used
     * instead.
     *
     * @param c         the character to place in the cell
     * @param defaultBg the fallback background color when this segment's
     *                  bg is {@link AnsiColor#DEFAULT}
     * @return a new {@code TextCell} with the segment's styling applied
     */
    public TextCell applyTo(char c, Color defaultBg) {
        Color effectiveBg = (bg == AnsiColor.DEFAULT) ? defaultBg : bg;
        return new TextCell(c, fg, effectiveBg, modifiers.toArray(new SGR[0]));
    }
}