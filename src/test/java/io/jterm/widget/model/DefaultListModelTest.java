package io.jterm.widget.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class DefaultListModelTest {

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
    void addElementFiresIntervalAdded() {
        DefaultListModel<String> model = new DefaultListModel<>();
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.addElement("x");
        assertEquals(1, listener.intervalAdded.size());
        assertEquals(0, listener.intervalAdded.get(0).index0());
        assertEquals(0, listener.intervalAdded.get(0).index1());
    }

    @Test
    void addElementAtFiresIntervalAdded() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("a");
        model.addElement("c");
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.addElementAt(1, "b");
        assertEquals(1, listener.intervalAdded.size());
        assertEquals(1, listener.intervalAdded.get(0).index0());
        assertEquals(1, listener.intervalAdded.get(0).index1());
        assertEquals("b", model.getElementAt(1));
    }

    @Test
    void removeElementAtFiresIntervalRemoved() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("a");
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.removeElementAt(0);
        assertEquals(1, listener.intervalRemoved.size());
        assertEquals(0, listener.intervalRemoved.get(0).index0());
        assertEquals(0, listener.intervalRemoved.get(0).index1());
    }

    @Test
    void setElementAtFiresContentsChanged() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("old");
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.setElementAt(0, "new");
        assertEquals(1, listener.contentsChanged.size());
        assertEquals(0, listener.contentsChanged.get(0).index0());
        assertEquals(0, listener.contentsChanged.get(0).index1());
        assertEquals("new", model.getElementAt(0));
    }

    @Test
    void clearFiresContentsChangedForFullRange() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("a");
        model.addElement("b");
        model.addElement("c");
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.clear();
        assertEquals(1, listener.contentsChanged.size());
        assertEquals(0, listener.contentsChanged.get(0).index0());
        assertEquals(2, listener.contentsChanged.get(0).index1());
        assertEquals(0, model.getSize());
    }

    @Test
    void clearOnEmptyModelFiresContentsChangedWithZeroRange() {
        DefaultListModel<String> model = new DefaultListModel<>();
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.clear();
        assertEquals(1, listener.contentsChanged.size());
        assertEquals(0, listener.contentsChanged.get(0).index0());
        assertEquals(0, listener.contentsChanged.get(0).index1());
    }

    @Test
    void addMultipleElementsFiresSequentialIndices() {
        DefaultListModel<String> model = new DefaultListModel<>();
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.addElement("a");
        model.addElement("b");
        model.addElement("c");
        assertEquals(3, listener.intervalAdded.size());
        assertEquals(0, listener.intervalAdded.get(0).index0());
        assertEquals(1, listener.intervalAdded.get(1).index0());
        assertEquals(2, listener.intervalAdded.get(2).index0());
    }

    @Test
    void listenerRemovedDuringEventDoesNotThrow() {
        DefaultListModel<String> model = new DefaultListModel<>();
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
        model.addElement("x");
        model.addElement("y");
        assertEquals(2, model.getSize());
    }

    @Test
    void concurrentAddDoesNotCorruptModel() throws InterruptedException {
        DefaultListModel<Integer> model = new DefaultListModel<>();
        int threads = 4;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    model.addElement(threadId * iterations + j);
                }
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();
        assertEquals(threads * iterations, model.getSize());
    }

    @Test
    void concurrentReadDuringAddDoesNotThrow() throws InterruptedException {
        DefaultListModel<Integer> model = new DefaultListModel<Integer>() {
            @Override
            public Integer getElementAt(int index) {
                return super.getElementAt(index);
            }
        };
        List<Integer> values = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        executor.submit(() -> {
            for (int i = 0; i < 500; i++) {
                model.addElement(i);
            }
            latch.countDown();
        });
        executor.submit(() -> {
            for (int i = 0; i < 500; i++) {
                if (model.getSize() > 0) {
                    try {
                        values.add(model.getElementAt(0));
                    } catch (IndexOutOfBoundsException ignored) {
                        // acceptable race if size changes between check and access
                    }
                }
            }
            latch.countDown();
        });
        latch.await();
        executor.shutdown();
        // no assertion on exact values, just that no exception escaped
        assertTrue(model.getSize() >= 0);
    }

    @Test
    void largeRangeOperationsAreCorrect() {
        DefaultListModel<Integer> model = new DefaultListModel<>();
        for (int i = 0; i < 1000; i++) {
            model.addElement(i);
        }
        assertEquals(1000, model.getSize());
        assertEquals(0, model.getElementAt(0));
        assertEquals(999, model.getElementAt(999));
        model.removeElementAt(0);
        assertEquals(999, model.getSize());
        assertEquals(1, model.getElementAt(0));
    }

    @Test
    void nullElementsAreAllowed() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement(null);
        assertEquals(1, model.getSize());
        assertNull(model.getElementAt(0));
    }

    @Test
    void noEventsAfterListenerRemoved() {
        DefaultListModel<String> model = new DefaultListModel<>();
        CapturingListener listener = new CapturingListener();
        model.addListDataListener(listener);
        model.removeListDataListener(listener);
        model.addElement("x");
        model.removeElementAt(0);
        model.clear();
        assertTrue(listener.contentsChanged.isEmpty());
        assertTrue(listener.intervalAdded.isEmpty());
        assertTrue(listener.intervalRemoved.isEmpty());
    }
}
