package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.widget.AnimatedBorder;
import io.jterm.widget.Border;
import io.jterm.widget.EmptySpace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorPulseEffectTest {

    private static final TerminalSize SIZE = new TerminalSize(10, 6);

    /** The color sequence for 16-color terminals (per spec). */
    private static final AnsiColor[] COLORS = {
            AnsiColor.RED, AnsiColor.GREEN, AnsiColor.YELLOW,
            AnsiColor.BLUE, AnsiColor.MAGENTA, AnsiColor.CYAN, AnsiColor.WHITE
    };

    private BorderContext newContext() {
        return new BorderContext(SIZE, Border.BorderStyle.SINGLE_LINE);
    }

    // ---- name() ----

    @Test
    @DisplayName("name() returns color-pulse")
    void nameReturnsColorPulse() {
        var effect = new ColorPulseEffect();
        assertEquals("color-pulse", effect.name());
    }

    // ---- update() sets border color ----

    @Test
    @DisplayName("update() sets border color on context")
    void updateSetsBorderColor() {
        var effect = new ColorPulseEffect();
        var ctx = newContext();
        assertNull(ctx.getBorderColor(), "border color should be null before update");
        effect.update(0, ctx);
        assertNotNull(ctx.getBorderColor(), "border color should be set after update");
    }

    // ---- Color cycles through the sequence ----

    @Test
    @DisplayName("color starts at RED (index 0) on frame 0")
    void colorStartsAtRed() {
        var effect = new ColorPulseEffect(10);
        var ctx = newContext();
        effect.update(0, ctx);
        assertEquals(AnsiColor.RED, ctx.getBorderColor(),
                "frame 0 should be RED (first in cycle)");
    }

    @Test
    @DisplayName("color changes to GREEN after framesPerColor frames")
    void colorChangesToGreenAfterFramesPerColor() {
        var effect = new ColorPulseEffect(10);
        var ctx = newContext();
        effect.update(10, ctx);
        assertEquals(AnsiColor.GREEN, ctx.getBorderColor(),
                "frame 10 should be GREEN (second in cycle)");
    }

    @Test
    @DisplayName("color stays same within framesPerColor window")
    void colorStaysSameWithinWindow() {
        var effect = new ColorPulseEffect(10);
        // Frames 0-9 should all be RED
        for (int f = 0; f < 10; f++) {
            var ctx = newContext();
            effect.update(f, ctx);
            assertEquals(AnsiColor.RED, ctx.getBorderColor(),
                    "frame " + f + " should still be RED");
        }
    }

    @Test
    @DisplayName("color cycles through all 7 colors and wraps back to RED")
    void colorCyclesThroughAllColors() {
        var effect = new ColorPulseEffect(10);

        // Check each color in the sequence
        for (int i = 0; i < COLORS.length; i++) {
            var ctx = newContext();
            effect.update(i * 10L, ctx);
            assertEquals(COLORS[i], ctx.getBorderColor(),
                    "color at index " + i + " should be " + COLORS[i]);
        }

        // After full cycle (7 colors * 10 frames = 70 frames), wraps back to RED
        var ctx = newContext();
        effect.update(70L, ctx);
        assertEquals(AnsiColor.RED, ctx.getBorderColor(),
                "color should wrap back to RED after full cycle");
    }

    // ---- Border characters remain as base style ----

    @Test
    @DisplayName("update() does not modify corner characters")
    void updateDoesNotModifyCorners() {
        var effect = new ColorPulseEffect();
        var ctx = newContext();
        effect.update(0, ctx);

        assertEquals(Border.BorderStyle.SINGLE_LINE.topLeft(), ctx.getCorner(BorderContext.Corner.TL));
        assertEquals(Border.BorderStyle.SINGLE_LINE.topRight(), ctx.getCorner(BorderContext.Corner.TR));
        assertEquals(Border.BorderStyle.SINGLE_LINE.bottomLeft(), ctx.getCorner(BorderContext.Corner.BL));
        assertEquals(Border.BorderStyle.SINGLE_LINE.bottomRight(), ctx.getCorner(BorderContext.Corner.BR));
    }

    @Test
    @DisplayName("update() does not modify edge characters")
    void updateDoesNotModifyEdges() {
        var effect = new ColorPulseEffect();
        var ctx = newContext();
        effect.update(0, ctx);

        // Edges should still fall back to style defaults (no override)
        for (int i = 0; i < 8; i++) {
            assertEquals(Border.BorderStyle.SINGLE_LINE.horizontal(),
                    ctx.getEdge(BorderContext.Side.TOP, i),
                    "TOP edge " + i + " should be default horizontal");
            assertEquals(Border.BorderStyle.SINGLE_LINE.horizontal(),
                    ctx.getEdge(BorderContext.Side.BOTTOM, i),
                    "BOTTOM edge " + i + " should be default horizontal");
        }
        for (int i = 0; i < 4; i++) {
            assertEquals(Border.BorderStyle.SINGLE_LINE.vertical(),
                    ctx.getEdge(BorderContext.Side.LEFT, i),
                    "LEFT edge " + i + " should be default vertical");
            assertEquals(Border.BorderStyle.SINGLE_LINE.vertical(),
                    ctx.getEdge(BorderContext.Side.RIGHT, i),
                    "RIGHT edge " + i + " should be default vertical");
        }
    }

    // ---- Custom framesPerColor ----

    @Test
    @DisplayName("custom framesPerColor changes color transition rate")
    void customFramesPerColor() {
        var effect = new ColorPulseEffect(5); // 5 frames per color
        var ctx0 = newContext();
        effect.update(0, ctx0);
        assertEquals(AnsiColor.RED, ctx0.getBorderColor());

        var ctx4 = newContext();
        effect.update(4, ctx4);
        assertEquals(AnsiColor.RED, ctx4.getBorderColor(),
                "frame 4 should still be RED with framesPerColor=5");

        var ctx5 = newContext();
        effect.update(5, ctx5);
        assertEquals(AnsiColor.GREEN, ctx5.getBorderColor(),
                "frame 5 should be GREEN with framesPerColor=5");
    }

    // ---- AnimatedBorder integration: render border with color ----

    @Test
    @DisplayName("AnimatedBorder with color-pulse renders border cells with correct foreground color")
    void colorPulseWithAnimatedBorder() {
        var effect = new ColorPulseEffect(10);
        var border = new AnimatedBorder(
                new EmptySpace(new TerminalSize(8, 4)),
                effect
        );
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 6));

        var ctx = border.createContext();
        effect.update(0, ctx);
        var buffer = new ScreenBuffer(new TerminalSize(10, 6));
        border.drawWithBorderContext(new TextGraphics(buffer), ctx);

        // All border cells should have RED foreground (first color in cycle)
        assertEquals(AnsiColor.RED, buffer.getCell(0, 0).fg(),
                "TL corner should have RED foreground");
        assertEquals(AnsiColor.RED, buffer.getCell(1, 0).fg(),
                "top edge should have RED foreground");
        assertEquals(AnsiColor.RED, buffer.getCell(9, 0).fg(),
                "TR corner should have RED foreground");
        assertEquals(AnsiColor.RED, buffer.getCell(0, 1).fg(),
                "left edge should have RED foreground");
        assertEquals(AnsiColor.RED, buffer.getCell(9, 1).fg(),
                "right edge should have RED foreground");
    }

    @Test
    @DisplayName("AnimatedBorder with color-pulse preserves border characters")
    void colorPulsePreservesBorderCharacters() {
        var effect = new ColorPulseEffect(10);
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

        // Characters should still be the standard border characters
        assertEquals('┌', buffer.getCell(0, 0).character().charAt(0), "TL corner char");
        assertEquals('┐', buffer.getCell(9, 0).character().charAt(0), "TR corner char");
        assertEquals('─', buffer.getCell(1, 0).character().charAt(0), "top edge char");
        assertEquals('│', buffer.getCell(0, 1).character().charAt(0), "left edge char");
    }

    @Test
    @DisplayName("AnimatedBorder with color-pulse at frame 50 shows CYAN (index 5)")
    void colorPulseAtFrame50() {
        var effect = new ColorPulseEffect(10);
        var border = new AnimatedBorder(
                new EmptySpace(new TerminalSize(8, 4)),
                effect
        );
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 6));

        var ctx = border.createContext();
        effect.update(50, ctx);
        var buffer = new ScreenBuffer(new TerminalSize(10, 6));
        border.drawWithBorderContext(new TextGraphics(buffer), ctx);

        // Frame 50 / 10 = color index 5 = CYAN
        assertEquals(AnsiColor.CYAN, ctx.getBorderColor(),
                "frame 50 should show CYAN");
        assertEquals(AnsiColor.CYAN, buffer.getCell(0, 0).fg(),
                "TL corner should have CYAN foreground");
    }

    // ---- BorderContext color tracking ----

    @Test
    @DisplayName("BorderContext.resetToStyle() clears border color")
    void resetClearsBorderColor() {
        var ctx = new BorderContext(SIZE, Border.BorderStyle.SINGLE_LINE);
        ctx.setBorderColor(AnsiColor.RED);
        assertEquals(AnsiColor.RED, ctx.getBorderColor());

        ctx.resetToStyle();
        assertNull(ctx.getBorderColor(), "resetToStyle should clear border color");
    }

    @Test
    @DisplayName("BorderContext.setBorderColor stores and returns color")
    void setBorderColorStoresAndReturns() {
        var ctx = new BorderContext(SIZE, Border.BorderStyle.SINGLE_LINE);
        assertNull(ctx.getBorderColor());

        ctx.setBorderColor(AnsiColor.GREEN);
        assertEquals(AnsiColor.GREEN, ctx.getBorderColor());

        ctx.setBorderColor(AnsiColor.BLUE);
        assertEquals(AnsiColor.BLUE, ctx.getBorderColor());
    }

    // ---- Constructor validation ----

    @Test
    @DisplayName("constructor rejects framesPerColor < 1")
    void constructorRejectsInvalidFramesPerColor() {
        assertThrows(IllegalArgumentException.class,
                () -> new ColorPulseEffect(0),
                "Should reject framesPerColor 0");
        assertThrows(IllegalArgumentException.class,
                () -> new ColorPulseEffect(-1),
                "Should reject negative framesPerColor");
    }

    @Test
    @DisplayName("framesPerColor = 1 changes color every frame")
    void framesPerColorOne() {
        var effect = new ColorPulseEffect(1);
        var ctx0 = newContext();
        effect.update(0, ctx0);
        assertEquals(AnsiColor.RED, ctx0.getBorderColor(), "frame 0 → RED");

        var ctx1 = newContext();
        effect.update(1, ctx1);
        assertEquals(AnsiColor.GREEN, ctx1.getBorderColor(), "frame 1 → GREEN");

        var ctx2 = newContext();
        effect.update(2, ctx2);
        assertEquals(AnsiColor.YELLOW, ctx2.getBorderColor(), "frame 2 → YELLOW");
    }
}