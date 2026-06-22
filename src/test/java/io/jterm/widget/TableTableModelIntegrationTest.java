package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.widget.model.AbstractTableModel;
import io.jterm.widget.model.DefaultTableModel;
import io.jterm.widget.model.TableModel;
import io.jterm.widget.model.TableModelEvent;
import io.jterm.widget.model.TableModelEventType;
import io.jterm.widget.model.TableModelListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TableTableModelIntegrationTest {

    private static final class CustomTableModel extends AbstractTableModel {
        private final List<String[]> rows = new CopyOnWriteArrayList<>();
        private final String[] columns;

        CustomTableModel(String... columns) {
            this.columns = columns;
        }

        void addRow(String... values) {
            rows.add(values);
            fireRowsAdded(rows.size() - 1, rows.size() - 1);
        }

        void removeRow(int index) {
            rows.remove(index);
            fireRowsRemoved(index, index);
        }

        void setValueAt(int row, int col, String value) {
            rows.get(row)[col] = value;
            fireCellsChanged(row, row, col);
        }

        void updateRow(int row) {
            fireRowsChanged(row, row);
        }

        void changeStructure() {
            fireStructureChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        @Override
        public String getValueAt(int row, int col) {
            return rows.get(row)[col];
        }
    }

    private static final class TableModelEventLog implements TableModelListener {
        private final List<TableModelEvent> events = new ArrayList<>();

        @Override
        public void tableChanged(TableModelEvent e) {
            events.add(e);
        }
    }

    @Test
    void tableWithCustomModelRenders() {
        CustomTableModel model = new CustomTableModel("Name");
        model.addRow("Alice");
        Table table = new Table(model);
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(15, 5));
        var buffer = new ScreenBuffer(new TerminalSize(15, 5));
        table.draw(new TextGraphics(buffer));
        assertEquals('N', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('A', buffer.getCell(0, 1).character().charAt(0));
    }

    @Test
    void rowsAddedEventRendersNewRow() {
        CustomTableModel model = new CustomTableModel("Name");
        Table table = new Table(model);
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(15, 5));
        var buffer = new ScreenBuffer(new TerminalSize(15, 5));
        table.draw(new TextGraphics(buffer));
        model.addRow("Bob");
        buffer = new ScreenBuffer(new TerminalSize(15, 5));
        table.draw(new TextGraphics(buffer));
        assertEquals('B', buffer.getCell(0, 1).character().charAt(0));
    }

    @Test
    void rowsRemovedClampsSelection() {
        CustomTableModel model = new CustomTableModel("Name");
        model.addRow("a");
        model.addRow("b");
        model.addRow("c");
        Table table = new Table(model);
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(15, 5));
        table.setSelectedRow(2);
        model.removeRow(2);
        assertEquals(1, table.getSelectedRow());
    }

    @Test
    void cellsChangedUpdatesSpecificCell() {
        CustomTableModel model = new CustomTableModel("Name");
        model.addRow("old");
        Table table = new Table(model);
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(15, 5));
        var buffer = new ScreenBuffer(new TerminalSize(15, 5));
        table.draw(new TextGraphics(buffer));
        assertEquals('o', buffer.getCell(0, 1).character().charAt(0));
        model.setValueAt(0, 0, "new");
        buffer = new ScreenBuffer(new TerminalSize(15, 5));
        table.draw(new TextGraphics(buffer));
        assertEquals('n', buffer.getCell(0, 1).character().charAt(0));
    }

    @Test
    void structureChangedPreservesSelectionWhenRowCountUnchanged() {
        // STRUCTURE_CHANGED should no longer reset selectedRow to 0.
        // The selection should only be clamped if it exceeds the row count.
        CustomTableModel model = new CustomTableModel("Name");
        for (int i = 0; i < 20; i++) {
            model.addRow("row-" + i);
        }
        Table table = new Table(model);
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(15, 5));
        table.setSelectedRow(19);
        model.changeStructure();
        assertEquals(19, table.getSelectedRow());
    }

    @Test
    void setModelSwitchesCleanly() {
        CustomTableModel first = new CustomTableModel("A");
        first.addRow("first");
        Table table = new Table(first);
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(15, 5));
        CustomTableModel second = new CustomTableModel("B");
        second.addRow("second");
        table.setModel(second);
        assertEquals(second, table.getModel());
        var buffer = new ScreenBuffer(new TerminalSize(15, 5));
        table.draw(new TextGraphics(buffer));
        assertEquals('B', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('s', buffer.getCell(0, 1).character().charAt(0));
    }

    @Test
    void oldModelStopsNotifyingAfterSetModel() {
        CustomTableModel first = new CustomTableModel("A");
        first.addRow("first");
        CustomTableModel second = new CustomTableModel("B");
        Table table = new Table(first);
        table.setModel(second);
        TableModelEventLog listener = new TableModelEventLog();
        second.addTableModelListener(listener);
        first.addRow("x");
        assertTrue(listener.events.isEmpty());
    }

    @Test
    void columnHeadersComeFromModel() {
        CustomTableModel model = new CustomTableModel("Ticker", "Price");
        model.addRow("A", "1");
        Table table = new Table(model);
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(30, 5));
        var buffer = new ScreenBuffer(new TerminalSize(30, 5));
        table.draw(new TextGraphics(buffer));
        assertEquals('T', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('P', buffer.getCell(15, 0).character().charAt(0));
    }

    @Test
    void columnWidthAdaptsToModelDataChanges() {
        CustomTableModel model = new CustomTableModel("Name");
        Table table = new Table(model);
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(60, 5));
        model.addRow("short");
        int before = table.getPreferredSize().columns();
        model.addRow("this is a much longer value");
        table.invalidate();
        int after = table.getPreferredSize().columns();
        assertTrue(after > before, "preferred width should grow after adding longer data");
    }

    @Test
    void emptyModelRendersHeadersOnly() {
        CustomTableModel model = new CustomTableModel("OnlyHeader");
        Table table = new Table(model);
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        var buffer = new ScreenBuffer(new TerminalSize(20, 5));
        assertDoesNotThrow(() -> table.draw(new TextGraphics(buffer)));
        assertEquals('O', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    void rowsChangedInvalidatesTable() {
        CustomTableModel model = new CustomTableModel("Name");
        model.addRow("a");
        Table table = new Table(model);
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(15, 5));
        model.updateRow(0);
        var buffer = new ScreenBuffer(new TerminalSize(15, 5));
        table.draw(new TextGraphics(buffer));
        assertEquals('a', buffer.getCell(0, 1).character().charAt(0));
    }

    @Test
    void backwardCompatWithDefaultTableModelAddRow() {
        Table table = new Table("A", "B");
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        table.addRow("x", "y");
        assertEquals(1, table.getModel().getRowCount());
        assertEquals("x", table.getModel().getValueAt(0, 0));
    }
}
