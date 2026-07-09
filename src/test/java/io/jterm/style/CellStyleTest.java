package io.jterm.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CellStyleTest {

    @Test
    void defaultConstantHasNullColors() {
        assertNull(CellStyle.DEFAULT.fg());
        assertNull(CellStyle.DEFAULT.bg());
        assertEquals(0, CellStyle.DEFAULT.modifiers().length);
    }

    @Test
    void greenConstantHasBrightGreenFg() {
        assertSame(AnsiColor.BRIGHT_GREEN, CellStyle.GREEN.fg());
        assertNull(CellStyle.GREEN.bg());
        assertEquals(0, CellStyle.GREEN.modifiers().length);
    }

    @Test
    void redConstantHasBrightRedFg() {
        assertSame(AnsiColor.BRIGHT_RED, CellStyle.RED.fg());
        assertNull(CellStyle.RED.bg());
        assertEquals(0, CellStyle.RED.modifiers().length);
    }

    @Test
    void boldConstantHasBoldModifier() {
        assertNull(CellStyle.BOLD.fg());
        assertNull(CellStyle.BOLD.bg());
        assertEquals(1, CellStyle.BOLD.modifiers().length);
        assertSame(SGR.BOLD, CellStyle.BOLD.modifiers()[0]);
    }

    @Test
    void customInstanceWithFgAndBg() {
        CellStyle style = new CellStyle(AnsiColor.BLUE, AnsiColor.YELLOW);
        assertSame(AnsiColor.BLUE, style.fg());
        assertSame(AnsiColor.YELLOW, style.bg());
        assertEquals(0, style.modifiers().length);
    }

    @Test
    void customInstanceWithModifiers() {
        CellStyle style = new CellStyle(AnsiColor.CYAN, AnsiColor.DEFAULT, SGR.BOLD, SGR.UNDERLINE);
        assertSame(AnsiColor.CYAN, style.fg());
        assertEquals(2, style.modifiers().length);
        assertSame(SGR.BOLD, style.modifiers()[0]);
        assertSame(SGR.UNDERLINE, style.modifiers()[1]);
    }

    @Test
    void modifiersArrayIsDefensivelyCopied() {
        SGR[] mods = {SGR.BOLD, SGR.ITALIC};
        CellStyle style = new CellStyle(AnsiColor.RED, null, mods);
        // Mutate the original array — the CellStyle copy must be unaffected
        mods[0] = SGR.UNDERLINE;
        assertSame(SGR.BOLD, style.modifiers()[0]);
        assertSame(SGR.ITALIC, style.modifiers()[1]);
    }

    @Test
    void modifiersArrayCannotBeMutatedViaAccessor() {
        CellStyle style = new CellStyle(AnsiColor.RED, null, SGR.BOLD, SGR.ITALIC);
        SGR[] returned = style.modifiers();
        returned[0] = SGR.UNDERLINE;
        // The internal array should not be affected
        assertSame(SGR.BOLD, style.modifiers()[0]);
    }

    @Test
    void nullFgMeansUseThemeDefault() {
        CellStyle style = new CellStyle(null, AnsiColor.BLUE);
        assertNull(style.fg());
        assertSame(AnsiColor.BLUE, style.bg());
    }

    @Test
    void nullBgMeansUseThemeDefault() {
        CellStyle style = new CellStyle(AnsiColor.GREEN, null);
        assertSame(AnsiColor.GREEN, style.fg());
        assertNull(style.bg());
    }
}