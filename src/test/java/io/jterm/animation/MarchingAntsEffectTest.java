package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.widget.AnimatedBorder;
import io.jterm.widget.Border;
import io.jterm.widget.EmptySpace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarchingAntsEffectTest {

    private static final TerminalSize SIZE = new TerminalSize(10, 6);

    private BorderContext newContext() {
        return new BorderContext(SIZE, Border.BorderStyle.SINGLE_LINE);
    }

    // ---- name() ----

    @Test
    @DisplayName("name() returns marching-ants")
    void nameReturnsMarchingAnts() {
        var effect = new MarchingAntsEffect();
        assertEquals("marching-ants", effect.name());
    }

    // ---- Default constructor (dash=3, gap=2) ----

    @Test
    @DisplayName("default constructor has dash length 3 and gap length 2")
    void defaultConstructorDashAndGap() {
        var effect = new MarchingAntsEffect();
        // Cycle length = dash + gap = 3 + 2 = 5
        // Verify by checking that frame 0 and frame 5 produce same output
        var ctx0 = newContext();
        effect.update(0, ctx0);
        var ctx5 = newContext();
        effect.update(5, ctx5);

        assertEquals(ctx0.getEdge(BorderContext.Side.TOP, 0),
                ctx5.getEdge(BorderContext.Side.TOP, 0),
                "cycle should repeat after dash+gap=5 frames");
    }

    // ---- update() modifies edges but not corners ----

    @Test
    @DisplayName("update() does not modify corners")
    void updateDoesNotModifyCorners() {
        var effect = new MarchingAntsEffect();
        var ctx = newContext();
        effect.update(0, ctx);

        // Corners should remain as base style corners
        assertEquals(Border.BorderStyle.SINGLE_LINE.topLeft(), ctx.getCorner(BorderContext.Corner.TL));
        assertEquals(Border.BorderStyle.SINGLE_LINE.topRight(), ctx.getCorner(BorderContext.Corner.TR));
        assertEquals(Border.BorderStyle.SINGLE_LINE.bottomLeft(), ctx.getCorner(BorderContext.Corner.BL));
        assertEquals(Border.BorderStyle.SINGLE_LINE.bottomRight(), ctx.getCorner(BorderContext.Corner.BR));
    }

    @Test
    @DisplayName("update() modifies top and bottom edge cells")
    void updateModifiesTopAndBottomEdges() {
        var effect = new MarchingAntsEffect();
        var ctx = newContext();
        effect.update(0, ctx);

        // Top/bottom edges should have dash/gap pattern (not all default horizontal)
        // For dash=3, gap=2, position 0 should be '-', position 3 should be '·'
        assertEquals("-", ctx.getEdge(BorderContext.Side.TOP, 0));
        assertEquals("·", ctx.getEdge(BorderContext.Side.TOP, 3));
        assertEquals("-", ctx.getEdge(BorderContext.Side.BOTTOM, 0));
        assertEquals("·", ctx.getEdge(BorderContext.Side.BOTTOM, 3));
    }

    @Test
    @DisplayName("update() modifies left and right edge cells")
    void updateModifiesLeftAndRightEdges() {
        var effect = new MarchingAntsEffect();
        var ctx = newContext();
        effect.update(0, ctx);

        // Left/right edges use '|' for dash and '·' for gap
        assertEquals("|", ctx.getEdge(BorderContext.Side.LEFT, 0));
        assertEquals("·", ctx.getEdge(BorderContext.Side.LEFT, 3));
        assertEquals("|", ctx.getEdge(BorderContext.Side.RIGHT, 0));
        assertEquals("·", ctx.getEdge(BorderContext.Side.RIGHT, 3));
    }

    // ---- Pattern shifts by 1 per frame ----

    @Test
    @DisplayName("top edge pattern shifts right by 1 cell per frame")
    void topEdgeShiftsRightPerFrame() {
        var effect = new MarchingAntsEffect();

        // At frame 0: position 0 = dash, position 3 = gap
        // At frame 1: position 0 = gap (was the char at position -1, shifted right means pattern starts 1 later)
        // Actually: offset = frame % cycleLength. At frame f, position p is dash if
        // (p - offset) % cycleLength < dashLength

        // Frame 0: pos 0 is dash (since (0 - 0) % 5 = 0 < 3)
        // Frame 1: pos 0 is gap  (since (0 - 1) % 5 = 4 ≥ 3)
        var ctx0 = newContext();
        effect.update(0, ctx0);
        assertEquals("-", ctx0.getEdge(BorderContext.Side.TOP, 0));

        var ctx1 = newContext();
        effect.update(1, ctx1);
        assertEquals("·", ctx1.getEdge(BorderContext.Side.TOP, 0),
                "at frame 1, position 0 should be gap (shifted right)");

        // At frame 1, position 1 should now be dash (shifted from position 0)
        assertEquals("-", ctx1.getEdge(BorderContext.Side.TOP, 1));
    }

    @Test
    @DisplayName("left edge pattern shifts down by 1 cell per frame")
    void leftEdgeShiftsDownPerFrame() {
        var effect = new MarchingAntsEffect();

        var ctx0 = newContext();
        effect.update(0, ctx0);
        assertEquals("|", ctx0.getEdge(BorderContext.Side.LEFT, 0));

        var ctx1 = newContext();
        effect.update(1, ctx1);
        assertEquals("·", ctx1.getEdge(BorderContext.Side.LEFT, 0),
                "at frame 1, position 0 should be gap (shifted down)");
        assertEquals("|", ctx1.getEdge(BorderContext.Side.LEFT, 1));
    }

    // ---- Cycle repeats ----

    @Test
    @DisplayName("pattern repeats after cycleLength frames (dash+gap=5)")
    void patternRepeatsAfterCycleLength() {
        var effect = new MarchingAntsEffect(); // default: dash=3, gap=2, cycle=5

        for (int f = 0; f < 10; f++) {
            var ctx = newContext();
            effect.update(f, ctx);
            var ctx2 = newContext();
            effect.update(f + 5, ctx2);

            // Compare all edge positions
            for (int p = 0; p < 8; p++) {
                assertEquals(ctx.getEdge(BorderContext.Side.TOP, p),
                        ctx2.getEdge(BorderContext.Side.TOP, p),
                        "TOP position " + p + " at frame " + f + " should repeat after 5");
                assertEquals(ctx.getEdge(BorderContext.Side.BOTTOM, p),
                        ctx2.getEdge(BorderContext.Side.BOTTOM, p),
                        "BOTTOM position " + p + " at frame " + f + " should repeat after 5");
            }
            for (int p = 0; p < 4; p++) {
                assertEquals(ctx.getEdge(BorderContext.Side.LEFT, p),
                        ctx2.getEdge(BorderContext.Side.LEFT, p),
                        "LEFT position " + p + " at frame " + f + " should repeat after 5");
                assertEquals(ctx.getEdge(BorderContext.Side.RIGHT, p),
                        ctx2.getEdge(BorderContext.Side.RIGHT, p),
                        "RIGHT position " + p + " at frame " + f + " should repeat after 5");
            }
        }
    }

    // ---- Custom dash/gap lengths ----

    @Test
    @DisplayName("custom dash and gap lengths work correctly")
    void customDashAndGapLengths() {
        var effect = new MarchingAntsEffect(2, 1); // dash=2, gap=1, cycle=3

        var ctx = newContext();
        effect.update(0, ctx);

        // Cycle of 3: positions 0,1 = dash, position 2 = gap
        assertEquals("-", ctx.getEdge(BorderContext.Side.TOP, 0));
        assertEquals("-", ctx.getEdge(BorderContext.Side.TOP, 1));
        assertEquals("·", ctx.getEdge(BorderContext.Side.TOP, 2));

        // Repeat after 3 frames
        var ctx3 = newContext();
        effect.update(3, ctx3);
        assertEquals(ctx.getEdge(BorderContext.Side.TOP, 0), ctx3.getEdge(BorderContext.Side.TOP, 0));
        assertEquals(ctx.getEdge(BorderContext.Side.TOP, 1), ctx3.getEdge(BorderContext.Side.TOP, 1));
        assertEquals(ctx.getEdge(BorderContext.Side.TOP, 2), ctx3.getEdge(BorderContext.Side.TOP, 2));
    }

    // ---- AnimatedBorder integration ----

    @Test
    @DisplayName("AnimatedBorder with marching ants renders edge cells")
    void marchingAntsWithAnimatedBorder() {
        var effect = new MarchingAntsEffect();
        var border = new AnimatedBorder(
                new EmptySpace(new TerminalSize(8, 4)),
                effect
        );
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 6));

        var ctx = border.createContext();
        effect.update(0, ctx);
        var buffer = new ScreenBuffer(new TerminalSize(10, 6));
        border.drawWithBorderContext(new TextGraphics(buffer), ctx);

        // Top edge position 1 should be dash '-'
        assertEquals('-', buffer.getCell(1, 0).character().charAt(0), "top edge col 1");
        // Top edge position 4 should be gap '·'
        assertEquals('·', buffer.getCell(4, 0).character().charAt(0), "top edge col 4");

        // Left edge row 1 should be dash '|'
        assertEquals('|', buffer.getCell(0, 1).character().charAt(0), "left edge row 1");
        // Left edge row 4 should be gap '·'
        assertEquals('·', buffer.getCell(0, 4).character().charAt(0), "left edge row 4");
    }

    @Test
    @DisplayName("AnimatedBorder with marching ants preserves corner cells")
    void marchingAntsPreservesCornerCells() {
        var effect = new MarchingAntsEffect();
        var border = new AnimatedBorder(
                new EmptySpace(new TerminalSize(8, 4)),
                Border.BorderStyle.SINGLE_LINE,
                effect
        );
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 6));

        var ctx = border.createContext();
        effect.update(0, ctx);
        var buffer = new ScreenBuffer(new TerminalSize(10, 6));
        border.drawWithBorderContext(new TextGraphics(buffer), ctx);

        // Corners should be the border style corners, not dash/gap chars
        assertEquals('┌', buffer.getCell(0, 0).character().charAt(0), "TL corner");
        assertEquals('┐', buffer.getCell(9, 0).character().charAt(0), "TR corner");
        assertEquals('└', buffer.getCell(0, 5).character().charAt(0), "BL corner");
        assertEquals('┘', buffer.getCell(9, 5).character().charAt(0), "BR corner");
    }

    // ---- Bottom edge same pattern as top, shifted by frame ----

    @Test
    @DisplayName("bottom edge follows same pattern as top, shifted by frame")
    void bottomEdgeSamePatternAsTop() {
        var effect = new MarchingAntsEffect();
        var ctx = newContext();
        effect.update(0, ctx);

        // At frame 0, bottom and top should have the same pattern
        for (int p = 0; p < 8; p++) {
            assertEquals(ctx.getEdge(BorderContext.Side.TOP, p),
                    ctx.getEdge(BorderContext.Side.BOTTOM, p),
                    "bottom should match top at position " + p + " for frame 0");
        }
    }

    // ---- Right edge same pattern as left, shifted by frame ----

    @Test
    @DisplayName("right edge follows same pattern as left, shifted by frame")
    void rightEdgeSamePatternAsLeft() {
        var effect = new MarchingAntsEffect();
        var ctx = newContext();
        effect.update(0, ctx);

        for (int p = 0; p < 4; p++) {
            assertEquals(ctx.getEdge(BorderContext.Side.LEFT, p),
                    ctx.getEdge(BorderContext.Side.RIGHT, p),
                    "right should match left at position " + p + " for frame 0");
        }
    }

    // ---- Horizontal uses '-', vertical uses '|' ----

    @Test
    @DisplayName("horizontal edges use dash character '-' for dash positions")
    void horizontalEdgesUseDash() {
        var effect = new MarchingAntsEffect();
        var ctx = newContext();
        effect.update(0, ctx);

        // Position 0 is in dash zone for default (3,2) at frame 0
        assertEquals("-", ctx.getEdge(BorderContext.Side.TOP, 0));
        assertEquals("-", ctx.getEdge(BorderContext.Side.BOTTOM, 0));
    }

    @Test
    @DisplayName("vertical edges use pipe character '|' for dash positions")
    void verticalEdgesUsePipe() {
        var effect = new MarchingAntsEffect();
        var ctx = newContext();
        effect.update(0, ctx);

        assertEquals("|", ctx.getEdge(BorderContext.Side.LEFT, 0));
        assertEquals("|", ctx.getEdge(BorderContext.Side.RIGHT, 0));
    }

    @Test
    @DisplayName("gap positions use middle dot '·'")
    void gapPositionsUseMiddleDot() {
        var effect = new MarchingAntsEffect(); // dash=3, gap=2
        var ctx = newContext();
        effect.update(0, ctx);

        // Position 3 is in gap zone (0,1,2=dash, 3,4=gap)
        assertEquals("·", ctx.getEdge(BorderContext.Side.TOP, 3));
        assertEquals("·", ctx.getEdge(BorderContext.Side.LEFT, 3));
    }

    // ---- Constructor validation ----

    @Test
    @DisplayName("constructor rejects dashLength < 1")
    void constructorRejectsInvalidDashLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new MarchingAntsEffect(0, 2),
                "Should reject dashLength 0");
        assertThrows(IllegalArgumentException.class,
                () -> new MarchingAntsEffect(-1, 2),
                "Should reject negative dashLength");
    }

    @Test
    @DisplayName("constructor rejects gapLength < 1")
    void constructorRejectsInvalidGapLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new MarchingAntsEffect(3, 0),
                "Should reject gapLength 0");
        assertThrows(IllegalArgumentException.class,
                () -> new MarchingAntsEffect(3, -1),
                "Should reject negative gapLength");
    }

    @Test
    @DisplayName("dash length 1 produces alternating dash/gap pattern")
    void dashLengthOne() {
        var effect = new MarchingAntsEffect(1, 1); // dash=1, gap=1, cycle=2
        var ctx = newContext();
        effect.update(0, ctx);

        // Position 0 = dash, position 1 = gap
        assertEquals("-", ctx.getEdge(BorderContext.Side.TOP, 0));
        assertEquals("·", ctx.getEdge(BorderContext.Side.TOP, 1));
    }
}