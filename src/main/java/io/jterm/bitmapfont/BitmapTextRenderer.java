package io.jterm.bitmapfont;

import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;

/**
 * Renders strings using a {@link BitmapFont} onto a {@link ScreenBuffer}. Each
 * non-transparent cell in a character grid is written as a full-block character
 * ('█') with a color determined by its layer:
 *
 * <ul>
 *   <li>Layer 1 (base) — base color</li>
 *   <li>Layer 2 (highlight) — highlight color, optionally dithered with base</li>
 *   <li>Layer 3 (shadow) — shadow color</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * var renderer = new BitmapTextRenderer(screenBuffer);
 * renderer.draw("SALVAGE", 10, 5, font)
 *         .withBase(AnsiColor.BRIGHT_BLACK)
 *         .withHighlight(AnsiColor.BLUE)
 *         .withShadow(AnsiColor.BLACK)
 *         .withDither(0.5)
 *         .render();
 * }</pre>
 *
 * <p>The renderer uses {@link ScreenBuffer#setCell(int, int, TextCell)}
 * for each non-zero cell. Characters are drawn left to right, spaced by the
 * font's character width.
 */
public class BitmapTextRenderer {

    private static final char BLOCK = '\u2588'; // full block

    private final ScreenBuffer buffer;

    /**
     * Construct a renderer for the given ScreenBuffer.
     *
     * @param buffer the target ScreenBuffer to render onto
     */
    public BitmapTextRenderer(ScreenBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Begin a draw operation. Returns a {@link Builder} for fluent configuration
     * and rendering.
     *
     * @param text the string to render
     * @param x    the starting column (0-based, left edge of first character)
     * @param y    the starting row (0-based, top edge of all characters)
     * @param font the bitmap font to use
     * @return a Builder for configuring colors and calling render()
     */
    public Builder draw(String text, int x, int y, BitmapFont font) {
        return new Builder(buffer, text, x, y, font);
    }

    /**
     * Fluent builder for configuring a single draw operation. Obtain an instance
     * via {@link BitmapTextRenderer#draw(String, int, int, BitmapFont)}.
     */
    public static class Builder {
        private final ScreenBuffer buffer;
        private final String text;
        private final int x;
        private final int y;
        private final BitmapFont font;
        private Color base = AnsiColor.WHITE;
        private Color highlight = AnsiColor.WHITE;
        private Color shadow = AnsiColor.BLACK;
        private Color background = null;
        private double dither = 0.0;

        /**
         * Construct a draw-operation builder.
         *
         * @param buffer the target ScreenBuffer
         * @param text   the string to render
         * @param x      the starting column
         * @param y      the starting row
         * @param font   the bitmap font
         */
        Builder(ScreenBuffer buffer, String text, int x, int y, BitmapFont font) {
            this.buffer = buffer;
            this.text = text;
            this.x = x;
            this.y = y;
            this.font = font;
        }

        /**
         * Set the base (layer 1) color.
         *
         * @param color the base color
         * @return this builder
         */
        public Builder withBase(Color color) {
            this.base = color;
            return this;
        }

        /**
         * Set the highlight (layer 2) color.
         *
         * @param color the highlight color
         * @return this builder
         */
        public Builder withHighlight(Color color) {
            this.highlight = color;
            return this;
        }

        /**
         * Set the shadow (layer 3) color.
         *
         * @param color the shadow color
         * @return this builder
         */
        public Builder withShadow(Color color) {
            this.shadow = color;
            return this;
        }

        /**
         * Set the background color for written cells. If not set, the active
         * theme background is used.
         *
         * @param color the background color
         * @return this builder
         */
        public Builder withBackground(Color color) {
            this.background = color;
            return this;
        }

        /**
         * Set the dither amount for layer 2 (highlight) cells.
         *
         * @param amount 0.0 = no dither (use highlight directly), 1.0 = all highlight,
         *               0.5 = checkerboard between highlight and base
         * @return this builder
         */
        public Builder withDither(double amount) {
            this.dither = amount;
            return this;
        }

        /**
         * Execute the render: write all non-transparent cells to the ScreenBuffer.
         */
        public void render() {
            Color bg = background != null ? background : ThemeManager.active().background();

            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                int[][] grid = font.gridOf(ch);
                if (grid == null) continue;

                int charX = x + i * font.charWidth();

                for (int row = 0; row < grid.length && row < font.charHeight(); row++) {
                    for (int col = 0; col < grid[row].length && col < font.charWidth(); col++) {
                        int layer = grid[row][col];
                        if (layer == 0) continue; // transparent

                        Color fg = switch (layer) {
                            case 1 -> base;
                            case 2 -> ditherColor(row, col);
                            case 3 -> shadow;
                            default -> base;
                        };

                        buffer.setCell(
                                charX + col,
                                y + row,
                                new TextCell(BLOCK, fg, bg)
                        );
                    }
                }
            }
        }

        /**
         * Determine the color for a layer 2 (highlight) cell based on the dither setting.
         *
         * @param row the row index within the character grid
         * @param col the column index within the character grid
         * @return the color to use (highlight or base)
         */
        private Color ditherColor(int row, int col) {
            if (dither <= 0.0) {
                return highlight;
            }
            if (dither >= 1.0) {
                return highlight;
            }
            // Checkerboard: even (row+col) → highlight, odd → base
            boolean isEven = (row + col) % 2 == 0;
            return isEven ? highlight : base;
        }
    }
}