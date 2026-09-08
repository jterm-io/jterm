package io.jterm.widget;

import io.jterm.animation.AnimatedBorderEffect;
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

    /**
     * Creates a single-line box-drawing border wrapping the given component.
     *
     * @param contents the component to wrap
     * @return the new single-line border
     */
    public static Border singleLine(Component contents) {
        return new Border(contents, Border.BorderStyle.SINGLE_LINE);
    }

    /**
     * Creates a double-line box-drawing border wrapping the given component.
     *
     * @param contents the component to wrap
     * @return the new double-line border
     */
    public static Border doubleLine(Component contents) {
        return new Border(contents, Border.BorderStyle.DOUBLE_LINE);
    }

    /**
     * Creates a rounded-corner single-line border wrapping the given component.
     *
     * @param contents the component to wrap
     * @return the new rounded border
     */
    public static Border rounded(Component contents) {
        return new Border(contents, Border.BorderStyle.ROUNDED);
    }

    /**
     * Bevel-style border: top/left double, bottom/right single.
     * Uses a custom corner set derived from box-drawing symbols.
     *
     * @param contents the component to wrap
     * @return the new bevel-style border
     */
    public static Border bevel(Component contents) {
        return new Border(contents, Border.BorderStyle.BEVEL);
    }

    /**
     * No visible border; contents occupy the full bounds.
     *
     * @param contents the component to wrap
     * @return the new empty border
     */
    public static Border empty(Component contents) {
        return new Border(contents, Border.BorderStyle.EMPTY);
    }

    /**
     * Creates an animated single-line border with the given effect.
     *
     * @param contents the component to wrap
     * @param effect   the animated border effect to render with
     * @return the new animated border
     */
    public static Border animated(Component contents, AnimatedBorderEffect effect) {
        return new AnimatedBorder(contents, effect);
    }

    /**
     * Creates an animated border with the given style and effect.
     *
     * @param contents the component to wrap
     * @param style    the border style to animate
     * @param effect   the animated border effect to render with
     * @return the new animated border
     */
    public static Border animated(Component contents, Border.BorderStyle style, AnimatedBorderEffect effect) {
        return new AnimatedBorder(contents, style, effect);
    }

    /**
     * Adds the given title to an existing border, mutating it in place.
     *
     * @param border the border to add the title to
     * @param title  the title text shown in the top border line
     * @return the same border instance with the title set
     */
    public static Border titled(Border border, String title) {
        border.setTitle(title);
        return border;
    }
}
