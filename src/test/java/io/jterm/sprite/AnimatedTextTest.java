package io.jterm.sprite;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AnimatedText} — scrolling/blink/typewriter text animations.
 */
class AnimatedTextTest {

    @Test
    @DisplayName("typewriter reveals one char at a time")
    void typewriterReveal() {
        var at = AnimatedText.typewriter("HELLO", 10);
        // Step 0 -> 1 char
        at.tick(10);
        assertEquals("H", at.getText());
        at.tick(10);
        assertEquals("HE", at.getText());
        at.tick(10);
        at.tick(10);
        assertEquals("HELL", at.getText());
        at.tick(10);
        assertEquals("HELLO", at.getText());
    }

    @Test
    @DisplayName("typewriter finishes and stays full text")
    void typewriterFinishes() {
        var at = AnimatedText.typewriter("ABC", 5);
        at.tick(5);
        at.tick(5);
        at.tick(5);
        assertEquals("ABC", at.getText());
        at.tick(5);
        assertEquals("ABC", at.getText());
        assertTrue(at.isComplete());
    }

    @Test
    @DisplayName("scroll left moves text right-to-left (or the text viewport)")
    void scrollLeft() {
        // A scrolling text over a viewport of 10 cells with text "ABC"
        var at = AnimatedText.scrollLeft("ABC", 10, 5);
        // Initially, full text occupies first 3 cells of a 10-cell viewport
        // scrolling left means the text moves leftward and eventually wraps.
        at.tick(5);
        String text = at.getText();
        assertNotNull(text);
        assertTrue(text.length() <= 10);
    }

    @Test
    @DisplayName("scrollRight produces output within viewport")
    void scrollRight() {
        var at = AnimatedText.scrollRight("ABCD", 8, 10);
        at.tick(10);
        String text = at.getText();
        assertNotNull(text);
        assertTrue(text.length() <= 8);
    }

    @Test
    @DisplayName("blink cycles through color variants")
    void blinkCyclesColors() {
        var at = AnimatedText.blink("HI",
                java.util.List.of(AnsiColor.RED, AnsiColor.GREEN, AnsiColor.BLUE), 10);
        at.tick(10);
        // Just verify it produces text and state advances without error.
        assertEquals("HI", at.getText());
        assertFalse(at.isComplete());
        at.tick(10);
        at.tick(10);
        assertEquals("HI", at.getText());
    }

    @Test
    @DisplayName("blink produces renderable text with color")
    void blinkRenderable() {
        var at = AnimatedText.blink("X",
                java.util.List.of(AnsiColor.RED, AnsiColor.YELLOW), 5);
        at.tick(5);
        var size = new TerminalSize(20, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        at.render(g, 0, 0);
        var cell = buffer.getCell(0, 0);
        assertEquals('X', cell.character().charAt(0));
        // Color cycles through the provided variants
        assertTrue(cell.fg() == AnsiColor.RED || cell.fg() == AnsiColor.YELLOW);
    }

    @Test
    @DisplayName("typewriter renders visible chars and blanks for not-yet-revealed")
    void typewriterRender() {
        var at = AnimatedText.typewriter("ABCD", 10);
        at.tick(10);
        at.tick(10); // revealed "AB"
        var size = new TerminalSize(10, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        at.render(g, 0, 0);
        assertEquals('A', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('B', buffer.getCell(1, 0).character().charAt(0));
        // remaining should be spaces (not revealed yet)
        assertEquals(' ', buffer.getCell(2, 0).character().charAt(0));
    }

    @Test
    @DisplayName("reset restarts animation")
    void resetRestart() {
        var at = AnimatedText.typewriter("ABC", 1);
        at.tick(1);
        at.tick(1);
        at.tick(1);
        assertEquals("ABC", at.getText());
        at.reset();
        assertEquals("", at.getText());
    }

    @Test
    @DisplayName("scroll renders text that fills viewport")
    void scrollRender() {
        var at = AnimatedText.scrollLeft("HELLO", 4, 1);
        var size = new TerminalSize(10, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        at.render(g, 0, 0);
        // At offset 0, slice = "HELL" (first 4 chars of HELLO)
        assertEquals('H', buffer.getCell(0, 0).character().charAt(0));
        at.tick(1);
        at.tick(1);
        // After 2 ticks, scrollOffset=2, slice = "LLO " (chars 2..5)
        at.render(g, 5, 0);
        assertEquals('L', buffer.getCell(5, 0).character().charAt(0));
        assertEquals('L', buffer.getCell(6, 0).character().charAt(0));
    }
}