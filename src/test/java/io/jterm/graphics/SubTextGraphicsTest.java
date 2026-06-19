package io.jterm.graphics;

import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class SubTextGraphicsTest {

    // ===== Sub-graphics creation from parent TextGraphics =====

    @Test
    @DisplayName("SubTextGraphics is created from parent with offset and size")
    void subGraphicsCreationFromParent() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 5, 3, 8, 4);
        assertEquals(new TerminalSize(8, 4), sub.getSize());
    }

    @Test
    @DisplayName("getGraphics creates nested SubTextGraphics with accumulated offset")
    void getGraphicsCreatesNestedSub() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 2, 2, 10, 8);
        var nested = sub.getGraphics(1, 1, 3, 3);
        nested.setCell(0, 0, new TextCell('A'));
        // parent offset = (2+1, 2+1) = (3, 3)
        assertEquals('A', parent.getCell(3, 3).character().charAt(0));
    }

    @Test
    @DisplayName("null parent throws IllegalArgumentException")
    void nullParentThrows() {
        assertThrows(IllegalArgumentException.class, () -> new SubTextGraphics(null, 0, 0, 1, 1));
    }

    // ===== draw() within bounds / setCell / getCell =====

    @Test
    @DisplayName("setCell translates sub-coords to parent-coords")
    void setCellTranslatesToParent() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 2, 1, 4, 3);
        sub.setCell(0, 0, new TextCell('A'));
        assertEquals('A', parent.getCell(2, 1).character().charAt(0));
    }

    @Test
    @DisplayName("setCell at corner of sub-area maps to correct parent cell")
    void setCellAtCornerTranslatesCorrectly() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 5, 3, 4, 4);
        sub.setCell(3, 3, new TextCell('Z')); // bottom-right of sub
        assertEquals('Z', parent.getCell(8, 6).character().charAt(0));
    }

    @Test
    @DisplayName("setCell ignores cells outside sub-area (x too large)")
    void setCellIgnoresOutsideSubAreaX() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 0, 0, 2, 2);
        sub.setCell(2, 0, new TextCell('A'));
        assertEquals(' ', parent.getCell(2, 0).character().charAt(0));
    }

    @Test
    @DisplayName("setCell ignores cells outside sub-area (y too large)")
    void setCellIgnoresOutsideSubAreaY() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 0, 0, 2, 2);
        sub.setCell(0, 2, new TextCell('A'));
        assertEquals(' ', parent.getCell(0, 2).character().charAt(0));
    }

    @Test
    @DisplayName("setCell ignores negative coordinates")
    void setCellIgnoresNegativeCoords() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 1, 1, 4, 4);
        sub.setCell(-1, 0, new TextCell('A'));
        sub.setCell(0, -1, new TextCell('A'));
        // Parent cells around the offset should be unchanged
        assertEquals(' ', parent.getCell(0, 1).character().charAt(0));
        assertEquals(' ', parent.getCell(1, 0).character().charAt(0));
    }

    @Test
    @DisplayName("getCell reads from parent with offset translation")
    void getCellReadsFromParent() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        parent.setCell(3, 2, new TextCell('A'));
        var sub = new SubTextGraphics(parent, 1, 1, 5, 3);
        assertEquals('A', sub.getCell(2, 1).character().charAt(0));
    }

    @Test
    @DisplayName("getCell returns EMPTY outside sub-area bounds")
    void getCellReturnsEmptyOutsideBounds() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 0, 0, 2, 2);
        assertEquals(TextCell.EMPTY, sub.getCell(3, 3));
    }

    @Test
    @DisplayName("getCell returns EMPTY for negative coordinates")
    void getCellReturnsEmptyForNegativeCoords() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 1, 1, 4, 4);
        assertEquals(TextCell.EMPTY, sub.getCell(-1, 0));
        assertEquals(TextCell.EMPTY, sub.getCell(0, -1));
    }

    @Test
    @DisplayName("putCell delegates to setCell")
    void putCellDelegatesToSetCell() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 2, 1, 4, 3);
        sub.putCell(1, 1, new TextCell('P'));
        assertEquals('P', parent.getCell(3, 2).character().charAt(0));
    }

    @Test
    @DisplayName("putCell outside bounds is clipped (no-op)")
    void putCellOutsideBoundsClipped() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 0, 0, 3, 3);
        sub.putCell(5, 5, new TextCell('X'));
        assertEquals(' ', parent.getCell(5, 5).character().charAt(0));
    }

    @Test
    @DisplayName("getSize returns sub-area size, not parent size")
    void getSizeReturnsSubSize() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 5, 5, 4, 2);
        assertEquals(new TerminalSize(4, 2), sub.getSize());
    }

    // ===== Coordinate translation tests =====

    @Test
    @DisplayName("Coordinate translation: sub (0,0) → parent offset")
    void coordinateTranslationOrigin() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 7, 4, 5, 3);
        sub.setCell(0, 0, new TextCell('O'));
        assertEquals('O', parent.getCell(7, 4).character().charAt(0));
    }

    @Test
    @DisplayName("Coordinate translation: sub (w-1,h-1) → parent offset + size - 1")
    void coordinateTranslationFarCorner() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 7, 4, 5, 3);
        sub.setCell(4, 2, new TextCell('F')); // (w-1, h-1)
        assertEquals('F', parent.getCell(11, 6).character().charAt(0));
    }

    @Test
    @DisplayName("Multiple setCell calls translate correctly")
    void multipleSetCellTranslations() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 3, 2, 5, 4);
        sub.setCell(0, 0, new TextCell('A'));
        sub.setCell(1, 0, new TextCell('B'));
        sub.setCell(0, 1, new TextCell('C'));
        sub.setCell(4, 3, new TextCell('D'));
        assertEquals('A', parent.getCell(3, 2).character().charAt(0));
        assertEquals('B', parent.getCell(4, 2).character().charAt(0));
        assertEquals('C', parent.getCell(3, 3).character().charAt(0));
        assertEquals('D', parent.getCell(7, 5).character().charAt(0));
    }

    // ===== Out-of-bounds drawing (clipping) =====

    @Test
    @DisplayName("fillRectangle clips to sub-area when exceeding bounds")
    void fillRectangleClipsToSubArea() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 1, 1, 3, 3);
        sub.fillRectangle(0, 0, 10, 10, new TextCell('X'));
        assertEquals('X', parent.getCell(1, 1).character().charAt(0));
        assertEquals('X', parent.getCell(3, 3).character().charAt(0));
        assertEquals(' ', parent.getCell(4, 4).character().charAt(0));
    }

    @Test
    @DisplayName("fillRectangle with negative offset clips correctly")
    void fillRectangleWithNegativeOffsetClips() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 2, 2, 5, 3);
        // Fill starting at negative sub-coords — should clip to sub-area start
        sub.fillRectangle(-2, -2, 10, 10, new TextCell('X'));
        assertEquals('X', parent.getCell(2, 2).character().charAt(0));
        assertEquals('X', parent.getCell(6, 4).character().charAt(0));
    }

    @Test
    @DisplayName("fillRectangle partially outside clips to visible portion")
    void fillRectanglePartialOutsideClips() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 0, 0, 4, 4);
        sub.fillRectangle(2, 2, 10, 10, new TextCell('X'));
        // Only cells (2,2) to (3,3) should be filled
        assertEquals('X', parent.getCell(2, 2).character().charAt(0));
        assertEquals('X', parent.getCell(3, 3).character().charAt(0));
        assertEquals(' ', parent.getCell(4, 4).character().charAt(0));
    }

    // ===== drawText / drawString =====

    @Test
    @DisplayName("drawString(template) passes to parent with offset")
    void drawStringPassesToParentWithOffset() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 1, 1, 5, 3);
        sub.drawString(0, 0, "Hi", new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT));
        assertEquals('H', parent.getCell(1, 1).character().charAt(0));
        assertEquals('i', parent.getCell(2, 1).character().charAt(0));
    }

    @Test
    @DisplayName("drawString(fg,bg,SGR...) passes to parent with offset")
    void drawStringWithColorsPassesToParentWithOffset() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 2, 1, 5, 3);
        sub.drawString(0, 0, "AB", AnsiColor.RED, AnsiColor.BLUE, SGR.BOLD);
        var cellH = parent.getCell(2, 1);
        var cellB = parent.getCell(3, 1);
        assertEquals('A', cellH.character().charAt(0));
        assertEquals('B', cellB.character().charAt(0));
        assertEquals(AnsiColor.RED, cellH.fg());
        assertEquals(AnsiColor.BLUE, cellH.bg());
        assertTrue(cellH.modifiers().contains(SGR.BOLD));
    }

    @Test
    @DisplayName("drawString with no SGR mods creates cell with empty modifiers")
    void drawStringWithNoSgrMods() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 0, 0, 5, 3);
        sub.drawString(0, 0, "X", AnsiColor.GREEN, AnsiColor.DEFAULT);
        var cell = parent.getCell(0, 0);
        assertEquals('X', cell.character().charAt(0));
        assertEquals(AnsiColor.GREEN, cell.fg());
        assertTrue(cell.modifiers().isEmpty());
    }

    @Test
    @DisplayName("drawString at sub offset translates to parent correctly")
    void drawStringTranslatesOffset() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 5, 3, 10, 5);
        sub.drawString(2, 1, "Hi", new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT));
        assertEquals('H', parent.getCell(7, 4).character().charAt(0));
        assertEquals('i', parent.getCell(8, 4).character().charAt(0));
    }

    // ===== drawLine =====

    @Test
    @DisplayName("drawLine translates to parent coordinates")
    void drawLineTranslatesToParent() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 5, 3, 10, 5);
        sub.drawLine(0, 0, 2, 0, new TextCell('-'));
        assertEquals('-', parent.getCell(5, 3).character().charAt(0));
        assertEquals('-', parent.getCell(6, 3).character().charAt(0));
        assertEquals('-', parent.getCell(7, 3).character().charAt(0));
    }

    @Test
    @DisplayName("drawLine vertical translates to parent coordinates")
    void drawLineVerticalTranslates() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 5, 3, 10, 5);
        sub.drawLine(0, 0, 0, 2, new TextCell('|'));
        assertEquals('|', parent.getCell(5, 3).character().charAt(0));
        assertEquals('|', parent.getCell(5, 4).character().charAt(0));
        assertEquals('|', parent.getCell(5, 5).character().charAt(0));
    }

    @Test
    @DisplayName("drawLine diagonal translates to parent coordinates")
    void drawLineDiagonalTranslates() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 2, 2, 10, 5);
        sub.drawLine(0, 0, 2, 2, new TextCell('*'));
        assertEquals('*', parent.getCell(2, 2).character().charAt(0));
        assertEquals('*', parent.getCell(3, 3).character().charAt(0));
        assertEquals('*', parent.getCell(4, 4).character().charAt(0));
    }

    // ===== drawRectangle =====

    @Test
    @DisplayName("drawRectangle translates border to parent coordinates")
    void drawRectangleTranslatesToParent() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 5, 3, 6, 4);
        sub.drawRectangle(0, 0, 3, 3, new TextCell('#'));
        // Top edge
        assertEquals('#', parent.getCell(5, 3).character().charAt(0));
        assertEquals('#', parent.getCell(6, 3).character().charAt(0));
        assertEquals('#', parent.getCell(7, 3).character().charAt(0));
        // Bottom edge
        assertEquals('#', parent.getCell(5, 5).character().charAt(0));
        assertEquals('#', parent.getCell(7, 5).character().charAt(0));
        // Interior should be empty
        assertEquals(' ', parent.getCell(6, 4).character().charAt(0));
    }

    @Test
    @DisplayName("drawRectangle clips when partially outside sub-area")
    void drawRectangleClipsWhenPartiallyOutside() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 0, 0, 4, 4);
        // Draw rect that extends beyond sub bounds — should clip
        sub.drawRectangle(0, 0, 10, 10, new TextCell('#'));
        // Corners of the clipped rect (0,0) to (3,3)
        assertEquals('#', parent.getCell(0, 0).character().charAt(0));
        assertEquals('#', parent.getCell(3, 0).character().charAt(0));
        assertEquals('#', parent.getCell(0, 3).character().charAt(0));
        assertEquals('#', parent.getCell(3, 3).character().charAt(0));
        // Outside sub area should be unchanged
        assertEquals(' ', parent.getCell(4, 4).character().charAt(0));
    }

    @Test
    @DisplayName("drawRectangle with negative sub-origin clips")
    void drawRectangleWithNegativeOriginClips() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 3, 3, 5, 5);
        sub.drawRectangle(-2, -2, 10, 10, new TextCell('B'));
        // Should clip to sub area: parent (3,3) to (7,7)
        assertEquals('B', parent.getCell(3, 3).character().charAt(0));
        assertEquals('B', parent.getCell(7, 7).character().charAt(0));
    }

    // ===== fillRectangle =====

    @Test
    @DisplayName("fillRectangle fills entire sub-area")
    void fillRectangleFillsEntireSubArea() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 1, 1, 3, 3);
        sub.fillRectangle(0, 0, 3, 3, new TextCell('*'));
        for (int r = 1; r <= 3; r++) {
            for (int c = 1; c <= 3; c++) {
                assertEquals('*', parent.getCell(c, r).character().charAt(0));
            }
        }
    }

    @Test
    @DisplayName("fillRectangle with single cell")
    void fillRectangleSingleCell() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 2, 2, 4, 3);
        sub.fillRectangle(1, 1, 1, 1, new TextCell('S'));
        assertEquals('S', parent.getCell(3, 3).character().charAt(0));
    }

    // ===== Nested sub-graphics =====

    @Test
    @DisplayName("Nested SubTextGraphics accumulates offset correctly")
    void nestedSubGraphicsAccumulatesOffset() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = parent.getGraphics(1, 1, 5, 3);
        var nested = sub.getGraphics(1, 1, 2, 2);
        nested.setCell(0, 0, new TextCell('A'));
        assertEquals('A', parent.getCell(2, 2).character().charAt(0));
    }

    @Test
    @DisplayName("Nested sub-graphics getCell reads through accumulated offset")
    void nestedSubGraphicsGetCellReadsThroughOffset() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        parent.setCell(5, 5, new TextCell('Z'));
        var sub = new SubTextGraphics(parent, 2, 2, 10, 8);
        var nested = sub.getGraphics(2, 2, 5, 5);
        // nested (1,1) → sub (3,3) → parent (5,5)
        assertEquals('Z', nested.getCell(1, 1).character().charAt(0));
    }
}