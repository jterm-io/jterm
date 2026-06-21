package io.jterm.widget.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbstractListModelTest {

    private static final class TestListModel extends AbstractListModel<String> {
        private final List<String> items = new ArrayList<>();

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

        void changeRange(int from, int to) {
            fireContentsChanged(from, to);
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

    private static final class CapturingListener implements ListDataListener {
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

    @Test
    void addListenerReceivesIntervalAdded() {
        TestListModel model = new TestListModel();
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.add("a");
        assertEquals(1, listener.intervalAdded.size());
        assertEquals(ListDataEventType.INTERVAL_ADDED, listener.intervalAdded.get(0).type());
        assertEquals(0, listener.intervalAdded.get(0).index0());
        assertEquals(0, listener.intervalAdded.get(0).index1());
    }

    @Test
    void removeListenerReceivesIntervalRemoved() {
        TestListModel model = new TestListModel();
        model.add("a");
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.remove(0);
        assertEquals(1, listener.intervalRemoved.size());
        assertEquals(0, listener.intervalRemoved.get(0).index0());
        assertEquals(0, listener.intervalRemoved.get(0).index1());
    }

    @Test
    void contentsChangedDispatchesCorrectly() {
        TestListModel model = new TestListModel();
        model.add("a");
        model.add("b");
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.changeAll();
        assertEquals(1, listener.contentsChanged.size());
        assertEquals(0, listener.contentsChanged.get(0).index0());
        assertEquals(1, listener.contentsChanged.get(0).index1());
    }

    @Test
    void multipleListenersAllReceiveEvents() {
        TestListModel model = new TestListModel();
        CapturingListener first = new CapturingListener();
        CapturingListener second = new CapturingListener();
        model.addListDataListener(first);
        model.addListDataListener(second);
        model.add("x");
        assertEquals(1, first.intervalAdded.size());
        assertEquals(1, second.intervalAdded.size());
    }

    @Test
    void removedListenerReceivesNoEvents() {
        TestListModel model = new TestListModel();
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.removeListDataListener(listener);
        model.add("x");
        assertTrue(listener.intervalAdded.isEmpty());
        assertTrue(listener.contentsChanged.isEmpty());
        assertTrue(listener.intervalRemoved.isEmpty());
    }

    @Test
    void rangeContentsChangedCarriesBounds() {
        TestListModel model = new TestListModel();
        model.add("a");
        model.add("b");
        model.add("c");
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.changeRange(1, 2);
        assertEquals(1, listener.contentsChanged.size());
        assertEquals(1, listener.contentsChanged.get(0).index0());
        assertEquals(2, listener.contentsChanged.get(0).index1());
    }

    @Test
    void listenerCanRemoveItselfDuringEvent() {
        TestListModel model = new TestListModel();
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
        model.addListDataListener(selfRemoving);
        model.add("x");
        // should not throw
        model.add("y");
        assertEquals(2, model.getSize());
    }

    @Test
    void emptyContentsChangedRangeIsValid() {
        TestListModel model = new TestListModel();
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.changeRange(0, 0);
        assertEquals(1, listener.contentsChanged.size());
        assertEquals(0, listener.contentsChanged.get(0).index0());
        assertEquals(0, listener.contentsChanged.get(0).index1());
    }
}
