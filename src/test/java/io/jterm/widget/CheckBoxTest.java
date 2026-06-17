package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CheckBoxTest {
    @Test
    void initialStateUnselected() {
        var cb = new CheckBox("Option");
        assertFalse(cb.isSelected());
    }

    @Test
    void setSelectedUpdatesState() {
        var cb = new CheckBox("Option");
        cb.setSelected(true);
        assertTrue(cb.isSelected());
    }

    @Test
    void toggleFlipsState() {
        var cb = new CheckBox("Option");
        cb.toggle();
        assertTrue(cb.isSelected());
        cb.toggle();
        assertFalse(cb.isSelected());
    }

    @Test
    void toggleFiresListener() {
        var cb = new CheckBox("Option");
        var fired = new boolean[1];
        cb.addListener(() -> fired[0] = !fired[0]);
        cb.toggle();
        assertTrue(fired[0]);
        cb.toggle();
        assertFalse(fired[0]);
    }

    @Test
    void spaceKeyToggles() {
        var cb = new CheckBox("Option");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        cb.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        assertTrue(cb.isSelected());
    }

    @Test
    void enterKeyToggles() {
        var cb = new CheckBox("Option");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        cb.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertTrue(cb.isSelected());
    }

    @Test
    void preferredSizeIncludesLabel() {
        var cb = new CheckBox("Yes");
        assertEquals(new TerminalSize(7, 1), cb.getPreferredSize());
    }
}
