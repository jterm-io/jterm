package io.jterm.widget.model;

import io.jterm.style.CellStyle;

import java.util.function.Function;

/**
 * Immutable column definition for {@code DataGrid<T>}. Describes how to extract
 * a value from a row of type {@code T}, how to format and align it, and how to
 * style it per-cell.
 *
 * @param header    column header text
 * @param accessor  function extracting the raw cell value from a row
 * @param alignment LEFT, CENTER, RIGHT, or AUTO (resolves to LEFT as fallback)
 * @param format    printf format string (e.g. {@code "$%,.2f"}, {@code "%.3f"});
 *                  {@code null} means use {@link Object#toString}
 * @param minWidth  minimum column width in terminal columns (0 = auto)
 * @param maxWidth  maximum column width (0 = unlimited)
 * @param styler    optional per-cell styler; {@code null} means use theme colors
 * @param <T>       the row type
 */
public record GridColumn<T>(
        String header,
        Function<T, Object> accessor,
        Alignment alignment,
        String format,
        int minWidth,
        int maxWidth,
        Function<Object, CellStyle> styler
) {

    /**
     * Column alignment options.
     */
    public enum Alignment {
        /** Left-align the cell value. */
        LEFT,
        /** Center the cell value. */
        CENTER,
        /** Right-align the cell value. */
        RIGHT,
        /** Resolve automatically; falls back to LEFT. */
        AUTO
    }

    // ---- Factory methods ---------------------------------------------------

    /**
     * Text column — LEFT alignment, null format (uses Object.toString).
     *
     * @param header   column header text
     * @param accessor function extracting the display string from a row
     * @param <T>      the row type
     * @return a new text column definition
     */
    public static <T> GridColumn<T> text(String header, Function<T, String> accessor) {
        return new GridColumn<>(header, accessor::apply, Alignment.LEFT, null, 0, 0, null);
    }

    /**
     * Integer column — RIGHT alignment, format {@code "%d"}.
     *
     * @param header   column header text
     * @param accessor function extracting the integer value from a row
     * @param <T>      the row type
     * @return a new integer column definition
     */
    public static <T> GridColumn<T> intCol(String header, Function<T, Integer> accessor) {
        return new GridColumn<>(header, accessor::apply, Alignment.RIGHT, "%d", 0, 0, null);
    }

    /**
     * Double column — RIGHT alignment, caller-supplied format string.
     *
     * @param header   column header text
     * @param format   printf format string applied to the double value
     * @param accessor function extracting the double value from a row
     * @param <T>      the row type
     * @return a new double column definition
     */
    public static <T> GridColumn<T> doubleCol(String header, String format, Function<T, Double> accessor) {
        return new GridColumn<>(header, accessor::apply, Alignment.RIGHT, format, 0, 0, null);
    }

    /**
     * Generic column — LEFT alignment, null format (uses Object.toString).
     *
     * @param header   column header text
     * @param accessor function extracting the raw cell value from a row
     * @param <T>      the row type
     * @return a new generic column definition
     */
    public static <T> GridColumn<T> column(String header, Function<T, Object> accessor) {
        return new GridColumn<>(header, accessor, Alignment.LEFT, null, 0, 0, null);
    }

    // ---- Builder-style copy methods ---------------------------------------

    /**
     * Returns a new GridColumn with the specified alignment, otherwise unchanged.
     *
     * @param a the alignment to use
     * @return a copy of this column with the new alignment
     */
    public GridColumn<T> withAlignment(Alignment a) {
        return new GridColumn<>(header, accessor, a, format, minWidth, maxWidth, styler);
    }

    /**
     * Returns a new GridColumn with the specified styler, otherwise unchanged.
     *
     * @param s the per-cell styler; {@code null} means use theme colors
     * @return a copy of this column with the new styler
     */
    public GridColumn<T> withStyler(Function<Object, CellStyle> s) {
        return new GridColumn<>(header, accessor, alignment, format, minWidth, maxWidth, s);
    }

    /**
     * Returns a new GridColumn with the specified minimum width, otherwise unchanged.
     *
     * @param w the minimum width in terminal columns (0 = auto)
     * @return a copy of this column with the new minimum width
     */
    public GridColumn<T> withMinWidth(int w) {
        return new GridColumn<>(header, accessor, alignment, format, w, maxWidth, styler);
    }

    /**
     * Returns a new GridColumn with the specified maximum width, otherwise unchanged.
     *
     * @param w the maximum width in terminal columns (0 = unlimited)
     * @return a copy of this column with the new maximum width
     */
    public GridColumn<T> withMaxWidth(int w) {
        return new GridColumn<>(header, accessor, alignment, format, minWidth, w, styler);
    }

    // ---- Alignment resolution ----------------------------------------------

    /**
     * Resolves the effective alignment. Explicit LEFT/CENTER/RIGHT values are
     * returned as-is. AUTO resolves to LEFT as a fallback (numeric columns
     * should set alignment explicitly to RIGHT via their factory methods).
     *
     * @return the resolved alignment (never AUTO)
     */
    public Alignment resolveAlignment() {
        return alignment == Alignment.AUTO ? Alignment.LEFT : alignment;
    }
}