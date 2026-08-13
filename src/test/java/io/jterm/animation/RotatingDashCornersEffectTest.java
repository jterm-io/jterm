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

class RotatingDashCornersEffectTest {

    private static final TerminalSize SIZE = new TerminalSize(10, 6);

    // The 8-step rotating dash cycle: ┌ ╱ ┐ ╲ ┘ ╱ └ ╲
    private static final char[] CYCLE = {
            '\u250C', '\u2571', '\u2510', '\u2572',
            '\u2518', '\u2571', '\u2514', '\u2572'
    };

    // Phase offsets: TL=0, TR=2, BR=4, BL=6 (90° apart in 8-step cycle)
    private static final int TL_PHASE = 0;
    private static final int TR_PHASE = 2;
    private static final int BR_PHASE = 4;
    private static final int BL_PHASE = 6;

    private BorderContext newContext() {
        return new BorderContext(SIZE, Border.BorderStyle.SINGLE_LINE);
    }

    @Test
    @DisplayName("name() returns rotating-dash-corners")
    void nameReturnsRotatingDashCorners() {
        var effect = new RotatingDashCornersEffect();
        assertEquals("rotating-dash-corners", effect.name());
    }

    @Test
    @DisplayName("update() sets all 4 corners")
    void updateSetsAllFourCorners() {
        var effect = new RotatingDashCornersEffect();
        var ctx = newContext();
        effect.update(0, ctx);

        assertNotNull(ctx.getCorner(BorderContext.Corner.TL));
        assertNotNull(ctx.getCorner(BorderContext.Corner.TR));
        assertNotNull(ctx.getCorner(BorderContext.Corner.BL));
        assertNotNull(ctx.getCorner(BorderContext.Corner.BR));
    }

    @Test
    @DisplayName("corners cycle through the char sequence")
    void cornersCycleThroughSequence() {
        var effect = new RotatingDashCornersEffect();

        // TL has phase 0, so at frame f it shows CYCLE[(0 + f) % 8]
        for (int f = 0; f < 8; f++) {
            var ctx = newContext();
            effect.update(f, ctx);
            assertEquals(String.valueOf(CYCLE[f % 8]),
                    ctx.getCorner(BorderContext.Corner.TL),
                    "TL at frame " + f);
        }
    }

    @Test
    @DisplayName("corners are 90 degrees out of phase")
    void cornersAre90DegreesOutOfPhase() {
        var effect = new RotatingDashCornersEffect();

        // At frame 0: TL=CYCLE[0], TR=CYCLE[2], BR=CYCLE[4], BL=CYCLE[6]
        var ctx = newContext();
        effect.update(0, ctx);

        assertEquals(String.valueOf(CYCLE[0]), ctx.getCorner(BorderContext.Corner.TL));
        assertEquals(String.valueOf(CYCLE[2]), ctx.getCorner(BorderContext.Corner.TR));
        assertEquals(String.valueOf(CYCLE[4]), ctx.getCorner(BorderContext.Corner.BR));
        assertEquals(String.valueOf(CYCLE[6]), ctx.getCorner(BorderContext.Corner.BL));
    }

    @Test
    @DisplayName("after 8 frames the pattern repeats")
    void patternRepeatsAfter8Frames() {
        var effect = new RotatingDashCornersEffect();

        for (int f = 0; f < 16; f++) {
            var ctx = newContext();
            effect.update(f, ctx);
            var ctx2 = newContext();
            effect.update(f + 8, ctx2);

            assertEquals(ctx2.getCorner(BorderContext.Corner.TL),
                    ctx.getCorner(BorderContext.Corner.TL),
                    "TL should repeat at frame " + f);
            assertEquals(ctx2.getCorner(BorderContext.Corner.TR),
                    ctx.getCorner(BorderContext.Corner.TR),
                    "TR should repeat at frame " + f);
            assertEquals(ctx2.getCorner(BorderContext.Corner.BR),
                    ctx.getCorner(BorderContext.Corner.BR),
                    "BR should repeat at frame " + f);
            assertEquals(ctx2.getCorner(BorderContext.Corner.BL),
                    ctx.getCorner(BorderContext.Corner.BL),
                    "BL should repeat at frame " + f);
        }
    }

