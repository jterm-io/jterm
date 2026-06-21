package io.jterm.widget.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbstractTableModelTest {

    private static final class TestTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return 0;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int col) {
            return "Col";
        }

        @Override
        public String getValueAt(int row, int col) {
            return "";
        }
    }

    private static final class CapturingListener implements TableModelListener {
        private final List<TableModelEvent> events = new ArrayList<>();

        @Override
        public void tableChanged(TableModelEvent e) {
            events.add(e);
        }
    }

    @Test
    void addListenerReceivesRowsAdded() {
        TestTableModel model = new TestTableModel();
        CapturingListener listener = new CapturingListener();
        model.addTableModelListener(listener);
        model.fireRowsAdded(0, 0);
        assertEquals(1, listener.events.size());
        assertEquals(TableModelEventType.ROWS_ADDED, listener.events.get(0).type());
        assertEquals(0, listener.events.get(0).firstRow());
        assertEquals(0, listener.events.get(0).lastRow());
        assertEquals(-1, listener.events.get(0).column());
    }

    @Test
    void rowsRemovedDispatchedCorrectly() {
        TestTableModel model = new TestTableModel();
        CapturingListener listener = new CapturingListener();
        model.addTableModelListener(listener);
        model.fireRowsRemoved(2, 4);
        assertEquals(1, listener.events.size());
        assertEquals(TableModelEventType.ROWS_REMOVED, listener.events.get(0).type());
        assertEquals(2, listener.events.get(0).firstRow());
        assertEquals(4, listener.events.get(0).lastRow());
    }

    @Test
    void rowsChangedDispatchedCorrectly() {
        TestTableModel model = new TestTableModel();
        CapturingListener listener = new CapturingListener();
        model.addTableModelListener(listener);
        model.fireRowsChanged(1, 1);
        assertEquals(1, listener.events.size());
        assertEquals(TableModelEventType.ROWS_CHANGED, listener.events.get(0).type());
        assertEquals(1, listener.events.get(0).firstRow());
        assertEquals(1, listener.events.get(0).lastRow());
    }

    @Test
    void cellsChangedCarriesColumn() {
        TestTableModel model = new TestTableModel();
        CapturingListener listener = new CapturingListener();
        model.addTableModelListener(listener);
        model.fireCellsChanged(0, 2, 3);
        assertEquals(1, listener.events.size());
        assertEquals(TableModelEventType.CELLS_CHANGED, listener.events.get(0).type());
        assertEquals(0, listener.events.get(0).firstRow());
        assertEquals(2, listener.events.get(0).lastRow());
        assertEquals(3, listener.events.get(0).column());
    }

    @Test
    void structureChangedUsesWildcards() {
        TestTableModel model = new TestTableModel();
        CapturingListener listener = new CapturingListener();
        model.addTableModelListener(listener);
        model.fireStructureChanged();
        assertEquals(1, listener.events.size());
        assertEquals(TableModelEventType.STRUCTURE_CHANGED, listener.events.get(0).type());
        assertEquals(-1, listener.events.get(0).firstRow());
        assertEquals(-1, listener.events.get(0).lastRow());
        assertEquals(-1, listener.events.get(0).column());
    }

    @Test
    void multipleListenersAllReceiveEvents() {
        TestTableModel model = new TestTableModel();
        CapturingListener first = new CapturingListener();
        CapturingListener second = new CapturingListener();
        model.addTableModelListener(first);
        model.addTableModelListener(second);
        model.fireStructureChanged();
        assertEquals(1, first.events.size());
        assertEquals(1, second.events.size());
    }

    @Test
    void removedListenerReceivesNoEvents() {
        TestTableModel model = new TestTableModel();
        CapturingListener listener = new CapturingListener();
        model.addTableModelListener(listener);
        model.removeTableModelListener(listener);
        model.fireRowsAdded(0, 0);
        assertTrue(listener.events.isEmpty());
    }

    @Test
    void listenerCanRemoveItselfDuringEvent() {
        TestTableModel model = new TestTableModel();
        TableModelListener[] selfRemoving = new TableModelListener[1];
        selfRemoving[0] = e -> model.removeTableModelListener(selfRemoving[0]);
        model.addTableModelListener(selfRemoving[0]);
        model.fireRowsAdded(0, 0);
        // should not throw on next fire
        model.fireRowsAdded(0, 0);
    }
}
