package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BasicWidgetsTest {
    @Test
    void checkBoxToggles() {
        var cb = new CheckBox("Enable");
        cb.setBounds(new TerminalPosition(0, 0), new TerminalSize(20, 1));
        cb.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        assertTrue(cb.isSelected());
        cb.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        assertFalse(cb.isSelected());
    }

    @Test
    void checkBoxFiresListener() {
        var cb = new CheckBox("Enable");
        var count = new int[1];
        cb.addListener(() -> count[0]++);
        cb.toggle();
        cb.toggle();
        assertEquals(2, count[0]);
    }

    @Test
    void radioGroupEnforcesSingleSelection() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        group.add(a);
        group.add(b);
        assertTrue(a.isSelected());
        assertFalse(b.isSelected());
        b.select();
        assertFalse(a.isSelected());
        assertTrue(b.isSelected());
        assertSame(b, group.getSelected());
    }

    @Test
    void radioGroupFiresSelectionListener() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        var count = new int[1];
        group.addSelectionListener(() -> count[0]++);
        group.add(a);
        group.add(b);
        assertTrue(count[0] >= 1, "adding first button should fire listener");
        b.select();
        assertEquals(2, count[0]);
    }

    @Test
    void separatorHorizontalPreferredSizeGrows() {
        var sep = new Separator(false);
        sep.setBounds(new TerminalPosition(0, 0), new TerminalSize(20, 1));
        assertEquals(new TerminalSize(1, 1), sep.getPreferredSize());
    }

    @Test
    void separatorVerticalPreferredSize() {
        var sep = new Separator(true);
        assertEquals(new TerminalSize(1, 1), sep.getPreferredSize());
    }

    @Test
    void progressBarCalculatesFill() {
        var bar = new ProgressBar(100);
        bar.setValue(50);
        assertEquals(50, bar.getValue());
    }

    @Test
    void progressBarClampsValue() {
        var bar = new ProgressBar(100);
        bar.setValue(150);
        assertEquals(100, bar.getValue());
        bar.setValue(-10);
        assertEquals(0, bar.getValue());
    }

    @Test
    void listBoxSelects() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(new TerminalPosition(0, 0), new TerminalSize(10, 5));
        list.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        assertEquals(1, list.getSelectedIndex());
    }

    @Test
    void listBoxSelectionListenerFiresOnEnter() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(new TerminalPosition(0, 0), new TerminalSize(10, 5));
        var count = new int[1];
        list.addSelectionListener(() -> count[0]++);
        assertEquals(0, count[0]);
        list.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertEquals(1, count[0]);
    }

    @Test
    void tableSelectsRow() {
        var table = new Table("Col1", "Col2");
        table.addRow("A", "1");
        table.addRow("B", "2");
        table.setBounds(new TerminalPosition(0, 0), new TerminalSize(30, 5));
        table.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        assertEquals(1, table.getSelectedRow());
    }
}