    @Test
    @DisplayName("edges are NOT modified — only corners change")
    void edgesAreNotModified() {
        var effect = new RotatingDashCornersEffect();
        var ctx = newContext();
        effect.update(5, ctx);

        assertEquals(Border.BorderStyle.SINGLE_LINE.horizontal(),
                ctx.getEdge(BorderContext.Side.TOP, 0));
        assertEquals(Border.BorderStyle.SINGLE_LINE.horizontal(),
                ctx.getEdge(BorderContext.Side.BOTTOM, 0));
        assertEquals(Border.BorderStyle.SINGLE_LINE.vertical(),
                ctx.getEdge(BorderContext.Side.LEFT, 0));
        assertEquals(Border.BorderStyle.SINGLE_LINE.vertical(),
                ctx.getEdge(BorderContext.Side.RIGHT, 0));
    }

    @Test
    @DisplayName("TR corner has phase offset 2")
    void trCornerPhaseOffset() {
        var effect = new RotatingDashCornersEffect();
        for (int f = 0; f < 12; f++) {
            var ctx = newContext();
            effect.update(f, ctx);
            assertEquals(String.valueOf(CYCLE[(2 + f) % 8]),
                    ctx.getCorner(BorderContext.Corner.TR),
                    "TR at frame " + f);
        }
    }

    @Test
    @DisplayName("BR corner has phase offset 4")
    void brCornerPhaseOffset() {
        var effect = new RotatingDashCornersEffect();
        for (int f = 0; f < 12; f++) {
            var ctx = newContext();
            effect.update(f, ctx);
            assertEquals(String.valueOf(CYCLE[(4 + f) % 8]),
                    ctx.getCorner(BorderContext.Corner.BR),
                    "BR at frame " + f);
        }
    }

    @Test
    @DisplayName("BL corner has phase offset 6")
    void blCornerPhaseOffset() {
        var effect = new RotatingDashCornersEffect();
        for (int f = 0; f < 12; f++) {
            var ctx = newContext();
            effect.update(f, ctx);
            assertEquals(String.valueOf(CYCLE[(6 + f) % 8]),
                    ctx.getCorner(BorderContext.Corner.BL),
                    "BL at frame " + f);
        }
    }

    @Test
    @DisplayName("AnimatedBorder with rotating-dash effect renders corner cells")
    void rotatingDashEffectWithAnimatedBorder() {
        var effect = new RotatingDashCornersEffect();
        var border = new AnimatedBorder(
                new EmptySpace(new TerminalSize(8, 4)),
                effect
        );
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 6));

        // Simulate frame 0: TL=┌(CYCLE[0]), TR=┐(CYCLE[2]), BR=┘(CYCLE[4]), BL=└(CYCLE[6])
        var ctx = border.createContext();
        effect.update(0, ctx);
        var buffer = new ScreenBuffer(new TerminalSize(10, 6));
        border.drawWithBorderContext(new TextGraphics(buffer), ctx);

        assertEquals(CYCLE[0], buffer.getCell(0, 0).character().charAt(0), "TL corner");
        assertEquals(CYCLE[2], buffer.getCell(9, 0).character().charAt(0), "TR corner");
        assertEquals(CYCLE[4], buffer.getCell(9, 5).character().charAt(0), "BR corner");
        assertEquals(CYCLE[6], buffer.getCell(0, 5).character().charAt(0), "BL corner");
    }

    @Test
    @DisplayName("AnimatedBorder with rotating-dash effect leaves edge cells unchanged")
    void rotatingDashEffectLeavesEdgeCells() {
        var effect = new RotatingDashCornersEffect();
        var border = new AnimatedBorder(
                new EmptySpace(new TerminalSize(8, 4)),
                Border.BorderStyle.SINGLE_LINE,
                effect
        );
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 6));

        var ctx = border.createContext();
        effect.update(3, ctx);
        var buffer = new ScreenBuffer(new TerminalSize(10, 6));
        border.drawWithBorderContext(new TextGraphics(buffer), ctx);

        // Top edge (column 1) should be horizontal line, not a rotated char
        assertEquals('\u2500', buffer.getCell(1, 0).character().charAt(0), "top edge");
        // Left edge (row 1) should be vertical line
        assertEquals('\u2502', buffer.getCell(0, 1).character().charAt(0), "left edge");
    }
}