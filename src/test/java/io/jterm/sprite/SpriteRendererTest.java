package io.jterm.sprite;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SpriteRenderer} — renders sprites to screen with timer-based
 * animation.
 */
class SpriteRendererTest {

    @Test
    @DisplayName("render draws current frame to graphics buffer at position")
    void renderDrawsFrame() {
        var sprite = new Sprite();
        sprite.addFrame("\u001B[31mA\u001B[0m");
        var size = new TerminalSize(10, 5);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        var renderer = new SpriteRenderer(g, size);
        renderer.render(sprite, 2, 3);
        var cell = buffer.getCell(2, 3);
        assertEquals('A', cell.character().charAt(0));
        assertEquals(AnsiColor.RED, cell.fg());
    }

    @Test
    @DisplayName("render clears previous frame region when sprite moves")
    void renderClearsPreviousRegion() {
        var sprite = new Sprite();
        sprite.addFrame("AB\nCD");
        var size = new TerminalSize(20, 10);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        var renderer = new SpriteRenderer(g, size);
        renderer.render(sprite, 0, 0);
        // verify A is at (0,0)
        assertEquals('A', buffer.getCell(0, 0).character().charAt(0));
        // now render at new position — previous region should be cleared
        renderer.render(sprite, 5, 5);
        assertEquals(' ', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('A', buffer.getCell(5, 5).character().charAt(0));
    }

    @Test
    @DisplayName("playOnce fires callback after animation completes")
    void playOnceFiresCallback() throws Exception {
        var sprite = new Sprite();
        sprite.addFrame("A");
        sprite.addFrame("B");
        sprite.addFrame("C");
        sprite.setLoopMode(Sprite.LoopMode.ONCE);
        sprite.setFrameMs(20);
        var size = new TerminalSize(20, 10);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        var renderer = new SpriteRenderer(g, size);

        var fired = new CountDownLatch(1);
        var handle = renderer.playOnce(sprite, 0, 0, fired::countDown);
        assertTrue(fired.await(3, TimeUnit.SECONDS), "callback should fire within 3s");
        handle.stop();
    }

    @Test
    @DisplayName("playLoop returns handle that can stop the loop")
    void playLoopCanBeStopped() throws Exception {
        var sprite = new Sprite();
        sprite.addFrame("A");
        sprite.addFrame("B");
        sprite.setLoopMode(Sprite.LoopMode.LOOP);
        sprite.setFrameMs(10);
        var size = new TerminalSize(20, 10);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        var renderer = new SpriteRenderer(g, size);

        var handle = renderer.playLoop(sprite, 0, 0);
        assertTrue(handle.isRunning());
        // let it run a bit
        Thread.sleep(100);
        handle.stop();
        assertFalse(handle.isRunning());
    }

    @Test
    @DisplayName("multiple playLoop handles run concurrently")
    void multipleLoopsConcurrently() throws Exception {
        var s1 = new Sprite();
        s1.addFrame("A");
        s1.addFrame("B");
        s1.setFrameMs(10);
        var s2 = new Sprite();
        s2.addFrame("X");
        s2.addFrame("Y");
        s2.setFrameMs(10);
        var size = new TerminalSize(40, 10);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        var renderer = new SpriteRenderer(g, size);
        var h1 = renderer.playLoop(s1, 0, 0);
        var h2 = renderer.playLoop(s2, 10, 0);
        assertTrue(h1.isRunning());
        assertTrue(h2.isRunning());
        Thread.sleep(100);
        h1.stop();
        h2.stop();
        assertFalse(h1.isRunning());
        assertFalse(h2.isRunning());
    }

    @Test
    @DisplayName("stop all clears running handles")
    void stopAllClears() throws Exception {
        var sprite = new Sprite();
        sprite.addFrame("A");
        sprite.setLoopMode(Sprite.LoopMode.LOOP);
        sprite.setFrameMs(10);
        var size = new TerminalSize(40, 10);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        var renderer = new SpriteRenderer(g, size);
        var h1 = renderer.playLoop(sprite, 0, 0);
        var h2 = renderer.playLoop(sprite, 5, 0);
        Thread.sleep(50);
        renderer.stopAll();
        assertFalse(h1.isRunning());
        assertFalse(h2.isRunning());
    }

    @Test
    @DisplayName("playOnce with single-frame sprite fires immediately-ish")
    void playOnceSingleFrame() throws Exception {
        var sprite = new Sprite();
        sprite.addFrame("Z");
        sprite.setLoopMode(Sprite.LoopMode.ONCE);
        sprite.setFrameMs(5);
        var size = new TerminalSize(10, 5);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        var renderer = new SpriteRenderer(g, size);
        var fired = new CountDownLatch(1);
        var handle = renderer.playOnce(sprite, 0, 0, fired::countDown);
        assertTrue(fired.await(2, TimeUnit.SECONDS));
        handle.stop();
    }
}