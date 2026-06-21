package io.jterm.widget.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableModelTest {

    private static final class SimpleTableModel extends AbstractTableModel {
        private final java.util.List<String[]> rows = new java.util.ArrayList<>();
        private final String[] columns;

        SimpleTableModel(String... columns) {
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

    @Test
    void emptyTableHasZeroRows() {
        TableModel model = new SimpleTableModel("A", "B");
        assertEquals(0, model.getRowCount());
        assertEquals(2, model.getColumnCount());
    }

    @Test
    void columnNamesMatchConstructor() {
        TableModel model = new SimpleTableModel("Name", "Value");
        assertEquals("Name", model.getColumnName(0));
        assertEquals("Value", model.getColumnName(1));
    }

    @Test
    void addRowIncreasesRowCount() {
        SimpleTableModel model = new SimpleTableModel("A", "B");
        model.addRow("1", "2");
        assertEquals(1, model.getRowCount());
        assertEquals("1", model.getValueAt(0, 0));
        assertEquals("2", model.getValueAt(0, 1));
    }

    @Test
    void removeRowDecreasesRowCount() {
        SimpleTableModel model = new SimpleTableModel("A");
        model.addRow("x");
        model.removeRow(0);
        assertEquals(0, model.getRowCount());
    }

    @Test
    void setValueAtChangesValue() {
        SimpleTableModel model = new SimpleTableModel("A");
        model.addRow("old");
        model.setValueAt(0, 0, "new");
        assertEquals("new", model.getValueAt(0, 0));
    }

    @Test
    void getValueAtOutOfBoundsThrows() {
        SimpleTableModel model = new SimpleTableModel("A");
        model.addRow("x");
        assertThrows(IndexOutOfBoundsException.class, () -> model.getValueAt(1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> model.getValueAt(0, 1));
    }

    @Test
    void getColumnNameOutOfBoundsThrows() {
        TableModel model = new SimpleTableModel("A");
        assertThrows(IndexOutOfBoundsException.class, () -> model.getColumnName(1));
    }
}
