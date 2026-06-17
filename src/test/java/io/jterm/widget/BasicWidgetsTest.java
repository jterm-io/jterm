package io.jterm.widget;

import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BasicWidgetsTest {
    @Test
    void checkBoxToggles() {
        var cb = new CheckBox("Enable");
        cb.setBounds(new io.jterm.core.TerminalPosition(0, 0), new io.jterm.core.TerminalSize(20, 1));
        cb.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        assertTrue(cb.isSelected());
        cb.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        assertFalse(cb.isSelected());
    }

    @Test
    void progressBarCalculatesFill() {
        var bar = new ProgressBar(100);
        bar.setValue(50);
        assertEquals(50, bar.getValue());
    }

    @Test
    void listBoxSelects() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(new io.jterm.core.TerminalPosition(0, 0), new io.jterm.core.TerminalSize(10, 5));
        list.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        assertEquals(1, list.getSelectedIndex());
    }

    @Test
    void tableSelectsRow() {
        var table = new Table("Col1", "Col2");
        table.addRow("A", "1");
        table.addRow("B", "2");
        table.setBounds(new io.jterm.core.TerminalPosition(0, 0), new io.jterm.core.TerminalSize(30, 5));
        table.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        assertEquals(1, table.getSelectedRow());
    }
}
