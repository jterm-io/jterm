package io.jterm.widget;

import io.jterm.animation.AnimatedBorderEffect;
import io.jterm.animation.BorderContext;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.util.Symbols;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimatedBorderTest {

    /** Trivial effect that records frame numbers for testing. */
    static class RecordingEffect implements AnimatedBorderEffect {
        final List<Long> frames = new ArrayList<>();
        final String name;

        RecordingEffect(String name) { this.name = name; }

        @Override
        public void update(long frame, BorderContext ctx) {
            frames.add(frame);
        }

        @Override
        public String name() { return name; }
    }

    /** Effect that sets custom corner chars on odd frames. */
    static class CornerFlipEffect implements AnimatedBorderEffect {
        @Override
        public void update(long frame, BorderContext ctx) {
            if (frame % 2 == 1) {
                ctx.setCorner(BorderContext.Corner.TL, '*');
                ctx.setCorner(BorderContext.Corner.TR, '*');
                ctx.setCorner(BorderContext.Corner.BL, '*');
                ctx.setCorner(BorderContext.Corner.BR, '*');
            }
        }

        @Override
        public String name() { return "corner-flip"; }
    }

    /** Effect that sets custom edge chars. */
    static class EdgeDashEffect implements AnimatedBorderEffect {
        @Override
        public void update(long frame, BorderContext ctx) {
            // Top edge position 0 = dash
            ctx.setEdge(BorderContext.Side.TOP, 0, '-');
            // Bottom edge position 0 = dash
            ctx.setEdge(BorderContext.Side.BOTTOM, 0, '_');
            // Left edge position 0 = dash
            ctx.setEdge(BorderContext.Side.LEFT, 0, '<');
            // Right edge position 0 = dash
            ctx.setEdge(BorderContext.Side.RIGHT, 0, '>');
        }

        @Override
        public String name() { return "edge-dash"; }
    }

    private TextGraphics graphicsFor(TerminalSize size) {
        return new TextGraphics(new ScreenBuffer(size));
    }

    // ---- BorderContext tests ----

    @Test
    @DisplayName("BorderContext exposes border size")
    void borderContextExposesSize() {
        var ctx = new BorderContext(new TerminalSize(10, 6), Border.BorderStyle.SINGLE_LINE);
        assertEquals(new TerminalSize(10, 6), ctx.getSize());
    }

    @Test
    @DisplayName("BorderContext corners default to style characters")
    void defaultCornersMatchStyle() {
        var ctx = new BorderContext(new TerminalSize(10, 6), Border.BorderStyle.SINGLE_LINE);
        assertEquals(Symbols.TL_CORNER, ctx.getCorner(BorderContext.Corner.TL));
        assertEquals(Symbols.TR_CORNER, ctx.getCorner(BorderContext.Corner.TR));
        assertEquals(Symbols.BL_CORNER, ctx.getCorner(BorderContext.Corner.BL));
        assertEquals(Symbols.BR_CORNER, ctx.getCorner(BorderContext.Corner.BR));
    }

    @Test
    @DisplayName("BorderContext setCorner overrides the corner char")
    void setCornerOverridesCorner() {
        var ctx = new BorderContext(new TerminalSize(10, 6), Border.BorderStyle.SINGLE_LINE);
        ctx.setCorner(BorderContext.Corner.TL, '#');
        assertEquals("#", ctx.getCorner(BorderContext.Corner.TL));
    }

    @Test
    @DisplayName("BorderContext edges default to style characters")
    void defaultEdgesMatchStyle() {
        var ctx = new BorderContext(new TerminalSize(10, 6), Border.BorderStyle.SINGLE_LINE);
        assertEquals(Symbols.H_LINE, ctx.getEdge(BorderContext.Side.TOP, 0));
        assertEquals(Symbols.V_LINE, ctx.getEdge(BorderContext.Side.LEFT, 0));
    }

    @Test
    @DisplayName("BorderContext setEdge overrides edge chars")
    void setEdgeOverridesEdge() {
        var ctx = new BorderContext(new TerminalSize(10, 6), Border.BorderStyle.SINGLE_LINE);
        ctx.setEdge(BorderContext.Side.TOP, 1, "=");
        assertEquals("=", ctx.getEdge(BorderContext.Side.TOP, 1));
    }

    @Test
    @DisplayName("BorderContext resetToStyle restores defaults")
    void resetToStyleRestoresDefaults() {
        var ctx = new BorderContext(new TerminalSize(10, 6), Border.BorderStyle.SINGLE_LINE);
        ctx.setCorner(BorderContext.Corner.TL, '#');
        ctx.setEdge(BorderContext.Side.TOP, 0, '=');
        ctx.resetToStyle();
        assertEquals(Symbols.TL_CORNER, ctx.getCorner(BorderContext.Corner.TL));
        assertEquals(Symbols.H_LINE, ctx.getEdge(BorderContext.Side.TOP, 0));
    }

    @Test
    @DisplayName("BorderContext resetToStyle with DOUBLE_LINE restores double chars")
    void resetToStyleRestoresDoubleLine() {
        var ctx = new BorderContext(new TerminalSize(10, 6), Border.BorderStyle.DOUBLE_LINE);
        ctx.setCorner(BorderContext.Corner.TL, '#');
        ctx.resetToStyle();
        assertEquals(Symbols.TL_DOUBLE, ctx.getCorner(BorderContext.Corner.TL));
    }

    // ---- AnimatedBorder lifecycle tests ----

    @Test
    @DisplayName("AnimatedBorder starts and stops timer cleanly")
    void startsStopsTimer() throws InterruptedException {
        var effect = new RecordingEffect("rec");
        var border = new AnimatedBorder(new EmptySpace(new TerminalSize(4, 3)), effect);
        assertFalse(border.isAnimating());
        border.start();
        assertTrue(border.isAnimating());
        Thread.sleep(150); // let at least one frame fire
        border.stop();
        assertFalse(border.isAnimating());
    }

    @Test
    @DisplayName("AnimatedBorder effect.update() is called on each frame")
    void effectUpdateIsCalled() throws InterruptedException {
        var effect = new RecordingEffect("rec");
        var border = new AnimatedBorder(new EmptySpace(new TerminalSize(4, 3)), effect);
        border.start();
        Thread.sleep(300); // ~3 frames at 10fps
        border.stop();
        assertTrue(effect.frames.size() >= 2, "expected >= 2 frames, got " + effect.frames.size());
    }

    @Test
    @DisplayName("AnimatedBorder frame counter increments")
    void frameCounterIncrements() throws InterruptedException {
        var effect = new RecordingEffect("rec");
        var border = new AnimatedBorder(new EmptySpace(new TerminalSize(4, 3)), effect);
        border.start();
        Thread.sleep(300);
        border.stop();
        // Frames should be increasing
        if (effect.frames.size() >= 2) {
            assertTrue(effect.frames.get(effect.frames.size() - 1) > effect.frames.get(0),
                    "frame counter should increase over time");
        }
    }

    @Test
    @DisplayName("AnimatedBorder stop is idempotent")
    void stopIsIdempotent() {
        var effect = new RecordingEffect("rec");
        var border = new AnimatedBorder(new EmptySpace(new TerminalSize(4, 3)), effect);
        border.stop(); // should not throw
        border.stop(); // double stop should also be safe
    }

    // ---- AnimatedBorder rendering tests ----

    @Test
    @DisplayName("AnimatedBorder renders custom corner chars from effect")
    void customCornersRendered() {
        var effect = new CornerFlipEffect();
        var border = new AnimatedBorder(new EmptySpace(new TerminalSize(4, 3)), effect);
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(6, 5));

        // Simulate frame 1 (odd) — effect sets corners to '*'
        var ctx = border.createContext();
        effect.update(1, ctx);
        var buffer = new ScreenBuffer(new TerminalSize(6, 5));
        border.drawWithBorderContext(new TextGraphics(buffer), ctx);

        assertEquals('*', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('*', buffer.getCell(5, 0).character().charAt(0));
        assertEquals('*', buffer.getCell(0, 4).character().charAt(0));
        assertEquals('*', buffer.getCell(5, 4).character().charAt(0));
    }

    @Test
    @DisplayName("AnimatedBorder renders custom edge chars from effect")
    void customEdgesRendered() {
        var effect = new EdgeDashEffect();
        var border = new AnimatedBorder(new EmptySpace(new TerminalSize(4, 3)), effect);
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(6, 5));

        var ctx = border.createContext();
        effect.update(0, ctx);
        var buffer = new ScreenBuffer(new TerminalSize(6, 5));
        border.drawWithBorderContext(new TextGraphics(buffer), ctx);

        // Top edge position 0 should be '-'
        assertEquals('-', buffer.getCell(1, 0).character().charAt(0));
        // Bottom edge position 0 should be '_'
        assertEquals('_', buffer.getCell(1, 4).character().charAt(0));
        // Left edge position 0 should be '<'
        assertEquals('<', buffer.getCell(0, 1).character().charAt(0));
        // Right edge position 0 should be '>'
        assertEquals('>', buffer.getCell(5, 1).character().charAt(0));
    }

    @Test
    @DisplayName("AnimatedBorder resetToStyle restores original border rendering")
    void resetToStyleRestoresRendering() {
        var effect = new CornerFlipEffect();
        var border = new AnimatedBorder(new EmptySpace(new TerminalSize(4, 3)), effect);
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(6, 5));

        // Frame 1 — corners are '*'
        var ctx = border.createContext();
        effect.update(1, ctx);

        // Reset — corners should be back to style defaults
        ctx.resetToStyle();
        var buffer = new ScreenBuffer(new TerminalSize(6, 5));
        border.drawWithBorderContext(new TextGraphics(buffer), ctx);

        assertEquals('┌', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    @DisplayName("AnimatedBorder with explicit BorderStyle")
    void withExplicitBorderStyle() {
        var effect = new RecordingEffect("rec");
        var border = new AnimatedBorder(
                new EmptySpace(new TerminalSize(4, 3)),
                Border.BorderStyle.DOUBLE_LINE,
                effect
        );
        assertEquals(new TerminalSize(6, 5), border.getPreferredSize());

        var ctx = border.createContext();
        assertEquals(Symbols.TL_DOUBLE, ctx.getCorner(BorderContext.Corner.TL));
    }

    @Test
    @DisplayName("AnimatedBorder defaults to SINGLE_LINE style")
    void defaultsToSingleLineStyle() {
        var effect = new RecordingEffect("rec");
        var border = new AnimatedBorder(new EmptySpace(new TerminalSize(4, 3)), effect);
        var ctx = border.createContext();
        assertEquals(Symbols.TL_CORNER, ctx.getCorner(BorderContext.Corner.TL));
    }

    // ---- Borders.animated() factory tests ----

    @Test
    @DisplayName("Borders.animated creates AnimatedBorder with SINGLE_LINE")
    void bordersAnimatedSingleLine() {
        var effect = new RecordingEffect("test");
        var border = Borders.animated(new EmptySpace(new TerminalSize(4, 3)), effect);
        assertInstanceOf(AnimatedBorder.class, border);
        var ctx = ((AnimatedBorder) border).createContext();
        assertEquals(Symbols.TL_CORNER, ctx.getCorner(BorderContext.Corner.TL));
    }

    @Test
    @DisplayName("Borders.animated with explicit style")
    void bordersAnimatedWithStyle() {
        var effect = new RecordingEffect("test");
        var border = Borders.animated(
                new EmptySpace(new TerminalSize(4, 3)),
                Border.BorderStyle.DOUBLE_LINE,
                effect
        );
        assertInstanceOf(AnimatedBorder.class, border);
        var ctx = ((AnimatedBorder) border).createContext();
        assertEquals(Symbols.TL_DOUBLE, ctx.getCorner(BorderContext.Corner.TL));
    }

    // ---- AnimatedBorderEffect interface contract ----

    @Test
    @DisplayName("AnimatedBorderEffect name() returns expected name")
    void effectName() {
        var effect = new RecordingEffect("my-effect");
        assertEquals("my-effect", effect.name());
    }

    @Test
    @DisplayName("BorderContext.Side enum has all four sides")
    void sideEnumComplete() {
        assertEquals(4, BorderContext.Side.values().length);
        assertNotNull(BorderContext.Side.valueOf("TOP"));
        assertNotNull(BorderContext.Side.valueOf("BOTTOM"));
        assertNotNull(BorderContext.Side.valueOf("LEFT"));
        assertNotNull(BorderContext.Side.valueOf("RIGHT"));
    }

    @Test
    @DisplayName("BorderContext.Corner enum has all four corners")
    void cornerEnumComplete() {
        assertEquals(4, BorderContext.Corner.values().length);
        assertNotNull(BorderContext.Corner.valueOf("TL"));
        assertNotNull(BorderContext.Corner.valueOf("TR"));
        assertNotNull(BorderContext.Corner.valueOf("BL"));
        assertNotNull(BorderContext.Corner.valueOf("BR"));
    }
}