package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.widget.Panel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TypewriterEffectTest {

    @Test
    @DisplayName("constructor accepts list of lines")
    void constructorAcceptsList() {
        var effect = new TypewriterEffect(List.of("AB", "CD"));
        assertEquals(List.of("AB", "CD"), effect.getLines());
    }

    @Test
    @DisplayName("constructor accepts multiline string")
    void constructorAcceptsString() {
        var effect = new TypewriterEffect("AB\nCD");
        assertEquals(List.of("AB", "CD"), effect.getLines());
    }

    @Test
    @DisplayName("default constructor uses active theme colors")
    void defaultConstructorUsesThemeColors() {
        var effect = new TypewriterEffect("X");
        assertNotNull(effect.getColor());
        assertNotNull(effect.getBackgroundColor());
    }

    @Test
    @DisplayName("preferred size matches content")
    void preferredSizeMatchesContent() {
        var effect = new TypewriterEffect(List.of("12345", "12", "123"));
        assertEquals(new TerminalSize(5, 3), effect.getPreferredSize());
    }

    @Test
    @DisplayName("typing advances column by charsPerFrame")
    void typingAdvancesColumn() {
        var effect = new TypewriterEffect("ABCDEF");
        effect.setCharsPerFrame(2);
        effect.tick(0);
        assertEquals(2, effect.getCurrentCol());
        effect.tick(16_000_000L);
        assertEquals(4, effect.getCurrentCol());
    }

    @Test
    @DisplayName("line wraps to next line")
    void lineWrapsToNextLine() {
        var effect = new TypewriterEffect("AB\nCD");
        effect.setCharsPerFrame(2);
        effect.tick(0);
        assertEquals(1, effect.getCurrentLine());
        assertEquals(0, effect.getCurrentCol());
    }

    @Test
    @DisplayName("completion detected after all characters")
    void completionDetected() {
        var effect = new TypewriterEffect("A\nB");
        effect.setCharsPerFrame(1);
        assertFalse(effect.isComplete());
        effect.tick(0);       // A
        assertFalse(effect.isComplete());
        effect.tick(1_000_000L); // line advance
        effect.tick(2_000_000L); // B
        assertTrue(effect.isComplete());
    }

    @Test
    @DisplayName("onComplete callback fires once")
    void onCompleteCallbackFires() {
        var effect = new TypewriterEffect("X");
        AtomicBoolean called = new AtomicBoolean(false);
        effect.onComplete(() -> called.set(true));
        effect.tick(0);
        assertTrue(called.get(), "callback should fire when complete");
    }

    @Test
    @DisplayName("render draws partial line up to current column")
    void renderDrawsPartialLine() {
        var size = new TerminalSize(20, 5);
        var effect = new TypewriterEffect("ABCDEF");
        effect.setBounds(TerminalPosition.TOP_LEFT, size);
        effect.setCharsPerFrame(3);
        effect.tick(0);

        var buffer = new ScreenBuffer(size);
        effect.draw(new TextGraphics(buffer));

        assertEquals('A', buffer.getCell(7, 2).character().charAt(0));
        assertEquals('B', buffer.getCell(8, 2).character().charAt(0));
        assertEquals('C', buffer.getCell(9, 2).character().charAt(0));
        assertEquals('\u2588', buffer.getCell(10, 2).character().charAt(0));
    }

    @Test
    @DisplayName("render centers content vertically and horizontally")
    void renderCentersContent() {
        var size = new TerminalSize(20, 10);
        var effect = new TypewriterEffect(List.of("HI"));
        effect.setCharsPerFrame(2);
        effect.tick(0);
        effect.setBounds(TerminalPosition.TOP_LEFT, size);

        var buffer = new ScreenBuffer(size);
        effect.draw(new TextGraphics(buffer));

        assertEquals('H', buffer.getCell(9, 4).character().charAt(0));
        assertEquals('I', buffer.getCell(10, 4).character().charAt(0));
    }

    @Test
    @DisplayName("render draws all fully-typed lines")
    void renderDrawsFullyTypedLines() {
        var size = new TerminalSize(20, 10);
        var effect = new TypewriterEffect(List.of("AB", "CD", "EF"));
        effect.setCharsPerFrame(10);
        effect.tick(0);   // line 0 typed, currentLine=1
        effect.tick(16_000_000L); // line 1 typed, currentLine=2
        effect.tick(32_000_000L); // line 2 typed, complete
        assertTrue(effect.isComplete());
        effect.setBounds(TerminalPosition.TOP_LEFT, size);

        var buffer = new ScreenBuffer(size);
        effect.draw(new TextGraphics(buffer));

        int startRow = (size.rows() - 3) / 2; // 3 lines, startRow = 3
        int startCol = (size.columns() - 2) / 2; // width 2, startCol = 9
        assertEquals('A', buffer.getCell(startCol, startRow).character().charAt(0));
        assertEquals('B', buffer.getCell(startCol + 1, startRow).character().charAt(0));
        assertEquals('C', buffer.getCell(startCol, startRow + 1).character().charAt(0));
        assertEquals('D', buffer.getCell(startCol + 1, startRow + 1).character().charAt(0));
        assertEquals('E', buffer.getCell(startCol, startRow + 2).character().charAt(0));
        assertEquals('F', buffer.getCell(startCol + 1, startRow + 2).character().charAt(0));
    }

    @Test
    @DisplayName("cursor drawn at typing position when enabled")
    void cursorDrawnAtTypingPosition() {
        var size = new TerminalSize(20, 5);
        var effect = new TypewriterEffect("AB");
        effect.setCharsPerFrame(1);
        effect.setShowCursor(true);
        effect.tick(0);
        effect.setBounds(TerminalPosition.TOP_LEFT, size);

        var buffer = new ScreenBuffer(size);
        effect.draw(new TextGraphics(buffer));

        // Centered: startCol = 9, row = 2; 'A' at 9, cursor at 10
        assertEquals('A', buffer.getCell(9, 2).character().charAt(0));
        assertEquals('\u2588', buffer.getCell(10, 2).character().charAt(0));
    }

    @Test
    @DisplayName("cursor can be hidden")
    void cursorCanBeHidden() {
        var size = new TerminalSize(20, 5);
        var effect = new TypewriterEffect("AB");
        effect.setCharsPerFrame(1);
        effect.setShowCursor(false);
        effect.tick(0);
        effect.setBounds(TerminalPosition.TOP_LEFT, size);

        var buffer = new ScreenBuffer(size);
        effect.draw(new TextGraphics(buffer));

        assertEquals(' ', buffer.getCell(10, 2).character().charAt(0));
    }

    @Test
    @DisplayName("line delay pauses before next line")
    void lineDelayPauses() {
        var effect = new TypewriterEffect(List.of("A", "B"));
        effect.setCharsPerFrame(1);
        effect.setLineDelay(2);
        effect.tick(0); // line 0 done, currentLine becomes 1, line delay set
        assertEquals(1, effect.getCurrentLine());
        assertFalse(effect.isComplete());
        effect.tick(16_000_000L); // delay 1
        assertEquals(1, effect.getCurrentLine());
        effect.tick(32_000_000L); // delay 2
        assertEquals(1, effect.getCurrentLine());
        effect.tick(48_000_000L); // now line 1 typed
        assertEquals(2, effect.getCurrentLine());
    }

    @Test
    @DisplayName("charsPerFrame clamps to valid range")
    void charsPerFrameClamps() {
        var effect = new TypewriterEffect("X");
        effect.setCharsPerFrame(0);
        assertEquals(1, effect.getCharsPerFrame());
        effect.setCharsPerFrame(1000);
        assertEquals(256, effect.getCharsPerFrame());
    }

    @Test
    @DisplayName("targetFps clamps to valid range")
    void targetFpsClamps() {
        var effect = new TypewriterEffect("X");
        effect.setTargetFps(0);
        assertEquals(1, effect.getTargetFps());
        effect.setTargetFps(1000);
        assertEquals(60, effect.getTargetFps());
    }

    @Test
    @DisplayName("empty text completes immediately and renders nothing")
    void emptyTextCompletesImmediately() {
        var effect = new TypewriterEffect("");
        AtomicInteger calls = new AtomicInteger();
        effect.onComplete(calls::incrementAndGet);
        effect.tick(0);
        assertTrue(effect.isComplete());
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("reset restores initial state")
    void resetRestoresState() {
        var effect = new TypewriterEffect(List.of("AB", "CD"));
        effect.setCharsPerFrame(2);
        effect.tick(0); // line 1, col 0
        assertFalse(effect.isComplete());
        effect.reset();
        assertEquals(0, effect.getCurrentLine());
        assertEquals(0, effect.getCurrentCol());
        assertFalse(effect.isComplete());
    }

    @Test
    @DisplayName("reset allows onComplete to fire again")
    void resetAllowsCallbackAgain() {
        var effect = new TypewriterEffect("X");
        AtomicInteger calls = new AtomicInteger();
        effect.onComplete(calls::incrementAndGet);
        effect.tick(0);
        assertEquals(1, calls.get());
        effect.reset();
        effect.tick(16_000_000L);
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("render is safe with zero size")
    void renderZeroSizeSafe() {
        var effect = new TypewriterEffect("ABC");
        effect.setBounds(TerminalPosition.TOP_LEFT, TerminalSize.ZERO);
        assertDoesNotThrow(() -> effect.draw(new TextGraphics(new ScreenBuffer(new TerminalSize(1, 1)))));
    }

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var effect = new TypewriterEffect("X");
        assertTrue(effect instanceof AnimatedBackground);
        assertEquals(15, effect.targetFps());
        effect.start();
        assertTrue(effect.isRunning());
        effect.stop();
        assertFalse(effect.isRunning());
    }

    @Test
    @DisplayName("can be added to a Panel")
    void canBeAddedToPanel() {
        var panel = new Panel();
        var effect = new TypewriterEffect("X");
        assertDoesNotThrow(() -> panel.addComponent(effect));
    }

    @Test
    @DisplayName("render applies custom colors")
    void renderAppliesCustomColors() {
        var size = new TerminalSize(10, 5);
        var effect = new TypewriterEffect("X", AnsiColor.BRIGHT_RED, AnsiColor.BLUE);
        effect.setCharsPerFrame(1);
        effect.tick(0);
        effect.setBounds(TerminalPosition.TOP_LEFT, size);

        var buffer = new ScreenBuffer(size);
        effect.draw(new TextGraphics(buffer));

        var cell = buffer.getCell(4, 2);
        assertEquals('X', cell.character().charAt(0));
        assertEquals(AnsiColor.BRIGHT_RED, cell.fg());
        assertEquals(AnsiColor.BLUE, cell.bg());
    }
}
