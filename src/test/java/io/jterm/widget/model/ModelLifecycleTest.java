package io.jterm.widget.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ModelLifecycleTest {

    private static final class TrackingListModel extends AbstractListModel<String> {
        private final List<String> items = new CopyOnWriteArrayList<>();

        void add(String item) {
            items.add(item);
            fireIntervalAdded(items.size() - 1, items.size() - 1);
        }

        void remove(int index) {
            items.remove(index);
            fireIntervalRemoved(index, index);
        }

        void changeAll() {
            fireContentsChanged(0, items.size() - 1);
        }

        void clear() {
            int size = items.size();
            items.clear();
            fireContentsChanged(0, Math.max(0, size - 1));
        }

        @Override
        public int getSize() {
            return items.size();
        }

        @Override
        public String getElementAt(int index) {
            return items.get(index);
        }
    }

    private static final class TrackingTableModel extends AbstractTableModel {
        private final List<String[]> rows = new CopyOnWriteArrayList<>();
        private final String[] columns;

        TrackingTableModel(String... columns) {
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

        void updateCell(int row, int col) {
            fireCellsChanged(row, row, col);
        }

        void updateAll() {
            fireRowsChanged(0, Math.max(0, rows.size() - 1));
        }

        void reset() {
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
    void listModelAddAndRemoveListener() {
        TrackingListModel model = new TrackingListModel();
        ListDataEventLog listener = new ListDataEventLog();
        model.addListDataListener(listener);
        model.add("a");
        assertEquals(1, listener.intervalAdded.size());
        model.removeListDataListener(listener);
        model.add("b");
        assertEquals(1, listener.intervalAdded.size());
    }

    @Test
    void listModelFireWithNoListenersDoesNotThrow() {
        TrackingListModel model = new TrackingListModel();
        assertDoesNotThrow(() -> {
            model.add("a");
            model.remove(0);
            model.changeAll();
            model.clear();
        });
    }

    @Test
    void listModelMultipleListenersAllReceiveEvents() {
        TrackingListModel model = new TrackingListModel();
        ListDataEventLog first = new ListDataEventLog();
        ListDataEventLog second = new ListDataEventLog();
        ListDataEventLog third = new ListDataEventLog();
        model.addListDataListener(first);
        model.addListDataListener(second);
        model.addListDataListener(third);
        model.add("x");
        assertEquals(1, first.intervalAdded.size());
        assertEquals(1, second.intervalAdded.size());
        assertEquals(1, third.intervalAdded.size());
    }

    @Test
    void listModelRemoveListenerMidDispatchDoesNotThrow() {
        TrackingListModel model = new TrackingListModel();
        ListDataListener selfRemoving = new ListDataListener() {
            @Override
            public void contentsChanged(ListDataEvent e) {
                model.removeListDataListener(this);
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                model.removeListDataListener(this);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                model.removeListDataListener(this);
            }
        };
        ListDataEventLog other = new ListDataEventLog();
        model.addListDataListener(selfRemoving);
        model.addListDataListener(other);
        assertDoesNotThrow(() -> model.add("a"));
        assertEquals(1, other.intervalAdded.size());
        assertDoesNotThrow(() -> model.add("b"));
        assertEquals(2, other.intervalAdded.size());
    }

    @Test
    void listModelListenersNotifiedInRegistrationOrder() {
        TrackingListModel model = new TrackingListModel();
        List<Integer> order = new ArrayList<>();
        model.addListDataListener(new ListDataListener() {
            @Override
            public void contentsChanged(ListDataEvent e) {
                order.add(1);
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                order.add(1);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                order.add(1);
            }
        });
        model.addListDataListener(new ListDataListener() {
            @Override
            public void contentsChanged(ListDataEvent e) {
                order.add(2);
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                order.add(2);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                order.add(2);
            }
        });
        model.add("a");
        assertEquals(List.of(1, 2), order);
    }

    @Test
    void listModelNoMemoryLeakAfterRemoval() {
        TrackingListModel model = new TrackingListModel();
        ListDataEventLog listener = new ListDataEventLog();
        for (int i = 0; i < 100; i++) {
            model.addListDataListener(listener);
            model.removeListDataListener(listener);
        }
        model.add("a");
        assertEquals(0, listener.intervalAdded.size());
    }

    @Test
    void tableModelAddAndRemoveListener() {
        TrackingTableModel model = new TrackingTableModel("A");
        TableModelEventLog listener = new TableModelEventLog();
        model.addTableModelListener(listener);
        model.addRow("a");
        assertEquals(1, listener.events.size());
        model.removeTableModelListener(listener);
        model.addRow("b");
        assertEquals(1, listener.events.size());
    }

    @Test
    void tableModelFireWithNoListenersDoesNotThrow() {
        TrackingTableModel model = new TrackingTableModel("A");
        assertDoesNotThrow(() -> {
            model.addRow("a");
            model.removeRow(0);
            model.updateAll();
            model.updateCell(0, 0);
            model.reset();
        });
    }

    @Test
    void tableModelMultipleListenersAllReceiveEvents() {
        TrackingTableModel model = new TrackingTableModel("A");
        TableModelEventLog first = new TableModelEventLog();
        TableModelEventLog second = new TableModelEventLog();
        TableModelEventLog third = new TableModelEventLog();
        model.addTableModelListener(first);
        model.addTableModelListener(second);
        model.addTableModelListener(third);
        model.addRow("x");
        assertEquals(1, first.events.size());
        assertEquals(1, second.events.size());
        assertEquals(1, third.events.size());
    }

    @Test
    void tableModelRemoveListenerMidDispatchDoesNotThrow() {
        TrackingTableModel model = new TrackingTableModel("A");
        TableModelListener selfRemoving = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                model.removeTableModelListener(this);
            }
        };
        TableModelEventLog other = new TableModelEventLog();
        model.addTableModelListener(selfRemoving);
        model.addTableModelListener(other);
        assertDoesNotThrow(() -> model.addRow("a"));
        assertEquals(1, other.events.size());
        assertDoesNotThrow(() -> model.addRow("b"));
        assertEquals(2, other.events.size());
    }

    @Test
    void tableModelListenersNotifiedInRegistrationOrder() {
        TrackingTableModel model = new TrackingTableModel("A");
        List<Integer> order = new ArrayList<>();
        model.addTableModelListener(e -> order.add(1));
        model.addTableModelListener(e -> order.add(2));
        model.addRow("a");
        assertEquals(List.of(1, 2), order);
    }
}
