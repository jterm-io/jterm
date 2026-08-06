package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.util.Symbols;

/**
 * Factory for common decorative border styles.
 *
 * <p>Each border wraps a single child component and draws a one-cell-thick outline
 * using Unicode box-drawing characters. {@link #empty(Component)} is a special no-op border
 * that passes the full area through to its child.</p>
 */
public final class Borders {
    private Borders() {}

    /** Single-line box-drawing border. */
    public static Border singleLine(Component contents) {
        return new Border(contents, Border.BorderStyle.SINGLE_LINE);
    }

    /** Double-line box-drawing border. */
    public static Border doubleLine(Component contents) {
        return new Border(contents, Border.BorderStyle.DOUBLE_LINE);
    }

    /** Rounded-corner single-line border. */
    public static Border rounded(Component contents) {
        return new Border(contents, Border.BorderStyle.ROUNDED);
    }

    /**
     * Bevel-style border: top/left double, bottom/right single.
     * Uses a custom corner set derived from box-drawing symbols.
     */
    public static Border bevel(Component contents) {
        return new Border(contents, Border.BorderStyle.BEVEL);
    }

    /** No visible border; contents occupy the full bounds. */
    public static Border empty(Component contents) {
        return new Border(contents, Border.BorderStyle.EMPTY);
    }

    /** Adds the named title to an existing border. */
    public static Border titled(Border border, String title) {
        border.setTitle(title);
        return border;
    }
}
