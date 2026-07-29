package io.jterm.sprite;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SpriteSheet} — splits an ANSI art file into a grid of frames.
 */
class SpriteSheetTest {

    @Test
    @DisplayName("SpriteSheet extracts frames from grid layout")
    void extractsFramesFromGrid() throws Exception {
        // 2 columns x 2 rows of cells, each cell 2x2 chars.
        // Grid layout (each cell separated by newlines):
        //   AB\nCD  EF\nGH
        //   IJ\nKL  MN\nOP
        // We need to build a content string that, when split into a grid,
        // yields 4 cells each 2x2.
        // With cellWidth=2, cellHeight=2, cols=2, rows=2:
        // Row 0 of grid (rows 0-1 of the file):
        //   "ABEF"
        //   "CDGH"
        // Row 1 of grid (rows 2-3 of the file):
        //   "IJMN"
        //   "KLOP"
        StringBuilder content = new StringBuilder();
        content.append("ABEF\n");
        content.append("CDGH\n");
        content.append("IJMN\n");
        content.append("KLOP");
        var sheet = new SpriteSheet(content.toString(), 2, 2, 2, 2);
        assertEquals(4, sheet.frameCount());

        // Frame 0 (col 0, row 0) should be AB / CD
        String f0 = sheet.frameAt(0);
        assertEquals("AB\nCD", f0);
        // Frame 1 (col 1, row 0)
        String f1 = sheet.frameAt(1);
        assertEquals("EF\nGH", f1);
        // Frame 2 (col 0, row 1)
        String f2 = sheet.frameAt(2);
        assertEquals("IJ\nKL", f2);
        // Frame 3 (col 1, row 1)
        String f3 = sheet.frameAt(3);
        assertEquals("MN\nOP", f3);
    }

    @Test
    @DisplayName("SpriteSheet.fromFile loads a file then splits")
    void fromFileSplits() throws Exception {
        Path file = Files.createTempFile("sheet-", ".ans");
        Files.writeString(file, "ABCD\nEFGH");
        var sheet = SpriteSheet.fromFile(file, 2, 2, 2, 1);
        assertEquals(2, sheet.frameCount());
        assertEquals("AB\nEF", sheet.frameAt(0));
        assertEquals("CD\nGH", sheet.frameAt(1));
        Files.deleteIfExists(file);
    }

    @Test
    @DisplayName("toSprite builds a playable Sprite from the sheet")
    void toSpriteBuildsPlayableSprite() throws Exception {
        // 2 cells horizontally, each 2 wide and 1 tall:
        // "ABCD" => cell 0 = "AB", cell 1 = "CD"
        String content = "ABCD";
        var sheet = new SpriteSheet(content, 2, 1, 2, 1);
        var sprite = sheet.toSprite();
        assertEquals(2, sprite.getFrameCount());
        assertEquals("AB", sprite.getCurrentFrame());
        assertEquals(2, sprite.getWidth());
        assertEquals(1, sprite.getHeight());
    }

    @Test
    @DisplayName("fromContentAndDimensions handles SGR codes within cells")
    void handlesSgrCodes() {
        // Cell 0 = red A, Cell 1 = blue B
        // With cellWidth=1, cellHeight=1, cols=2, rows=1:
        // File content: "\u001B[31mA\u001B[34mB"
        String content = "\u001B[31mA\u001B[34mB";
        var sheet = new SpriteSheet(content, 1, 1, 2, 1);
        assertEquals(2, sheet.frameCount());
        // Build a Sprite from the sheet and render to verify SGR survives
        var sprite = sheet.toSprite();
        var size = new TerminalSize(10, 5);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        sprite.render(g, 0, 0);
        var cell0 = buffer.getCell(0, 0);
        assertEquals('A', cell0.character().charAt(0));
        assertEquals(AnsiColor.RED, cell0.fg());
    }

    @Test
    @DisplayName("frameCount is cols*rows")
    void frameCountIsColsTimesRows() {
        var sheet = new SpriteSheet("ABCDEF\nGHIJKL", 2, 2, 3, 2);
        assertEquals(6, sheet.frameCount());
    }

    @Test
    @DisplayName("rejects invalid dimensions")
    void rejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new SpriteSheet("ABC", 0, 2, 3, 1));
        assertThrows(IllegalArgumentException.class, () -> new SpriteSheet("ABC", 2, 0, 3, 1));
        assertThrows(IllegalArgumentException.class, () -> new SpriteSheet("ABC", 2, 2, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new SpriteSheet("ABC", 2, 2, 3, 0));
    }

    @Test
    @DisplayName("handles lines shorter than expected (pad with spaces)")
    void handlesShortLines() {
        // 2 cols × 2 rows of 2x1 cells. Content has 2 lines:
        //  "ABC" — only 3 chars wide but expected 4 (cols * cellWidth)
        //  "DEF" — only 3 chars wide
        // Frame 0 (col 0, row 0) = "AB"
        // Frame 1 (col 1, row 0) = "C " (padded to width 2)
        // Frame 2 (col 0, row 1) = "DE"
        // Frame 3 (col 1, row 1) = "F " (padded)
        var sheet = new SpriteSheet("ABC\nDEF", 2, 1, 2, 2);
        assertEquals(4, sheet.frameCount());
        assertEquals("AB", sheet.frameAt(0));
        assertEquals("C ", sheet.frameAt(1));
        assertEquals("DE", sheet.frameAt(2));
        assertEquals("F ", sheet.frameAt(3));
    }
}