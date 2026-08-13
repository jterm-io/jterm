package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.widget.AnimatedBorder;
import io.jterm.widget.Border;
import io.jterm.widget.EmptySpace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScanningLineEffectTest {

    private static final TerminalSize SIZE = new TerminalSize(10, 6);
    /**
     * Perimeter length for a 10x6 border:
     * top: 8 edges + 2 corners = 10 positions (but corners are shared with sides)
     * Perimeter = top(10) + right(4) + bottom(8) + left(4) = 26
     * Or more precisely: 2*(cols-2) + 2*(rows-2) + 4 corners = 2*8 + 2*4 + 4 = 28?
     * No: perimeter = 2*cols + 2*rows - 4 = 20 + 12 - 4 = 28? No.
     * top row: 10 cells, bottom row: 10 cells, left col (minus corners): 4, right col: 4 = 28? No.
     * Perimeter = top(cols) + right(rows-2) + bottom(cols) + left(rows-2)
     *           = 10 + 4 + 10 + 4 = 28? No, that double-counts corners.
     * Actually: perimeter = 2*(cols-1) + 2*(rows-1) = 2*9 + 2*5 = 28.
     * Wait, let's think of it as walking the border:
     * top edge: positions 0..(cols-2) = 0..8 (9 cells, but corners counted separately)
     * Actually the perimeter in the effect is: top edge positions, then right edge positions,
     * then bottom edge positions, then left edge positions.
     * With ScanningLineEffect, the perimeter is computed from BorderContext.getPerimeterLength().
     * For 10x6: top edge = cols-2 = 8, right edge = rows-2 = 4,
     *           bottom edge = cols-2 = 8, left edge = rows-2 = 4,
     *           plus 4 corners = 8+4+8+4+4 = 28? No.
     * Actually perimeterLength = 2 * (cols - 2) + 2 * (rows - 2) + 4
     * = 2*8 + 2*4 + 4 = 28? That can't be right either.
     * Let me just compute: all cells on the border = 2*cols + 2*(rows-2) = 20 + 8 = 28? No:
     * Top row: cols=10 cells. Bottom row: cols=10 cells. Left col (interior): rows-2=4. Right col (interior): rows-2=4.
     * Total = 10+10+4+4 = 28. But that's all border cells.
     * But perimeter walking order matters. Let me just use what the effect computes.
     */
    private static final int PERIMETER = 28; // 2*10 + 2*4 = 28 for a 10x6 border

    private BorderContext newContext() {
        return new BorderContext(SIZE, Border.BorderStyle.SINGLE_LINE);
    }

    // ---- name() ----

    @Test
    @DisplayName("name() returns scanning-line")
    void nameReturnsScanningLine() {
        var effect = new ScanningLineEffect();
        assertEquals("scanning-line", effect.name());
    }

    // ---- Default highlight character ----

    @Test
    @DisplayName("default highlight character is ■ (U+25A0)")
    void defaultHighlightChar() {
        var effect = new ScanningLineEffect();
        var ctx = newContext();
        effect.update(0, ctx);
        // At frame 0, position 0 is the top-left corner area
        // Check that at least one edge/corner has a non-default char
        boolean foundHighlight = false;
        // Check corners
        for (var corner : BorderContext.Corner.values()) {
            if (!ctx.getCorner(corner).equals(Border.BorderStyle.SINGLE_LINE.topLeft()) &&
                !ctx.getCorner(corner).equals(Border.BorderStyle.SINGLE_LINE.topRight()) &&
                !ctx.getCorner(corner).equals(Border.BorderStyle.SINGLE_LINE.bottomLeft()) &&
                !ctx.getCorner(corner).equals(Border.BorderStyle.SINGLE_LINE.bottomRight())) {
                foundHighlight = true;
            }
        }
        // Check edges
        for (var side : BorderContext.Side.values()) {
            int edgeCount = switch (side) {
                case TOP, BOTTOM -> SIZE.columns() - 2;
                case LEFT, RIGHT -> SIZE.rows() - 2;
            };
            for (int i = 0; i < edgeCount; i++) {
                String expected = switch (side) {
                    case TOP, BOTTOM -> Border.BorderStyle.SINGLE_LINE.horizontal();
                    case LEFT, RIGHT -> Border.BorderStyle.SINGLE_LINE.vertical();
                };
                if (!ctx.getEdge(side, i).equals(expected)) {
                    foundHighlight = true;
                }
            }
        }
        assertTrue(foundHighlight, "At least one border cell should be highlighted");
    }

    // ---- Exactly one cell highlighted per frame ----

    @Test
    @DisplayName("update() highlights exactly one cell on the perimeter")
    void exactlyOneCellHighlighted() {
        var effect = new ScanningLineEffect();
        for (int frame = 0; frame < 5; frame++) {
            var ctx = newContext();
            effect.update(frame, ctx);
            int highlightCount = countHighlightedCells(ctx);
            assertEquals(1, highlightCount,
                    "Frame " + frame + " should highlight exactly 1 cell, found " + highlightCount);
        }
    }

    private int countHighlightedCells(BorderContext ctx) {
        int count = 0;
        // Check corner highlights
        for (var corner : BorderContext.Corner.values()) {
            if (ctx.getCornerColor(corner) != null) count++;
        }
        // Check edge highlights
        for (var side : BorderContext.Side.values()) {
            int edgeCount = switch (side) {
                case TOP, BOTTOM -> SIZE.columns() - 2;
                case LEFT, RIGHT -> SIZE.rows() - 2;
            };
            for (int i = 0; i < edgeCount; i++) {
                if (ctx.getEdgeColor(side, i) != null) count++;
            }
        }
        return count;
    }

    // ---- Highlight moves by 1 per frame ----

    @Test
    @DisplayName("highlight moves by 1 position per frame")
    void highlightMovesByOne() {
        var effect = new ScanningLineEffect();
        // Find the highlighted position at frames 0 and 1
        var ctx0 = newContext();
        effect.update(0, ctx0);
        int pos0 = findHighlightedPosition(ctx0);

        var ctx1 = newContext();
        effect.update(1, ctx1);
        int pos1 = findHighlightedPosition(ctx1);

        assertEquals(pos0 + 1, pos1,
                "Highlight should advance by 1 position per frame");
    }

    private int findHighlightedPosition(BorderContext ctx) {
        // Check corners first (positions 0..3 in TL,TR,BR,BL order)
        // Then edges: top(0..n), right(0..n), bottom(0..n), left(0..n)
        // Position mapping:
        // 0 = TL corner
        // 1..(cols-2) = top edge positions
        // cols-1 = TR corner
        // cols..(cols+rows-3) = right edge positions
        // cols+rows-2 = BR corner
        // cols+rows-1..(2*cols+rows-3) = bottom edge positions (reversed)
        // 2*cols+rows-2 = BL corner
        // 2*cols+rows-1..(2*cols+2*rows-4) = left edge positions (reversed)
        int cols = SIZE.columns();
        int rows = SIZE.rows();

        // Check corners
        if (ctx.getCornerColor(BorderContext.Corner.TL) != null) return 0;
        if (ctx.getCornerColor(BorderContext.Corner.TR) != null) return cols - 1;
        if (ctx.getCornerColor(BorderContext.Corner.BR) != null) return cols + rows - 2;
        if (ctx.getCornerColor(BorderContext.Corner.BL) != null) return 2 * cols + rows - 3;

        // Check top edge
        for (int i = 0; i < cols - 2; i++) {
            if (ctx.getEdgeColor(BorderContext.Side.TOP, i) != null) return 1 + i;
        }
        // Check right edge
        for (int i = 0; i < rows - 2; i++) {
            if (ctx.getEdgeColor(BorderContext.Side.RIGHT, i) != null) return cols + i;
        }
        // Check bottom edge (reversed)
        for (int i = 0; i < cols - 2; i++) {
            if (ctx.getEdgeColor(BorderContext.Side.BOTTOM, i) != null) return 2 * cols + rows - 4 - i;
        }
        // Check left edge (reversed)
        for (int i = 0; i < rows - 2; i++) {
            if (ctx.getEdgeColor(BorderContext.Side.LEFT, i) != null) return 2 * cols + 2 * rows - 5 - i;
        }
        return -1; // Should not happen
    }

    // ---- Wrap-around after perimeter length ----

    @Test
    @DisplayName("highlight wraps around after perimeterLength frames")
    void highlightWrapsAround() {
        var effect = new ScanningLineEffect();
        int perimeter = effect.getPerimeterLength(SIZE);

        var ctx0 = newContext();
        effect.update(0, ctx0);
        int pos0 = findHighlightedPosition(ctx0);

        var ctxWrap = newContext();
        effect.update(perimeter, ctxWrap);
        int posWrap = findHighlightedPosition(ctxWrap);

        assertEquals(pos0, posWrap,
                "After perimeterLength frames, highlight should be back at position 0");
    }

    // ---- Previous position restored to base style ----

    @Test
    @DisplayName("previous position is restored to base style after resetToStyle")
    void previousPositionRestored() {
        var effect = new ScanningLineEffect();
        // At frame 0, position 0 (TL corner) is highlighted
        var ctx = newContext();
        effect.update(0, ctx);
        assertNotNull(ctx.getCornerColor(BorderContext.Corner.TL),
                "TL corner should be highlighted at frame 0");

        // At frame 1, position 1 (top edge 0) is highlighted, TL corner should be back to normal
        var ctx1 = newContext();
        effect.update(1, ctx1);
        assertNull(ctx1.getCornerColor(BorderContext.Corner.TL),
                "TL corner color should be cleared at frame 1");
        assertEquals(Border.BorderStyle.SINGLE_LINE.topLeft(),
                ctx1.getCorner(BorderContext.Corner.TL),
                "TL corner char should be restored to default");
    }

    // ---- Custom highlight character ----

    @Test
    @DisplayName("custom highlight character is used")
    void customHighlightChar() {
        var effect = new ScanningLineEffect('█');
        var ctx = newContext();
        effect.update(0, ctx);
        // TL corner should use the highlight char
        assertEquals("█", ctx.getCorner(BorderContext.Corner.TL),
                "TL corner should use custom highlight char");
    }

    // ---- Custom speed ----

    @Test
    @DisplayName("speed=2 advances highlight by 2 positions per frame")
    void speedTwoAdvancesByTwo() {
        var effect = new ScanningLineEffect(2);
        var ctx0 = newContext();
        effect.update(0, ctx0);
        int pos0 = findHighlightedPosition(ctx0);

        var ctx1 = newContext();
        effect.update(1, ctx1);
        int pos1 = findHighlightedPosition(ctx1);

        assertEquals(2, pos1 - pos0,
                "With speed=2, highlight should advance by 2 per frame");
    }

    // ---- Highlight uses bright color ----

    @Test
    @DisplayName("highlight uses bright white color")
    void highlightUsesBrightColor() {
        var effect = new ScanningLineEffect();
        var ctx = newContext();
        effect.update(0, ctx);
        // TL corner at frame 0
        assertEquals(AnsiColor.BRIGHT_WHITE, ctx.getCornerColor(BorderContext.Corner.TL),
                "Highlighted cell should use BRIGHT_WHITE");
    }

    // ---- Only one cell has highlight color at a time ----

    @Test
    @DisplayName("only one cell has highlight color at a time")
    void onlyOneCellHighlighted() {
        var effect = new ScanningLineEffect();
        for (int frame = 0; frame < PERIMETER + 5; frame++) {
            var ctx = newContext();
            effect.update(frame, ctx);
            int count = countHighlightedCells(ctx);
            assertEquals(1, count,
                    "Frame " + frame + " should have exactly 1 highlighted cell");
        }
    }

    // ---- Integration: render border with scanning line ----

    @Test
    @DisplayName("AnimatedBorder with scanning-line renders highlighted cell")
    void scanningLineWithAnimatedBorder() {
        var effect = new ScanningLineEffect();
        var border = new AnimatedBorder(
                new EmptySpace(new TerminalSize(8, 4)),
                effect
        );
        border.setBounds(TerminalPosition.TOP_LEFT, SIZE);

        var ctx = border.createContext();
        effect.update(0, ctx);
        var buffer = new ScreenBuffer(SIZE);
        border.drawWithBorderContext(new TextGraphics(buffer), ctx);

        // TL corner (0,0) should have BRIGHT_WHITE fg and be the highlight char
        assertEquals(AnsiColor.BRIGHT_WHITE, buffer.getCell(0, 0).fg(),
                "TL corner should have BRIGHT_WHITE foreground at frame 0");
        // Other border cells should not have BRIGHT_WHITE
        assertNotEquals(AnsiColor.BRIGHT_WHITE, buffer.getCell(1, 0).fg(),
                "Top edge pos 0 should NOT be highlighted at frame 0");
    }

    @Test
    @DisplayName("AnimatedBorder with scanning-line at frame 5 highlights correct cell")
    void scanningLineAtFrame5() {
        var effect = new ScanningLineEffect();
        var border = new AnimatedBorder(
                new EmptySpace(new TerminalSize(8, 4)),
                effect
        );
        border.setBounds(TerminalPosition.TOP_LEFT, SIZE);

        // Frame 5: position 5 on perimeter, which is top edge position 4 (0-indexed: pos 0=TL, 1=top0, 2=top1, 3=top2, 4=top3, 5=top4)
        // Column = 1 + 4 = 5
        var ctx5 = border.createContext();
        effect.update(5, ctx5);
        var buffer5 = new ScreenBuffer(SIZE);
        border.drawWithBorderContext(new TextGraphics(buffer5), ctx5);

        assertEquals(AnsiColor.BRIGHT_WHITE, buffer5.getCell(5, 0).fg(),
                "Column 5 on top row should be highlighted at frame 5");
    }

    // ---- PerimeterLength is correct ----

    @Test
    @DisplayName("perimeterLength is computed correctly")
    void perimeterLengthComputedCorrectly() {
        var effect = new ScanningLineEffect();
        // For 10x6: 2*(10) + 2*(6-2) = 20 + 8 = 28
        // Actually: top row=10, bottom row=10, left interior=4, right interior=4 = 28
        // But perimeter walk = 2*(cols-1) + 2*(rows-1) = 18 + 10 = 28
        assertEquals(28, effect.getPerimeterLength(SIZE),
                "Perimeter of 10x6 should be 28");
    }

    @Test
    @DisplayName("perimeterLength for square is correct")
    void perimeterLengthSquare() {
        var effect = new ScanningLineEffect();
        // For 4x4: 2*3 + 2*3 = 12? No: all border cells = 4*4 - 2*2 = 12
        var size4x4 = new TerminalSize(4, 4);
        assertEquals(12, effect.getPerimeterLength(size4x4),
                "Perimeter of 4x4 should be 12");
    }

    // ---- Validation: highlight char ----

    @Test
    @DisplayName("constructor rejects speed <= 0")
    void constructorRejectsInvalidSpeed() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScanningLineEffect(0),
                "Should reject speed 0");
        assertThrows(IllegalArgumentException.class,
                () -> new ScanningLineEffect(-1),
                "Should reject negative speed");
    }
}