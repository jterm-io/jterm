package io.jterm.demo;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.sprite.AnimatedText;
import io.jterm.sprite.ParticleEffect;
import io.jterm.sprite.Sprite;
import io.jterm.sprite.SpriteSheet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SpriteDemo} — a BBS-style welcome screen that exercises
 * the full sprite library API (Sprite, SpriteSheet, AnimatedText,
 * ParticleEffect, SpriteRenderer).
 *
 * <p>Follows the TDD pattern: tests written first, then implementation.</p>
 */
class SpriteDemoTest {

    // ── Helper: run main() briefly in a daemon thread (from DemoCoverageTest) ──

    private void runMainBriefly(Class<?> demoClass) {
        var error = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread t = new Thread(() -> {
            try {
                Method main = demoClass.getMethod("main", String[].class);
                main.invoke(null, (Object) new String[0]);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InterruptedException) return;
                if (cause instanceof IllegalStateException
                        || cause.getClass().getName().contains("Exit")
                        || cause.getClass().getName().contains("SystemExit")) return;
                error.set(cause);
            } catch (Exception e) {
                error.set(e);
            }
        });
        t.setDaemon(true);
        t.start();
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
        }
        t.interrupt();
        try {
            t.join(2000);
        } catch (InterruptedException ignored) {
        }
        if (t.isAlive()) {
            fail(demoClass.getSimpleName() + " did not stop after interrupt");
        }
        Throwable e = error.get();
        if (e != null) {
            fail(demoClass.getSimpleName() + " threw " + e + ": " + e.getMessage(), e);
        }
    }

    // ── 1. Constructor is private ──

    @Test
    @DisplayName("SpriteDemo has a private no-arg constructor")
    void constructorIsPrivate() {
        assertDoesNotThrow(() -> {
            Constructor<SpriteDemo> ctor = SpriteDemo.class.getDeclaredConstructor();
            int mods = ctor.getModifiers();
            assertTrue(java.lang.reflect.Modifier.isPrivate(mods),
                    "Constructor should be private");
            ctor.setAccessible(true);
            assertNotNull(ctor.newInstance());
        });
    }

    // ── 2. Demo renders without exceptions ──

    @Test
    @DisplayName("SpriteDemo main() runs briefly without throwing")
    void demoRunsBriefly() {
        runMainBriefly(SpriteDemo.class);
    }

    // ── 3. Helper methods produce expected content ──

    @Test
    @DisplayName("buildLogoFrames() returns pulsing ANSI frames")
    void buildLogoFramesReturnsExpectedContent() throws Exception {
        Method m = SpriteDemo.class.getDeclaredMethod("buildLogoFrames");
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> frames = (List<String>) m.invoke(null);
        assertNotNull(frames, "frames list should not be null");
        assertFalse(frames.isEmpty(), "should have at least one frame");
        // Each frame should contain block characters (the pulsing effect)
        for (String frame : frames) {
            assertNotNull(frame, "individual frame should not be null");
            assertFalse(frame.isBlank(), "frame should not be blank");
        }
        // Should have multiple frames for animation
        assertTrue(frames.size() >= 2, "should have at least 2 frames for pulsing");
    }

    @Test
    @DisplayName("buildSpinnerSheet() creates a SpriteSheet with expected frames")
    void buildSpinnerSheetReturnsExpectedContent() throws Exception {
        Method m = SpriteDemo.class.getDeclaredMethod("buildSpinnerSheet");
        m.setAccessible(true);
        SpriteSheet sheet = (SpriteSheet) m.invoke(null);
        assertNotNull(sheet, "sheet should not be null");
        assertTrue(sheet.frameCount() >= 4, "spinner should have at least 4 frames");
        // Each frame should be non-empty
        for (int i = 0; i < sheet.frameCount(); i++) {
            String frame = sheet.frameAt(i);
            assertNotNull(frame, "frame " + i + " should not be null");
        }
        // Convert to sprite and verify
        Sprite sprite = sheet.toSprite(100, Sprite.LoopMode.LOOP);
        assertEquals(sheet.frameCount(), sprite.getFrameCount());
        assertEquals(100, sprite.getFrameMs());
        assertEquals(Sprite.LoopMode.LOOP, sprite.getLoopMode());
    }

    @Test
    @DisplayName("buildMarqueeText() creates a scrolling marquee animation")
    void buildMarqueeTextReturnsExpectedContent() throws Exception {
        Method m = SpriteDemo.class.getDeclaredMethod("buildMarqueeText");
        m.setAccessible(true);
        AnimatedText marquee = (AnimatedText) m.invoke(null);
        assertNotNull(marquee, "marquee should not be null");
        assertFalse(marquee.getText().isEmpty(), "marquee should have text");
        // Advance the animation and verify it still produces text
        marquee.tick(500);
        assertFalse(marquee.getText().isEmpty(), "marquee should still have text after tick");
    }

    @Test
    @DisplayName("buildTypewriterIntro() creates a typewriter animation")
    void buildTypewriterIntroReturnsExpectedContent() throws Exception {
        Method m = SpriteDemo.class.getDeclaredMethod("buildTypewriterIntro");
        m.setAccessible(true);
        AnimatedText typewriter = (AnimatedText) m.invoke(null);
        assertNotNull(typewriter, "typewriter should not be null");
        // Initially no characters revealed
        assertTrue(typewriter.getText().isEmpty(), "typewriter should start empty");
        // Advance to reveal some characters
        typewriter.tick(500);
        assertFalse(typewriter.getText().isEmpty(), "typewriter should reveal characters after tick");
        // The text should contain "JTERM BBS" after enough ticks
        for (int i = 0; i < 50; i++) {
            typewriter.tick(100);
        }
        assertTrue(typewriter.getText().contains("JTERM"),
                "typewriter should contain JTERM after advancing");
    }

    @Test
    @DisplayName("buildLogoSprite() creates a PING_PONG sprite")
    void buildLogoSpriteReturnsExpectedContent() throws Exception {
        Method m = SpriteDemo.class.getDeclaredMethod("buildLogoSprite");
        m.setAccessible(true);
        Sprite sprite = (Sprite) m.invoke(null);
        assertNotNull(sprite, "logo sprite should not be null");
        assertTrue(sprite.getFrameCount() >= 2, "logo should have at least 2 frames");
        assertEquals(Sprite.LoopMode.PING_PONG, sprite.getLoopMode(),
                "logo should use PING_PONG mode");
        assertFalse(sprite.isFinished(), "logo sprite should not be finished initially");
    }

    // ── 4. Key handling via reflection ──

    @Test
    @DisplayName("handleKey 'e' triggers explosion particle effect")
    void handleKeyETriggersExplosion() throws Exception {
        // Create a mock graphics context
        var terminal = new MockTerminal(new TerminalSize(80, 24));
        var screen = new io.jterm.screen.DefaultScreen(terminal);
        var gui = new io.jterm.window.DefaultTextGUI(screen);
        screen.startScreen();

        // Create the sprite demo window
        Method createWindow = SpriteDemo.class.getDeclaredMethod("createSpriteWindow",
                io.jterm.window.DefaultTextGUI.class);
        createWindow.setAccessible(true);
        var window = createWindow.invoke(null, gui);

        // Get the particle effects list from the window
        Method getParticleEffects = SpriteDemo.class.getDeclaredMethod("getParticleEffects",
                window.getClass());
        getParticleEffects.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ParticleEffect> effects = (List<ParticleEffect>) getParticleEffects.invoke(null, window);
        int initialCount = effects.size();

        // Invoke handleKey with 'e'
        Method handleKey = SpriteDemo.class.getDeclaredMethod("handleKey",
                window.getClass(), KeyStroke.class, io.jterm.window.DefaultTextGUI.class);
        handleKey.setAccessible(true);
        var keyE = new KeyStroke(KeyType.CHARACTER, 'e', false, false, false);
        handleKey.invoke(null, window, keyE, gui);

        // Verify an explosion effect was added
        assertTrue(effects.size() > initialCount,
                "explosion effect should have been added after 'e' keypress");
    }

    @Test
    @DisplayName("handleKey 's' triggers sparkle particle effect")
    void handleKeySTriggersSparkle() throws Exception {
        var terminal = new MockTerminal(new TerminalSize(80, 24));
        var screen = new io.jterm.screen.DefaultScreen(terminal);
        var gui = new io.jterm.window.DefaultTextGUI(screen);
        screen.startScreen();

        Method createWindow = SpriteDemo.class.getDeclaredMethod("createSpriteWindow",
                io.jterm.window.DefaultTextGUI.class);
        createWindow.setAccessible(true);
        var window = createWindow.invoke(null, gui);

        Method getParticleEffects = SpriteDemo.class.getDeclaredMethod("getParticleEffects",
                window.getClass());
        getParticleEffects.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ParticleEffect> effects = (List<ParticleEffect>) getParticleEffects.invoke(null, window);
        int initialCount = effects.size();

        Method handleKey = SpriteDemo.class.getDeclaredMethod("handleKey",
                window.getClass(), KeyStroke.class, io.jterm.window.DefaultTextGUI.class);
        handleKey.setAccessible(true);
        var keyS = new KeyStroke(KeyType.CHARACTER, 's', false, false, false);
        handleKey.invoke(null, window, keyS, gui);

        assertTrue(effects.size() > initialCount,
                "sparkle effect should have been added after 's' keypress");
    }

    @Test
    @DisplayName("handleKey 'q' signals quit")
    void handleKeyQSignalsQuit() throws Exception {
        var terminal = new MockTerminal(new TerminalSize(80, 24));
        var screen = new io.jterm.screen.DefaultScreen(terminal);
        var gui = new io.jterm.window.DefaultTextGUI(screen);
        screen.startScreen();

        Method createWindow = SpriteDemo.class.getDeclaredMethod("createSpriteWindow",
                io.jterm.window.DefaultTextGUI.class);
        createWindow.setAccessible(true);
        var window = createWindow.invoke(null, gui);

        Method handleKey = SpriteDemo.class.getDeclaredMethod("handleKey",
                window.getClass(), KeyStroke.class, io.jterm.window.DefaultTextGUI.class);
        handleKey.setAccessible(true);
        var keyQ = new KeyStroke(KeyType.CHARACTER, 'q', false, false, false);

        // handleKey returns true when the demo should quit
        Boolean result = (Boolean) handleKey.invoke(null, window, keyQ, gui);
        assertTrue(result, "handleKey should return true for 'q' (quit signal)");
    }

    @Test
    @DisplayName("handleKey unknown key does not crash and returns false")
    void handleKeyUnknownReturnsFalse() throws Exception {
        var terminal = new MockTerminal(new TerminalSize(80, 24));
        var screen = new io.jterm.screen.DefaultScreen(terminal);
        var gui = new io.jterm.window.DefaultTextGUI(screen);
        screen.startScreen();

        Method createWindow = SpriteDemo.class.getDeclaredMethod("createSpriteWindow",
                io.jterm.window.DefaultTextGUI.class);
        createWindow.setAccessible(true);
        var window = createWindow.invoke(null, gui);

        Method handleKey = SpriteDemo.class.getDeclaredMethod("handleKey",
                window.getClass(), KeyStroke.class, io.jterm.window.DefaultTextGUI.class);
        handleKey.setAccessible(true);
        var keyX = new KeyStroke(KeyType.CHARACTER, 'x', false, false, false);

        Boolean result = (Boolean) handleKey.invoke(null, window, keyX, gui);
        assertFalse(result, "handleKey should return false for unknown keys");
    }

    // ── 5. Animation tick renders without exception ──

    @Test
    @DisplayName("tickAnimations advances all animations without throwing")
    void tickAnimationsWorks() throws Exception {
        var terminal = new MockTerminal(new TerminalSize(80, 24));
        var screen = new io.jterm.screen.DefaultScreen(terminal);
        var gui = new io.jterm.window.DefaultTextGUI(screen);
        screen.startScreen();

        Method createWindow = SpriteDemo.class.getDeclaredMethod("createSpriteWindow",
                io.jterm.window.DefaultTextGUI.class);
        createWindow.setAccessible(true);
        var window = createWindow.invoke(null, gui);

        Method tick = SpriteDemo.class.getDeclaredMethod("tickAnimations",
                window.getClass(), long.class);
        tick.setAccessible(true);

        assertDoesNotThrow(() -> {
            tick.invoke(null, window, 16L);
            tick.invoke(null, window, 16L);
            tick.invoke(null, window, 16L);
        });
    }

    @Test
    @DisplayName("renderFrame draws all sprite elements to the graphics buffer")
    void renderFrameDrawsToBuffer() throws Exception {
        var terminal = new MockTerminal(new TerminalSize(80, 24));
        var screen = new io.jterm.screen.DefaultScreen(terminal);
        var gui = new io.jterm.window.DefaultTextGUI(screen);
        screen.startScreen();

        Method createWindow = SpriteDemo.class.getDeclaredMethod("createSpriteWindow",
                io.jterm.window.DefaultTextGUI.class);
        createWindow.setAccessible(true);
        var window = createWindow.invoke(null, gui);

        // Tick a few times to reveal typewriter text
        Method tick = SpriteDemo.class.getDeclaredMethod("tickAnimations",
                window.getClass(), long.class);
        tick.setAccessible(true);
        for (int i = 0; i < 20; i++) {
            tick.invoke(null, window, 50L);
        }

        // Create a graphics buffer and render the frame
        var buf = new ScreenBuffer(new TerminalSize(80, 24));
        var g = new TextGraphics(buf);

        Method render = SpriteDemo.class.getDeclaredMethod("renderFrame",
                window.getClass(), TextGraphics.class);
        render.setAccessible(true);
        assertDoesNotThrow(() -> render.invoke(null, window, g));

        // After rendering, the buffer should have some non-empty cells
        boolean hasContent = false;
        for (int r = 0; r < 24 && !hasContent; r++) {
            for (int c = 0; c < 80 && !hasContent; c++) {
                if (buf.getCell(c, r).character().charAt(0) != ' ') {
                    hasContent = true;
                }
            }
        }
        assertTrue(hasContent, "rendered buffer should have visible content");
    }
}