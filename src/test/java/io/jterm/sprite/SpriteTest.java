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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Sprite} — animated sprite with multiple ANSI frames.
 */
class SpriteTest {

    @Test
    @DisplayName("Sprite has default frame timing and loop mode")
    void defaultsAndMutators() {
        var sprite = new Sprite();
        assertEquals(Sprite.LoopMode.LOOP, sprite.getLoopMode());
        assertTrue(sprite.getFrameCount() == 0);
        assertEquals(Sprite.DEFAULT_FRAME_MS, sprite.getFrameMs());
        sprite.setFrameMs(150);
        assertEquals(150, sprite.getFrameMs());
        sprite.setLoopMode(Sprite.LoopMode.ONCE);
        assertEquals(Sprite.LoopMode.ONCE, sprite.getLoopMode());
    }

    @Test
    @DisplayName("addFrame stores raw ANSI string and tracks size")
    void addFrameStoresAnsiString() {
        var sprite = new Sprite();
        sprite.addFrame("AB\nCD");
        assertEquals(1, sprite.getFrameCount());
        assertEquals(2, sprite.getWidth());
        assertEquals(2, sprite.getHeight());
    }

    @Test
    @DisplayName("addFrame larger frame updates width/height")
    void largerFrameGrowsSize() {
        var sprite = new Sprite();
        sprite.addFrame("AB\nCD");
        sprite.addFrame("XYZ");
        // width grows to max across frames (3), height grows to 2
        assertEquals(3, sprite.getWidth());
        assertEquals(2, sprite.getHeight());
    }

    @Test
    @DisplayName("addFrame rejects null")
    void addFrameRejectsNull() {
        var sprite = new Sprite();
        assertThrows(NullPointerException.class, () -> sprite.addFrame(null));
    }

    @Test
    @DisplayName("fromFiles loads multiple ANSI files into frames")
    void fromFilesLoadsMultiple() throws Exception {
        Path dir = Files.createTempDirectory("sprite-frames");
        Path f1 = dir.resolve("f1.ans");
        Path f2 = dir.resolve("f2.ans");
        Files.writeString(f1, "A\nB");
        Files.writeString(f2, "C\nD");
        var sprite = Sprite.fromFiles(List.of(f1, f2));
        assertEquals(2, sprite.getFrameCount());
        Files.deleteIfExists(f1);
        Files.deleteIfExists(f2);
        Files.deleteIfExists(dir);
    }

    @Test
    @DisplayName("fromFiles rejects empty list")
    void fromFilesRejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Sprite.fromFiles(List.of()));
    }

    @Test
    @DisplayName("ONCE loop: advances to last frame then stays")
    void onceLoopStopsAtLast() {
        var sprite = new Sprite();
        sprite.addFrame("A");
        sprite.addFrame("B");
        sprite.addFrame("C");
        sprite.setLoopMode(Sprite.LoopMode.ONCE);
        sprite.setFrameMs(10);
        assertEquals(0, sprite.getCurrentFrameIndex());
        sprite.advance(5); // < 10ms, no advance
        assertEquals(0, sprite.getCurrentFrameIndex());
        sprite.advance(10);
        assertEquals(1, sprite.getCurrentFrameIndex());
        sprite.advance(10);
        assertEquals(2, sprite.getCurrentFrameIndex());
        sprite.advance(10); // would be frame 3 but ONCE clamps
        assertEquals(2, sprite.getCurrentFrameIndex());
    }

    @Test
    @DisplayName("LOOP mode wraps around")
    void loopWraps() {
        var sprite = new Sprite();
        sprite.addFrame("A");
        sprite.addFrame("B");
        sprite.setLoopMode(Sprite.LoopMode.LOOP);
        sprite.setFrameMs(10);
        sprite.advance(10);
        assertEquals(1, sprite.getCurrentFrameIndex());
        sprite.advance(10);
        assertEquals(0, sprite.getCurrentFrameIndex()); // wraps 2->0
        sprite.advance(10);
        assertEquals(1, sprite.getCurrentFrameIndex()); // forward again
    }

    @Test
    @DisplayName("PING_PONG mode reverses direction at ends")
    void pingPongReverses() {
        var sprite = new Sprite();
        sprite.addFrame("A");
        sprite.addFrame("B");
        sprite.addFrame("C");
        sprite.setLoopMode(Sprite.LoopMode.PING_PONG);
        sprite.setFrameMs(10);
        // forward: 0 -> 1 -> 2
        sprite.advance(10);
        assertEquals(1, sprite.getCurrentFrameIndex());
        sprite.advance(10);
        assertEquals(2, sprite.getCurrentFrameIndex());
        sprite.advance(10);
        assertEquals(1, sprite.getCurrentFrameIndex()); // reversed
        sprite.advance(10);
        assertEquals(0, sprite.getCurrentFrameIndex());
        sprite.advance(10);
        assertEquals(1, sprite.getCurrentFrameIndex()); // forward again
    }

    @Test
    @DisplayName("reset returns to first frame")
    void resetReturnsToFirst() {
        var sprite = new Sprite();
        sprite.addFrame("A");
        sprite.addFrame("B");
        sprite.advance(1000);
        sprite.reset();
        assertEquals(0, sprite.getCurrentFrameIndex());
    }

    @Test
    @DisplayName("getCurrentFrame returns the ANSI string for current frame")
    void getCurrentFrameReturnsAnsiString() {
        var sprite = new Sprite();
        sprite.addFrame("AAA\nBBB");
        sprite.addFrame("CCC\nDDD");
        assertEquals("AAA\nBBB", sprite.getCurrentFrame());
        sprite.advance(100);
        assertEquals("CCC\nDDD", sprite.getCurrentFrame());
    }

    @Test
    @DisplayName("render current frame to TextGraphics at position")
    void renderCurrentFrameToGraphics() {
        var sprite = new Sprite();
        sprite.addFrame("\u001B[31mA\u001B[0m");
        var size = new TerminalSize(10, 5);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        sprite.render(g, 2, 3);
        var cell = buffer.getCell(2, 3);
        assertEquals('A', cell.character().charAt(0));
        assertEquals(AnsiColor.RED, cell.fg());
    }

    @Test
    @DisplayName("isFinished true when ONCE reaches last frame")
    void isFinishedOnceLastFrame() {
        var sprite = new Sprite();
        sprite.addFrame("A");
        sprite.addFrame("B");
        sprite.setLoopMode(Sprite.LoopMode.ONCE);
        sprite.setFrameMs(10);
        sprite.advance(10);
        sprite.advance(10);
        // at last frame, should be finished
        assertTrue(sprite.isFinished());
    }

    @Test
    @DisplayName("isFinished false for LOOP/PING_PONG")
    void isFinishedFalseForLoop() {
        var sprite = new Sprite();
        sprite.addFrame("A");
        sprite.addFrame("B");
        sprite.setLoopMode(Sprite.LoopMode.LOOP);
        sprite.advance(1000);
        assertFalse(sprite.isFinished());

        sprite.setLoopMode(Sprite.LoopMode.PING_PONG);
        sprite.reset();
        sprite.advance(1000);
        assertFalse(sprite.isFinished());
    }

    @Test
    @DisplayName("isFinished false for ONCE before last frame")
    void isFinishedFalseBeforeLast() {
        var sprite = new Sprite();
        sprite.addFrame("A");
        sprite.addFrame("B");
        sprite.setLoopMode(Sprite.LoopMode.ONCE);
        sprite.setFrameMs(10);
        sprite.advance(10);
        assertFalse(sprite.isFinished()); // still at frame 1
    }
}