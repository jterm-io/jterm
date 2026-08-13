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

class SparkleCornersEffectTest {

    private static final TerminalSize SIZE = new TerminalSize(10, 6);

    // The 16-step sparkle cycle
    private static final char[] CYCLE = {
            '\u2598', '\u259D', '\u2596', '\u2597',
            '\u258C', '\u2590', '\u2580', '\u2584',
            '\u2588', '\u2584', '\u2580', '\u2590',
            '\u258C', '\u2597', '\u2596', '\u259D'
    };

    private BorderContext newContext() {
        return new BorderContext(SIZE, Border.BorderStyle.SINGLE_LINE);
    }

    @Test
    @DisplayName("name() returns sparkle-corners")
    void nameReturnsSparkleCorners() {
        var effect = new SparkleCornersEffect();
        assertEquals("sparkle-corners", effect.name());
    }

    @Test
    @DisplayName("update() sets all 4 corners")
    void updateSetsAllFourCorners() {
        var effect = new SparkleCornersEffect();
        var ctx = newContext();
        effect.update(0, ctx);

        // All 4 corners should be non-null and from the cycle
        assertNotNull(ctx.getCorner(BorderContext.Corner.TL));
        assertNotNull(ctx.getCorner(BorderContext.Corner.TR));
        assertNotNull(ctx.getCorner(BorderContext.Corner.BL));
        assertNotNull(ctx.getCorner(BorderContext.Corner.BR));
    }

    @Test
    @DisplayName("corners are 90 degrees out of phase at frame 0")
    void cornersAre90DegreesOutOfPhaseAtFrame0() {
        var effect = new SparkleCornersEffect();
        var ctx = newContext();
        effect.update(0, ctx);

        // TL=phase 0='\u2598', TR=phase 4='\u258C', BR=phase 8='\u2588', BL=phase 12='\u258C'
        assertEquals(String.valueOf(CYCLE[0]),  ctx.getCorner(BorderContext.Corner.TL));
        assertEquals(String.valueOf(CYCLE[4]),  ctx.getCorner(BorderContext.Corner.TR));
        assertEquals(String.valueOf(CYCLE[8]),  ctx.getCorner(BorderContext.Corner.BR));
        assertEquals(String.valueOf(CYCLE[12]), ctx.getCorner(BorderContext.Corner.BL));
    }

    @Test
    @DisplayName("corners cycle through the char sequence")
    void cornersCycleThroughSequence() {
        var effect = new SparkleCornersEffect();

        // TL has phase 0, so at frame f it shows CYCLE[(0 + f) % 16]
        for (int f = 0; f < 16; f++) {
            var ctx = newContext();
            effect.update(f, ctx);
            assertEquals(String.valueOf(CYCLE[f % 16]),
                    ctx.getCorner(BorderContext.Corner.TL),
                    "TL at frame " + f);
        }
    }

    @Test
    @DisplayName("pattern repeats after 16 frames")
    void patternRepeatsAfter16Frames() {
        var effect = new SparkleCornersEffect();

        for (int f = 0; f < 32; f++) {
            var ctx = newContext();
            effect.update(f, ctx);
            // Compare frame f with frame f+16
            var ctx2 = newContext();
            effect.update(f + 16, ctx2);

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
        var effect = new SparkleCornersEffect();
        var ctx = newContext();
        effect.update(5, ctx);

        // All edges should still be default style characters
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
    @DisplayName("TR corner has phase offset 4")
    void trCornerPhaseOffset() {
        var effect = new SparkleCornersEffect();
        // TR: phase 4, so at frame f shows CYCLE[(4 + f) % 16]
        for (int f = 0; f < 20; f++) {
            var ctx = newContext();
            effect.update(f, ctx);
            assertEquals(String.valueOf(CYCLE[(4 + f) % 16]),
                    ctx.getCorner(BorderContext.Corner.TR),
                    "TR at frame " + f);
        }
    }

    @Test
    @DisplayName("BR corner has phase offset 8")
    void brCornerPhaseOffset() {
        var effect = new SparkleCornersEffect();
        for (int f = 0; f < 20; f++) {
            var ctx = newContext();
            effect.update(f, ctx);
            assertEquals(String.valueOf(CYCLE[(8 + f) % 16]),
                    ctx.getCorner(BorderContext.Corner.BR),
                    "BR at frame " + f);
        }
    }

    @Test
    @DisplayName("BL corner has phase offset 12")
    void blCornerPhaseOffset() {
        var effect = new SparkleCornersEffect();
        for (int f = 0; f < 20; f++) {
            var ctx = newContext();
            effect.update(f, ctx);
            assertEquals(String.valueOf(CYCLE[(12 + f) % 16]),
                    ctx.getCorner(BorderContext.Corner.BL),
                    "BL at frame " + f);
        }
    }

    @Test
    @DisplayName("AnimatedBorder with sparkle effect renders corner cells")
    void sparkleEffectWithAnimatedBorder() {
        var effect = new SparkleCornersEffect();
        var border = new AnimatedBorder(
                new EmptySpace(new TerminalSize(8, 4)),
                effect
        );
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 6));

        // Simulate frame 0: TL='\u2598', TR='\u258C', BR='\u2588', BL='\u258C'
        var ctx = border.createContext();
        effect.update(0, ctx);
        var buffer = new ScreenBuffer(new TerminalSize(10, 6));
        border.drawWithBorderContext(new TextGraphics(buffer), ctx);

        // Corner cells should match the sparkle chars
        assertEquals('\u2598', buffer.getCell(0, 0).character().charAt(0), "TL corner");
        assertEquals('\u258C', buffer.getCell(9, 0).character().charAt(0), "TR corner");
        assertEquals('\u2588', buffer.getCell(9, 5).character().charAt(0), "BR corner");
        assertEquals('\u258C', buffer.getCell(0, 5).character().charAt(0), "BL corner");
    }

    @Test
    @DisplayName("AnimatedBorder with sparkle effect leaves edge cells unchanged")
    void sparkleEffectLeavesEdgeCells() {
        var effect = new SparkleCornersEffect();
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

        // Top edge (column 1) should be horizontal line, not a sparkle char
        assertEquals('\u2500', buffer.getCell(1, 0).character().charAt(0), "top edge");
        // Left edge (row 1) should be vertical line
        assertEquals('\u2502', buffer.getCell(0, 1).character().charAt(0), "left edge");
    }
}