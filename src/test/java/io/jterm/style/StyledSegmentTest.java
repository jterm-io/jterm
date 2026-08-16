package io.jterm.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StyledSegment} — a record representing a styled range of text.
 */
class StyledSegmentTest {

    // ── Construction ───────────────────────────────────────────

    @Test
    void fullArgsConstructorStoresAllFields() {
        var seg = new StyledSegment(0, 5, AnsiColor.RED, AnsiColor.BLACK, SGR.BOLD);
        assertEquals(0, seg.start());
        assertEquals(5, seg.end());
        assertEquals(AnsiColor.RED, seg.fg());
        assertEquals(AnsiColor.BLACK, seg.bg());
        assertTrue(seg.modifiers().contains(SGR.BOLD));
    }

    @Test
    void convenienceConstructorDefaultsBgAndMods() {
        var seg = new StyledSegment(2, 8, AnsiColor.GREEN);
        assertEquals(2, seg.start());
        assertEquals(8, seg.end());
        assertEquals(AnsiColor.GREEN, seg.fg());
        // default bg should be AnsiColor.DEFAULT
        assertEquals(AnsiColor.DEFAULT, seg.bg());
        assertTrue(seg.modifiers().isEmpty());
    }

    // ── Validation ─────────────────────────────────────────────

    @Test
    void negativeStartThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new StyledSegment(-1, 3, AnsiColor.RED));
    }

    @Test
    void endNotGreaterThanStartThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new StyledSegment(5, 5, AnsiColor.RED));
    }

    @Test
    void endLessThanStartThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new StyledSegment(5, 3, AnsiColor.RED));
    }

    // ── contains() ─────────────────────────────────────────────

    @Test
    void containsReturnsTrueForIndexWithinRange() {
        var seg = new StyledSegment(2, 6, AnsiColor.RED);
        assertTrue(seg.contains(2));  // start inclusive
        assertTrue(seg.contains(3));
        assertTrue(seg.contains(5));  // one before end
    }

    @Test
    void containsReturnsFalseForIndexBeforeStart() {
        var seg = new StyledSegment(2, 6, AnsiColor.RED);
        assertFalse(seg.contains(1));
        assertFalse(seg.contains(0));
    }

    @Test
    void containsReturnsFalseForIndexAtOrAfterEnd() {
        var seg = new StyledSegment(2, 6, AnsiColor.RED);
        assertFalse(seg.contains(6));  // end exclusive
        assertFalse(seg.contains(7));
    }

    // ── applyTo() ──────────────────────────────────────────────

    @Test
    void applyToReturnsCellWithSegmentColors() {
        var seg = new StyledSegment(0, 3, AnsiColor.RED, AnsiColor.BLUE, SGR.UNDERLINE);
        var cell = seg.applyTo('x', AnsiColor.BLACK);
        assertEquals('x', cell.character().charAt(0));
        assertEquals(AnsiColor.RED, cell.fg());
        assertEquals(AnsiColor.BLUE, cell.bg());
        assertTrue(cell.modifiers().contains(SGR.UNDERLINE));
    }

    @Test
    void applyToWithDefaultBgUsesProvidedDefaultBg() {
        // convenience constructor uses AnsiColor.DEFAULT as bg
        var seg = new StyledSegment(0, 3, AnsiColor.GREEN);
        var cell = seg.applyTo('y', AnsiColor.BLACK);
        assertEquals('y', cell.character().charAt(0));
        assertEquals(AnsiColor.GREEN, cell.fg());
        // When segment bg is DEFAULT, applyTo should use the provided defaultBg
        assertEquals(AnsiColor.BLACK, cell.bg());
    }

    @Test
    void applyToWithExplicitBgKeepsExplicitBg() {
        var seg = new StyledSegment(0, 3, AnsiColor.GREEN, AnsiColor.YELLOW);
        var cell = seg.applyTo('z', AnsiColor.BLACK);
        assertEquals(AnsiColor.YELLOW, cell.bg());
    }
}