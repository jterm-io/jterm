package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.style.AnsiColor;
import io.jterm.widget.Border;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Mutable context passed to {@link AnimatedBorderEffect} on each frame.
 *
 * <p>Provides accessor/mutator methods for individual border cells:
 * four corners and per-position edge overrides. Effects call
 * {@link #setCorner}, {@link #setEdge}, etc. to customize the border
 * characters before the border is rendered.</p>
 *
 * <p>Call {@link #resetToStyle()} to restore all characters to the
 * base {@link Border.BorderStyle} defaults.</p>
 */
public class BorderContext {

    /** The four corners of a rectangular border. */
    public enum Corner { TL, TR, BL, BR }

    /** The four sides of a rectangular border. */
    public enum Side { TOP, BOTTOM, LEFT, RIGHT }

    private final TerminalSize size;
    private final Border.BorderStyle style;

    private final EnumMap<Corner, String> corners = new EnumMap<>(Corner.class);
    private final Map<String, String> edges = new HashMap<>();

    /** Foreground color override for border cells, or null to use theme default. */
    private AnsiColor borderColor;

    /**
     * Creates a border context for the given size and style.
     *
     * @param size  the border dimensions
     * @param style the base border style
     */
    public BorderContext(TerminalSize size, Border.BorderStyle style) {
        this.size = size;
        this.style = style;
        resetToStyle();
    }

    /** Returns the border dimensions. */
    public TerminalSize getSize() {
        return size;
    }

    /** Returns the base border style. */
    public Border.BorderStyle getStyle() {
        return style;
    }

    // ---- Corners ----

    /** Sets a corner character. */
    public void setCorner(Corner corner, char ch) {
        corners.put(corner, String.valueOf(ch));
    }

    /** Sets a corner character (multi-codepoint safe). */
    public void setCorner(Corner corner, String ch) {
        corners.put(corner, ch);
    }

    /** Returns the current character for the given corner. */
    public String getCorner(Corner corner) {
        return corners.get(corner);
    }

    // ---- Edges ----

    /** Sets an edge character at the given position (0-based from left/top). */
    public void setEdge(Side side, int position, char ch) {
        edges.put(edgeKey(side, position), String.valueOf(ch));
    }

    /** Sets an edge character at the given position (multi-codepoint safe). */
    public void setEdge(Side side, int position, String ch) {
        edges.put(edgeKey(side, position), ch);
    }

    /** Returns the current character for the given edge position,
     *  falling back to the style default if not overridden. */
    public String getEdge(Side side, int position) {
        String override = edges.get(edgeKey(side, position));
        if (override != null) return override;
        return switch (side) {
            case TOP, BOTTOM -> style.horizontal();
            case LEFT, RIGHT -> style.vertical();
        };
    }

    // ---- Border color ----

    /** Sets the foreground color for all border cells (corners + edges).
     *  When set, the border renderer will use this color instead of
     *  the theme's border color for all perimeter cells. */
    public void setBorderColor(AnsiColor color) {
        this.borderColor = color;
    }

    /** Returns the current border foreground color override, or null if
     *  the theme default should be used. */
    public AnsiColor getBorderColor() {
        return borderColor;
    }

    // ---- Reset ----

    /** Restores all corners, edges, and border color to the base style defaults. */
    public void resetToStyle() {
        corners.put(Corner.TL, style.topLeft());
        corners.put(Corner.TR, style.topRight());
        corners.put(Corner.BL, style.bottomLeft());
        corners.put(Corner.BR, style.bottomRight());
        edges.clear();
        borderColor = null;
    }

    private static String edgeKey(Side side, int position) {
        return side.name() + ":" + position;
    }
}