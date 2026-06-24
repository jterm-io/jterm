package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextBoxExtraTest {
    @Test
    void maskedValueDrawsAsterisks() {
        var box = new TextBox();
        box.setValue("secret");
        box.setMasked(true);
        var buf = new ScreenBuffer(new TerminalSize(20, 1));
        var g = new TextGraphics(buf);
        box.setBounds(new io.jterm.core.TerminalPosition(0, 0), new TerminalSize(20, 1));
        box.draw(g);
        String line = buf.getCell(0, 0).character();
        assertTrue(line.startsWith("*"));
    }

    @Test
    void forceUppercaseConvertsSetValue() {
        var box = new TextBox();
        box.setForceUppercase(true);
        box.setValue("abc");
        assertEquals("ABC", box.getValue());
    }

    @Test
    void backgroundColorOverrideStored() {
        var box = new TextBox();
        assertNull(box.getBackgroundColorOverride());
        box.setBackgroundColorOverride(AnsiColor.BLUE);
        assertEquals(AnsiColor.BLUE, box.getBackgroundColorOverride());
    }

    @Test
    void forceUppercaseOnExistingValue() {
        var box = new TextBox();
        box.setValue("abc");
        box.setForceUppercase(true);
        assertEquals("ABC", box.getValue());
    }

    @Test
    void nullBackgroundColorOverrideAllowed() {
        var box = new TextBox();
        box.setBackgroundColorOverride(AnsiColor.BLUE);
        box.setBackgroundColorOverride(null);
        assertNull(box.getBackgroundColorOverride());
    }
}
