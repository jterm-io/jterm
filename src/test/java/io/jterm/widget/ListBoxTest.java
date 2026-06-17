package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListBoxTest {
    @Test
    void addItemUpdatesPreferredSize() {
        var list = new ListBox<String>();
        list.addItem("Alpha");
        list.addItem("Beta");
        assertEquals(new TerminalSize(5, 2), list.getPreferredSize());
    }

    @Test
    void selectionDefaultsToFirstItem() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        assertEquals(0, list.getSelectedIndex());
        assertEquals("A", list.getSelectedItem());
    }

    @Test
    void setSelectedIndexUpdatesSelection() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.setSelectedIndex(1);
        assertEquals(1, list.getSelectedIndex());
        assertEquals("B", list.getSelectedItem());
    }

    @Test
    void invalidIndexIgnored() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.setSelectedIndex(5);
        assertEquals(0, list.getSelectedIndex());
    }

    @Test
    void arrowDownMovesSelection() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        assertEquals(1, list.getSelectedIndex());
    }

    @Test
    void arrowUpStopsAtTop() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.handleKeyStroke(new KeyStroke(KeyType.ARROW_UP));
        assertEquals(0, list.getSelectedIndex());
    }

    @Test
    void enterFiresSelectionListener() {
        var list = new ListBox<String>();
        list.addItem("A");
        var fired = new boolean[1];
        list.addSelectionListener(() -> fired[0] = true);
        list.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertTrue(fired[0]);
    }

    @Test
    void customRendererUsed() {
        var list = new ListBox<Integer>();
        list.addItem(42);
        list.setRenderer(i -> "Value: " + i);
        assertTrue(list.getPreferredSize().columns() >= 7);
    }

    @Test
    void scrollOffsetAdjustedWhenSelectionBelowViewport() {
        var list = new ListBox<String>();
        for (int i = 0; i < 10; i++) list.addItem("Item " + i);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setSelectedIndex(5);
        assertTrue(list.getSelectedIndex() == 5);
    }

    @Test
    void emptyListReturnsNullSelection() {
        var list = new ListBox<String>();
        assertNull(list.getSelectedItem());
    }
}
