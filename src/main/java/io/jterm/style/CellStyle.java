package io.jterm.style;

/**
 * Per-cell visual style: foreground color, background color, and SGR modifiers.
 * Immutable. {@code null} fg/bg means "use theme default". Lighter weight than
 * {@link TextCell} — no character content, just styling.
 *
 * <p>Static constants provide commonly-used styles:
 * <ul>
 *   <li>{@link #DEFAULT} — null fg/bg (use theme colors)</li>
 *   <li>{@link #GREEN} — bright green foreground</li>
 *   <li>{@link #RED} — bright red foreground</li>
 *   <li>{@link #BOLD} — bold SGR modifier, default colors</li>
 * </ul>
 *
 * @param fg        foreground color, or {@code null} to use the theme default
 * @param bg        background color, or {@code null} to use the theme default
 * @param modifiers SGR modifiers to apply (never {@code null} after construction)
 */
public record CellStyle(Color fg, Color bg, SGR... modifiers) {

    /** Use theme default colors, no modifiers. */
    public static final CellStyle DEFAULT = new CellStyle(null, null);
    /** Bright green foreground, default background. */
    public static final CellStyle GREEN = new CellStyle(AnsiColor.BRIGHT_GREEN, null);
    /** Bright red foreground, default background. */
    public static final CellStyle RED = new CellStyle(AnsiColor.BRIGHT_RED, null);
    /** Bold modifier, default colors. */
    public static final CellStyle BOLD = new CellStyle(null, null, SGR.BOLD);

    /**
     * Compact constructor — defensively copies the modifiers varargs array so
     * callers cannot mutate the record's internal state after construction.
     */
    public CellStyle {
        modifiers = modifiers == null ? new SGR[0] : modifiers.clone();
    }

    /**
     * Returns a defensive copy of the modifiers array so callers cannot mutate
     * the record's internal state via the returned array.
     *
     * @return a defensive copy of the modifiers array
     */
    @Override
    public SGR[] modifiers() {
        return modifiers.clone();
    }
}