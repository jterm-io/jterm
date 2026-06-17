package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProgressBarTest {
    @Test
    void valueClampedToMax() {
        var bar = new ProgressBar(100);
        bar.setValue(150);
        assertEquals(100, bar.getValue());
    }

    @Test
    void valueCannotBeNegative() {
        var bar = new ProgressBar(100);
        bar.setValue(-10);
        assertEquals(0, bar.getValue());
    }

    @Test
    void maxIsStored() {
        var bar = new ProgressBar(50);
        assertEquals(50, bar.getMax());
    }

    @Test
    void defaultMaxOneHundred() {
        var bar = new ProgressBar();
        assertEquals(100, bar.getMax());
    }

    @Test
    void rendersFilledPortion() {
        var buffer = new ScreenBuffer(new TerminalSize(20, 1));
        var bar = new ProgressBar(100);
        bar.setValue(50);
        bar.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        bar.draw(new TextGraphics(buffer));
        assertEquals('[', buffer.getCell(0, 0).character().charAt(0));
        assertTrue(buffer.getCell(1, 0).character().charAt(0) == '█');
    }

    @Test
    void zeroValueNoFill() {
        var buffer = new ScreenBuffer(new TerminalSize(20, 1));
        var bar = new ProgressBar(100);
        bar.setValue(0);
        bar.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        bar.draw(new TextGraphics(buffer));
        assertEquals('[', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    void preferredSizeIncludesPercentage() {
        var bar = new ProgressBar();
        assertEquals(new TerminalSize(20, 1), bar.getPreferredSize());
    }
}
