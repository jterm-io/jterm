package io.jterm.screen;

import io.jterm.style.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SgrStateTrackerTest {
    @Test
    void noChangeProducesNoOutput() {
        var tracker = new DefaultScreen.SgrStateTracker();
        tracker.transitionTo(TextCell.EMPTY);
        byte[] output = tracker.transitionTo(TextCell.EMPTY);
        assertEquals(0, output.length);
    }

    @Test
    void foregroundChangeEmitsSequence() {
        var tracker = new DefaultScreen.SgrStateTracker();
        tracker.transitionTo(TextCell.EMPTY);
        var red = TextCell.EMPTY.withForeground(AnsiColor.RED);
        byte[] output = tracker.transitionTo(red);
        String s = new String(output);
        assertTrue(s.contains("31m"));
    }

    @Test
    void addingBoldEmitsEnable() {
        var tracker = new DefaultScreen.SgrStateTracker();
        var base = new TextCell('A');
        tracker.transitionTo(base);
        var bold = base.withModifier(SGR.BOLD);
        byte[] output = tracker.transitionTo(bold);
        assertTrue(new String(output).contains("\033[1m"));
    }

    @Test
    void removingBoldEmitsDisable() {
        var tracker = new DefaultScreen.SgrStateTracker();
        var bold = new TextCell('A', AnsiColor.DEFAULT, AnsiColor.DEFAULT, SGR.BOLD);
        tracker.transitionTo(bold);
        var plain = bold.withoutModifier(SGR.BOLD);
        byte[] output = tracker.transitionTo(plain);
        assertTrue(new String(output).contains("\033[22m"));
    }

    @Test
    void resetClearsState() {
        var tracker = new DefaultScreen.SgrStateTracker();
        tracker.transitionTo(new TextCell('X', AnsiColor.RED, AnsiColor.BLUE, SGR.BOLD));
        tracker.reset();
        byte[] output = tracker.transitionTo(TextCell.EMPTY);
        assertEquals(0, output.length);
    }
}
