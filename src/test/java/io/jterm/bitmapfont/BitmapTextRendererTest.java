package io.jterm.bitmapfont;

import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

/**
 * Tests for {@link BitmapTextRenderer} — rendering bitmap fonts onto a ScreenBuffer.
 */
class BitmapTextRendererTest {

    /**
     * Helper: create a ScreenBuffer of the given size filled with empty cells.
     *
     * @param cols the number of columns
     * @param rows the number of rows
     * @return a fresh ScreenBuffer
     */
    private ScreenBuffer newBuffer(int cols, int rows) {
        return new ScreenBuffer(new TerminalSize(cols, rows));
    }

    /**
     * Test that the renderer writes base (layer 1) cells with the base color.
     *
     * @throws IOException if the font resource cannot be loaded
     */
    @Test
    @DisplayName("Renderer writes base layer cells with base color")
    void writesBaseLayerCells() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        var buf = newBuffer(20, 20);
        var renderer = new BitmapTextRenderer(buf);

        renderer.draw("B", 5, 5, font)
                .withBase(AnsiColor.GREEN)
                .withHighlight(AnsiColor.BLUE)
                .withShadow(AnsiColor.RED)
                .withDither(0.0)
                .render();

        // 'B' grid is: 010 / 111 / 010
        // Center cell (1,1) is base layer
        var center = buf.getCell(5 + 1, 5 + 1);
        assertTrue(center.is('\u2588'), "Center cell should be full block char");
        assertEquals(AnsiColor.GREEN, center.fg(), "Center cell fg should be base color");
    }

    /**
     * Test that transparent cells (0) are not written to the buffer.
     *
     * @throws IOException if the font resource cannot be loaded
     */
    @Test
    @DisplayName("Renderer does not write transparent cells")
    void skipsTransparentCells() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        var buf = newBuffer(20, 20);
        var renderer = new BitmapTextRenderer(buf);

        renderer.draw("B", 5, 5, font)
                .withBase(AnsiColor.GREEN)
                .withHighlight(AnsiColor.BLUE)
                .withShadow(AnsiColor.RED)
                .withDither(0.0)
                .render();

        // 'B' grid is: 010 / 111 / 010
        // Corner (0,0) is transparent
        var corner = buf.getCell(5 + 0, 5 + 0);
        assertFalse(corner.is('\u2588'), "Corner cell should not be block char");
    }

    /**
     * Test that shadow cells (layer 3) get the shadow color.
     *
     * @throws IOException if the font resource cannot be loaded
     */
    @Test
    @DisplayName("Renderer writes shadow layer cells with shadow color")
    void writesShadowLayerCells() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        var buf = newBuffer(20, 20);
        var renderer = new BitmapTextRenderer(buf);

        renderer.draw("A", 5, 5, font)
                .withBase(AnsiColor.GREEN)
                .withHighlight(AnsiColor.BLUE)
                .withShadow(AnsiColor.RED)
                .withDither(0.0)
                .render();

        // 'A' grid is: 111 / 121 / 113
        // Bottom-right (2,2) is layer 3 (shadow)
        var shadowCell = buf.getCell(5 + 2, 5 + 2);
        assertTrue(shadowCell.is('\u2588'), "Shadow cell should be full block char");
        assertEquals(AnsiColor.RED, shadowCell.fg(), "Shadow cell fg should be shadow color");
    }

    /**
     * Test that highlight cells (layer 2) with dither == 0 use highlight color directly.
     *
     * @throws IOException if the font resource cannot be loaded
     */
    @Test
    @DisplayName("Renderer writes highlight cells with highlight color when dither=0")
    void writesHighlightCellsNoDither() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        var buf = newBuffer(20, 20);
        var renderer = new BitmapTextRenderer(buf);

        renderer.draw("A", 5, 5, font)
                .withBase(AnsiColor.GREEN)
                .withHighlight(AnsiColor.BLUE)
                .withShadow(AnsiColor.RED)
                .withDither(0.0)
                .render();

        // 'A' grid is: 111 / 121 / 113
        // Center (1,1) is layer 2 (highlight)
        var highlightCell = buf.getCell(5 + 1, 5 + 1);
        assertTrue(highlightCell.is('\u2588'), "Highlight cell should be full block char");
        assertEquals(AnsiColor.BLUE, highlightCell.fg(), "Highlight cell fg should be highlight color");
    }

    /**
     * Test that dithering produces a checkerboard pattern for layer 2 cells.
     * With dither 0.5, even (row+col) cells get highlight, odd get base.
     *
     * @throws IOException if the font resource cannot be loaded
     */
    @Test
    @DisplayName("Renderer dithers highlight cells in checkerboard pattern")
    void dithersHighlightCells() throws IOException {
        // Build a font where all cells are layer 2 (highlight) — 2x2 grid
        var font = BitmapFont.loadResource("bitmapfonts/dither_test.jfont");
        var buf = newBuffer(20, 20);
        var renderer = new BitmapTextRenderer(buf);

        renderer.draw("A", 5, 5, font)
                .withBase(AnsiColor.GREEN)
                .withHighlight(AnsiColor.BLUE)
                .withShadow(AnsiColor.RED)
                .withDither(0.5)
                .render();

        // 2x2 all-highlight grid. With dither 0.5:
        // (0,0) → even → highlight
        // (0,1) → odd → base
        // (1,0) → odd → base
        // (1,1) → even → highlight
        var cell00 = buf.getCell(5 + 0, 5 + 0);
        var cell01 = buf.getCell(5 + 0, 5 + 1);
        var cell10 = buf.getCell(5 + 1, 5 + 0);
        var cell11 = buf.getCell(5 + 1, 5 + 1);

        assertEquals(AnsiColor.BLUE, cell00.fg(), "Even cell (0,0) should get highlight");
        assertEquals(AnsiColor.GREEN, cell01.fg(), "Odd cell (0,1) should get base");
        assertEquals(AnsiColor.GREEN, cell10.fg(), "Odd cell (1,0) should get base");
        assertEquals(AnsiColor.BLUE, cell11.fg(), "Even cell (1,1) should get highlight");
    }

    /**
     * Test that text is positioned correctly starting at the given x,y offset.
     *
     * @throws IOException if the font resource cannot be loaded
     */
    @Test
    @DisplayName("Renderer positions text at given offset")
    void positionsTextCorrectly() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        var buf = newBuffer(20, 20);
        var renderer = new BitmapTextRenderer(buf);

        renderer.draw("A", 7, 3, font)
                .withBase(AnsiColor.GREEN)
                .withHighlight(AnsiColor.BLUE)
                .withShadow(AnsiColor.RED)
                .withDither(0.0)
                .render();

        // 'A' grid is: 111 / 121 / 113
        // Top-left (0,0) is base
        var topLeft = buf.getCell(7, 3);
        assertTrue(topLeft.is('\u2588'), "Top-left cell at (7,3) should be block char");
        assertEquals(AnsiColor.GREEN, topLeft.fg(), "Top-left cell should be base color");
    }

    /**
     * Test that multiple characters are spaced by the font's character width.
     *
     * @throws IOException if the font resource cannot be loaded
     */
    @Test
    @DisplayName("Renderer spaces multiple characters by font width")
    void spacesCharactersByFontWidth() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        var buf = newBuffer(20, 20);
        var renderer = new BitmapTextRenderer(buf);

        renderer.draw("AB", 0, 0, font)
                .withBase(AnsiColor.GREEN)
                .withHighlight(AnsiColor.BLUE)
                .withShadow(AnsiColor.RED)
                .withDither(0.0)
                .render();

        // 'A' at x=0, 'B' at x=3 (font width is 3)
        // 'A' top-left (0,0) is base
        var aTopLeft = buf.getCell(0, 0);
        assertTrue(aTopLeft.is('\u2588'), "A top-left should be block char");

        // 'B' top-left (3,0) is transparent (B grid: 010/111/010)
        // 'B' center (4,1) is base
        var bCenter = buf.getCell(3 + 1, 0 + 1);
        assertTrue(bCenter.is('\u2588'), "B center should be block char");
        assertEquals(AnsiColor.GREEN, bCenter.fg(), "B center should be base color");
    }

    /**
     * Test that space characters leave the buffer untouched (all cells transparent).
     *
     * @throws IOException if the font resource cannot be loaded
     */
    @Test
    @DisplayName("Renderer renders space as transparent (no cells written)")
    void rendersSpaceAsTransparent() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        var buf = newBuffer(20, 20);
        var renderer = new BitmapTextRenderer(buf);

        renderer.draw(" ", 5, 5, font)
                .withBase(AnsiColor.GREEN)
                .withHighlight(AnsiColor.BLUE)
                .withShadow(AnsiColor.RED)
                .withDither(0.0)
                .render();

        // No cells should be written — all should be empty
        for (int r = 5; r < 5 + font.charHeight(); r++) {
            for (int c = 5; c < 5 + font.charWidth(); c++) {
                var cell = buf.getCell(c, r);
                assertFalse(cell.is('\u2588'), "Space should not write any block chars");
            }
        }
    }

    /**
     * Test that the background color is applied to written cells.
     *
     * @throws IOException if the font resource cannot be loaded
     */
    @Test
    @DisplayName("Renderer applies background color to written cells")
    void appliesBackground() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        var buf = newBuffer(20, 20);
        var renderer = new BitmapTextRenderer(buf);

        renderer.draw("A", 5, 5, font)
                .withBase(AnsiColor.GREEN)
                .withHighlight(AnsiColor.BLUE)
                .withShadow(AnsiColor.RED)
                .withBackground(AnsiColor.BLACK)
                .withDither(0.0)
                .render();

        var topLeft = buf.getCell(5, 5);
        assertEquals(AnsiColor.BLACK, topLeft.bg(), "Background should be applied to written cells");
    }

    /**
     * Test that draw() returns the builder for chaining.
     *
     * @throws IOException if the font resource cannot be loaded
     */
    @Test
    @DisplayName("draw returns builder for chaining")
    void drawReturnsBuilder() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        var buf = newBuffer(20, 20);
        var renderer = new BitmapTextRenderer(buf);

        var builder = renderer.draw("A", 0, 0, font);
        assertNotNull(builder, "draw should return a builder");
        // Verify builder allows chaining and returns same builder
        var chained = builder.withBase(AnsiColor.GREEN);
        assertSame(builder, chained, "withBase should return same builder for chaining");
    }

    /**
     * Test that full dither (1.0) makes all layer 2 cells use highlight color.
     *
     * @throws IOException if the font resource cannot be loaded
     */
    @Test
    @DisplayName("Full dither (1.0) makes all highlight cells use highlight color")
    void fullDitherAllHighlight() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/dither_test.jfont");
        var buf = newBuffer(20, 20);
        var renderer = new BitmapTextRenderer(buf);

        renderer.draw("A", 5, 5, font)
                .withBase(AnsiColor.GREEN)
                .withHighlight(AnsiColor.BLUE)
                .withShadow(AnsiColor.RED)
                .withDither(1.0)
                .render();

        // All 4 cells should be highlight color
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                var cell = buf.getCell(5 + c, 5 + r);
                assertEquals(AnsiColor.BLUE, cell.fg(),
                        "Cell (" + c + "," + r + ") should be highlight with full dither");
            }
        }
    }

    /**
     * Test that zero dither (0.0) makes all layer 2 cells use highlight color directly.
     *
     * @throws IOException if the font resource cannot be loaded
     */
    @Test
    @DisplayName("Zero dither (0.0) makes all highlight cells use highlight color")
    void zeroDitherAllHighlight() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/dither_test.jfont");
        var buf = newBuffer(20, 20);
        var renderer = new BitmapTextRenderer(buf);

        renderer.draw("A", 5, 5, font)
                .withBase(AnsiColor.GREEN)
                .withHighlight(AnsiColor.BLUE)
                .withShadow(AnsiColor.RED)
                .withDither(0.0)
                .render();

        // All 4 cells should be highlight color (no dither = direct highlight)
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                var cell = buf.getCell(5 + c, 5 + r);
                assertEquals(AnsiColor.BLUE, cell.fg(),
                        "Cell (" + c + "," + r + ") should be highlight with zero dither");
            }
        }
    }
}