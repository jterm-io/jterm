package io.jterm.widget.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventCorrectnessTest {

    private static final class ListDataEventLog implements ListDataListener {
        private final List<ListDataEvent> contentsChanged = new ArrayList<>();
        private final List<ListDataEvent> intervalAdded = new ArrayList<>();
        private final List<ListDataEvent> intervalRemoved = new ArrayList<>();

        @Override
        public void contentsChanged(ListDataEvent e) {
            contentsChanged.add(e);
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            intervalAdded.add(e);
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            intervalRemoved.add(e);
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
    void listDataEventTypesMatchOperation() {
        DefaultListModel<String> model = new DefaultListModel<>();
        ListDataEventLog listener = new ListDataEventLog();
        model.addListDataListener(listener);
        model.addElement("a");
        assertEquals(ListDataEventType.INTERVAL_ADDED, listener.intervalAdded.get(0).type());
        assertEquals(0, listener.intervalAdded.get(0).index0());
        assertEquals(0, listener.intervalAdded.get(0).index1());

        model.addElementAt(0, "b");
        assertEquals(ListDataEventType.INTERVAL_ADDED, listener.intervalAdded.get(1).type());
        assertEquals(0, listener.intervalAdded.get(1).index0());
        assertEquals(0, listener.intervalAdded.get(1).index1());

        model.setElementAt(0, "c");
        assertEquals(ListDataEventType.CONTENTS_CHANGED, listener.contentsChanged.get(0).type());
        assertEquals(0, listener.contentsChanged.get(0).index0());
        assertEquals(0, listener.contentsChanged.get(0).index1());

        model.removeElementAt(0);
        assertEquals(ListDataEventType.INTERVAL_REMOVED, listener.intervalRemoved.get(0).type());
        assertEquals(0, listener.intervalRemoved.get(0).index0());
        assertEquals(0, listener.intervalRemoved.get(0).index1());

        model.clear();
        assertEquals(ListDataEventType.CONTENTS_CHANGED, listener.contentsChanged.get(1).type());
        assertEquals(0, listener.contentsChanged.get(1).index0());
        assertEquals(0, listener.contentsChanged.get(1).index1());
    }

    @Test
    void listDataEventRangeForBatchClear() {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < 10; i++) {
            model.addElement(String.valueOf(i));
        }
        ListDataEventLog listener = new ListDataEventLog();
        model.addListDataListener(listener);
        model.clear();
        assertEquals(1, listener.contentsChanged.size());
        ListDataEvent event = listener.contentsChanged.get(0);
        assertEquals(0, event.index0());
        assertEquals(9, event.index1());
    }

    @Test
    void noStaleListDataEventsAfterListenerRemoval() {
        DefaultListModel<String> model = new DefaultListModel<>();
        ListDataEventLog listener = new ListDataEventLog();
        model.addListDataListener(listener);
        model.addElement("a");
        model.removeListDataListener(listener);
        model.addElement("b");
        model.removeElementAt(0);
        model.clear();
        assertEquals(1, listener.intervalAdded.size());
        assertTrue(listener.contentsChanged.isEmpty());
        assertTrue(listener.intervalRemoved.isEmpty());
    }

    @Test
    void tableModelEventTypesMatchOperation() {
        DefaultTableModel model = new DefaultTableModel("A");
        TableModelEventLog listener = new TableModelEventLog();
        model.addTableModelListener(listener);
        model.addRow("a");
        assertEquals(TableModelEventType.ROWS_ADDED, listener.events.get(0).type());
        assertEquals(0, listener.events.get(0).firstRow());
        assertEquals(0, listener.events.get(0).lastRow());
        assertEquals(-1, listener.events.get(0).column());

        model.setValueAt(0, 0, "b");
        assertEquals(TableModelEventType.CELLS_CHANGED, listener.events.get(1).type());
        assertEquals(0, listener.events.get(1).firstRow());
        assertEquals(0, listener.events.get(1).lastRow());
        assertEquals(0, listener.events.get(1).column());

        model.clear();
        assertEquals(TableModelEventType.ROWS_CHANGED, listener.events.get(2).type());
        assertEquals(0, listener.events.get(2).firstRow());
        assertEquals(0, listener.events.get(2).lastRow());
        assertEquals(-1, listener.events.get(2).column());

        model.addRow("c");
        model.removeRow(0);
        assertEquals(TableModelEventType.ROWS_REMOVED, listener.events.get(4).type());
        assertEquals(0, listener.events.get(4).firstRow());
        assertEquals(0, listener.events.get(4).lastRow());
    }

    @Test
    void tableModelStructureChangedEventIsWildcard() {
        DefaultTableModel model = new DefaultTableModel("A");
        TableModelEventLog listener = new TableModelEventLog();
        model.addTableModelListener(listener);
        model.fireStructureChanged();
        assertEquals(1, listener.events.size());
        TableModelEvent event = listener.events.get(0);
        assertEquals(TableModelEventType.STRUCTURE_CHANGED, event.type());
        assertEquals(-1, event.firstRow());
        assertEquals(-1, event.lastRow());
        assertEquals(-1, event.column());
    }

    @Test
    void noStaleTableModelEventsAfterListenerRemoval() {
        DefaultTableModel model = new DefaultTableModel("A");
        TableModelEventLog listener = new TableModelEventLog();
        model.addTableModelListener(listener);
        model.addRow("a");
        assertEquals(1, listener.events.size());
        model.removeTableModelListener(listener);
        model.addRow("b");
        model.setValueAt(0, 0, "c");
        model.clear();
        assertEquals(1, listener.events.size());
    }

    @Test
    void batchOperationsFireSingleEvent() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (int i = 0; i < 5; i++) {
            listModel.addElement(String.valueOf(i));
        }
        ListDataEventLog listListener = new ListDataEventLog();
        listModel.addListDataListener(listListener);
        listModel.clear();
        assertEquals(1, listListener.contentsChanged.size());

        DefaultTableModel tableModel = new DefaultTableModel("A");
        for (int i = 0; i < 5; i++) {
            tableModel.addRow(String.valueOf(i));
        }
        TableModelEventLog tableListener = new TableModelEventLog();
        tableModel.addTableModelListener(tableListener);
        tableModel.clear();
        assertEquals(1, tableListener.events.size());
    }
}
