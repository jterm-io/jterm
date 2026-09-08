package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.style.AnsiColor;
import io.jterm.widget.Border;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;

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
    public enum Corner {
        /** Top-left corner. */
        TL,
        /** Top-right corner. */
        TR,
        /** Bottom-left corner. */
        BL,
        /** Bottom-right corner. */
        BR
    }

    /** The four sides of a rectangular border. */
    public enum Side {
        /** Top edge. */
        TOP,
        /** Bottom edge. */
        BOTTOM,
        /** Left edge. */
        LEFT,
        /** Right edge. */
        RIGHT
    }

    private final TerminalSize size;
    private final Border.BorderStyle style;

    private final EnumMap<Corner, String> corners = new EnumMap<>(Corner.class);
    private final Map<String, String> edges = new HashMap<>();

    /** Per-corner foreground color overrides, or null for theme/border default. */
    private final EnumMap<Corner, AnsiColor> cornerColors = new EnumMap<>(Corner.class);

    /** Per-edge foreground color overrides, or null for theme/border default. */
    private final Map<String, AnsiColor> edgeColors = new HashMap<>();

    /** Foreground color override for all border cells, or null to use theme default. */
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

    /**
     * Returns the border dimensions.
     *
     * @return the border dimensions
     */
    public TerminalSize getSize() {
        return size;
    }

    /**
     * Returns the base border style.
     *
     * @return the base border style
     */
    public Border.BorderStyle getStyle() {
        return style;
    }

    // ---- Corners ----

    /**
     * Sets a corner character.
     *
     * @param corner the corner to set
     * @param ch     the replacement character
     */
    public void setCorner(Corner corner, char ch) {
        corners.put(corner, String.valueOf(ch));
    }

    /**
     * Sets a corner character (multi-codepoint safe).
     *
     * @param corner the corner to set
     * @param ch     the replacement character (may be multi-codepoint)
     */
    public void setCorner(Corner corner, String ch) {
        corners.put(corner, ch);
    }

    /**
     * Sets a corner character with a per-cell foreground color override.
     *
     * @param corner the corner to set
     * @param ch     the replacement character
     * @param color  foreground color for this corner
     */
    public void setCorner(Corner corner, char ch, AnsiColor color) {
        corners.put(corner, String.valueOf(ch));
        cornerColors.put(corner, color);
    }

    /**
     * Sets a corner character with a per-cell foreground color override (multi-codepoint safe).
     *
     * @param corner the corner to set
     * @param ch     the replacement character (may be multi-codepoint)
     * @param color  foreground color for this corner
     */
    public void setCorner(Corner corner, String ch, AnsiColor color) {
        corners.put(corner, ch);
        cornerColors.put(corner, color);
    }

    /**
     * Returns the current character for the given corner.
     *
     * @param corner the corner to read
     * @return the current corner character
     */
    public String getCorner(Corner corner) {
        return corners.get(corner);
    }

    /**
     * Returns the per-cell foreground color override for the given corner, or null if none.
     *
     * @param corner the corner to read
     * @return the color override, or {@code null} if none
     */
    public AnsiColor getCornerColor(Corner corner) {
        return cornerColors.get(corner);
    }

    // ---- Edges ----

    /**
     * Sets an edge character at the given position (0-based from left/top).
     *
     * @param side     the edge the cell sits on
     * @param position 0-based offset along the edge from left/top
     * @param ch       the replacement character
     */
    public void setEdge(Side side, int position, char ch) {
        edges.put(edgeKey(side, position), String.valueOf(ch));
    }

    /**
     * Sets an edge character at the given position (multi-codepoint safe).
     *
     * @param side     the edge the cell sits on
     * @param position 0-based offset along the edge from left/top
     * @param ch       the replacement character (may be multi-codepoint)
     */
    public void setEdge(Side side, int position, String ch) {
        edges.put(edgeKey(side, position), ch);
    }

    /**
     * Sets an edge character at the given position with a per-cell foreground color override.
     *
     * @param side     the edge the cell sits on
     * @param position 0-based offset along the edge from left/top
     * @param ch       the replacement character
     * @param color    foreground color for this cell
     */
    public void setEdge(Side side, int position, char ch, AnsiColor color) {
        edges.put(edgeKey(side, position), String.valueOf(ch));
        edgeColors.put(edgeKey(side, position), color);
    }

    /**
     * Sets an edge character at the given position with a per-cell foreground color override (multi-codepoint safe).
     *
     * @param side     the edge the cell sits on
     * @param position 0-based offset along the edge from left/top
     * @param ch       the replacement character (may be multi-codepoint)
     * @param color    foreground color for this cell
     */
    public void setEdge(Side side, int position, String ch, AnsiColor color) {
        edges.put(edgeKey(side, position), ch);
        edgeColors.put(edgeKey(side, position), color);
    }

    /**
     * Returns the current character for the given edge position,
     * falling back to the style default if not overridden.
     *
     * @param side     the edge the cell sits on
     * @param position 0-based offset along the edge from left/top
     * @return the current edge character
     */
    public String getEdge(Side side, int position) {
        String override = edges.get(edgeKey(side, position));
        if (override != null) return override;
        return switch (side) {
            case TOP, BOTTOM -> style.horizontal();
            case LEFT, RIGHT -> style.vertical();
        };
    }

    /**
     * Returns the per-cell foreground color override for the given edge position, or null if none.
     *
     * @param side     the edge the cell sits on
     * @param position 0-based offset along the edge from left/top
     * @return the color override, or {@code null} if none
     */
    public AnsiColor getEdgeColor(Side side, int position) {
        return edgeColors.get(edgeKey(side, position));
    }

    // ---- Border color ----

    /**
     * Sets the foreground color for all border cells (corners + edges).
     * When set, the border renderer will use this color instead of
     * the theme's border color for all perimeter cells.
     *
     * @param color foreground color for all border cells, or {@code null} to use the theme default
     */
    public void setBorderColor(AnsiColor color) {
        this.borderColor = color;
    }

    /**
     * Returns the current border foreground color override, or null if
     * the theme default should be used.
     *
     * @return the border color override, or {@code null} if none
     */
    public AnsiColor getBorderColor() {
        return borderColor;
    }

    // ---- Reset ----

    /** Restores all corners, edges, corner colors, edge colors, and border color to the base style defaults. */
    public void resetToStyle() {
        corners.put(Corner.TL, style.topLeft());
        corners.put(Corner.TR, style.topRight());
        corners.put(Corner.BL, style.bottomLeft());
        corners.put(Corner.BR, style.bottomRight());
        edges.clear();
        cornerColors.clear();
        edgeColors.clear();
        borderColor = null;
    }

    private static String edgeKey(Side side, int position) {
        return side.name() + ":" + position;
    }
}