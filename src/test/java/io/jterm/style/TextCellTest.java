package io.jterm.style;

import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import static org.junit.jupiter.api.Assertions.*;

class TextCellTest {
    @Test
    void emptyCellIsSpace() {
        assertEquals(" ", TextCell.EMPTY.character());
        assertEquals(AnsiColor.DEFAULT, TextCell.EMPTY.fg());
    }

    @Test
    void withModifierReturnsNewInstance() {
        TextCell cell = new TextCell('A');
        TextCell bold = cell.withModifier(SGR.BOLD);
        assertFalse(cell.modifiers().contains(SGR.BOLD));
        assertTrue(bold.modifiers().contains(SGR.BOLD));
    }

    @Test
    void withoutModifierReturnsNewInstance() {
        TextCell cell = new TextCell('A', AnsiColor.RED, AnsiColor.DEFAULT, SGR.BOLD);
        TextCell plain = cell.withoutModifier(SGR.BOLD);
        assertTrue(cell.modifiers().contains(SGR.BOLD));
        assertFalse(plain.modifiers().contains(SGR.BOLD));
    }

    @Test
    void isCharCheck() {
        assertTrue(new TextCell('X').is('X'));
        assertFalse(new TextCell('X').is('Y'));
    }

    @Test
    void rejectsNullChar() {
        assertThrows(IllegalArgumentException.class, () -> new TextCell(null, AnsiColor.DEFAULT, AnsiColor.DEFAULT, EnumSet.noneOf(SGR.class)));
    }

    @Test
    void cjkIsDoubleWidth() {
        TextCell cjk = new TextCell('漢');
        assertTrue(cjk.isDoubleWidth());
    }

    @Test
    void asciiIsSingleWidth() {
        TextCell ascii = new TextCell('A');
        assertFalse(ascii.isDoubleWidth());
    }

    @Test
    void modifiersAreDefensiveCopied() {
        var mods = EnumSet.of(SGR.BOLD);
        TextCell cell = new TextCell("A", AnsiColor.DEFAULT, AnsiColor.DEFAULT, mods);
        mods.add(SGR.ITALIC);
        assertFalse(cell.modifiers().contains(SGR.ITALIC));
    }
}
