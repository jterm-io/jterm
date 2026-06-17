package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableTest {
    @Test
    void constructorStoresHeaders() {
        var table = new Table("Name", "Age");
        var ps = table.getPreferredSize();
        assertTrue(ps.columns() >= 9);
        assertTrue(ps.rows() >= 3);
    }

    @Test
    void addRowUpdatesPreferredSize() {
        var table = new Table("A");
        table.addRow("Alpha");
        table.addRow("Beta");
        assertTrue(table.getPreferredSize().rows() >= 3);
    }

    @Test
    void setSelectedRowClampsToRange() {
        var table = new Table("A");
        table.addRow("1");
        table.addRow("2");
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        table.setSelectedRow(5);
        assertEquals(1, table.getSelectedRow());
    }

    @Test
    void arrowDownSelectsNextRow() {
        var table = new Table("A");
        table.addRow("1");
        table.addRow("2");
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        table.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        assertEquals(1, table.getSelectedRow());
    }

    @Test
    void arrowUpStopsAtTop() {
        var table = new Table("A");
        table.addRow("1");
        table.addRow("2");
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        table.setSelectedRow(1);
        table.handleKeyStroke(new KeyStroke(KeyType.ARROW_UP));
        assertEquals(0, table.getSelectedRow());
    }

    @Test
    void drawsHeaderRow() {
        var buffer = new ScreenBuffer(new TerminalSize(20, 5));
        var table = new Table("Name", "Age");
        table.addRow("Alice", "30");
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        table.draw(new TextGraphics(buffer));
        assertEquals('N', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    void emptyTableRendersHeader() {
        var buffer = new ScreenBuffer(new TerminalSize(10, 3));
        var table = new Table("A");
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        table.draw(new TextGraphics(buffer));
        assertEquals('A', buffer.getCell(0, 0).character().charAt(0));
    }
}
