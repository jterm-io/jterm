package io.jterm.widget.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class DefaultListModelStressTest {

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

    @Test
    void addOneThousandElements() {
        DefaultListModel<Integer> model = new DefaultListModel<>();
        ListDataEventLog listener = new ListDataEventLog();
        model.addListDataListener(listener);
        for (int i = 0; i < 1000; i++) {
            model.addElement(i);
        }
        assertEquals(1000, model.getSize());
        assertEquals(1000, listener.intervalAdded.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, model.getElementAt(i), "element at index " + i);
        }
    }

    @Test
    void removeFromMiddleShiftsIndices() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("a");
        model.addElement("b");
        model.addElement("c");
        model.addElement("d");
        model.removeElementAt(1);
        assertEquals(3, model.getSize());
        assertEquals("a", model.getElementAt(0));
        assertEquals("c", model.getElementAt(1));
        assertEquals("d", model.getElementAt(2));
    }

    @Test
    void clearAfterAddingEmptiesModel() {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < 100; i++) {
            model.addElement("item-" + i);
        }
        model.clear();
        assertEquals(0, model.getSize());
        assertThrows(IndexOutOfBoundsException.class, () -> model.getElementAt(0));
    }

    @Test
    void setElementAtVariousPositions() {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < 10; i++) {
            model.addElement("initial-" + i);
        }
        model.setElementAt(0, "head");
        model.setElementAt(9, "tail");
        model.setElementAt(5, "middle");
        assertEquals("head", model.getElementAt(0));
        assertEquals("middle", model.getElementAt(5));
        assertEquals("tail", model.getElementAt(9));
    }

    @Test
    void interleavedAddRemoveOperations() {
        DefaultListModel<Integer> model = new DefaultListModel<>();
        ListDataEventLog listener = new ListDataEventLog();
        model.addListDataListener(listener);
        for (int i = 0; i < 50; i++) {
            model.addElement(i);
        }
        for (int i = 0; i < 25; i++) {
            model.removeElementAt(0);
        }
        for (int i = 0; i < 25; i++) {
            model.addElementAt(i, i * 10);
        }
        assertEquals(50, model.getSize());
        for (int i = 0; i < 25; i++) {
            assertEquals(i * 10, model.getElementAt(i));
            assertEquals(i + 25, model.getElementAt(i + 25));
        }
        assertEquals(100, listener.intervalAdded.size() + listener.intervalRemoved.size());
    }

    @Test
    void concurrentAddFromVirtualThreads() throws InterruptedException {
        DefaultListModel<Integer> model = new DefaultListModel<>();
        int threads = 10;
        int iterations = 100;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int t = 0; t < threads; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < iterations; i++) {
                        model.addElement(threadId * iterations + i);
                    }
                    done.countDown();
                });
            }
            ready.await();
            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "virtual-thread adds should complete");
        }
        assertEquals(threads * iterations, model.getSize());
        List<Integer> values = IntStream.range(0, model.getSize())
                .mapToObj(model::getElementAt)
                .sorted()
                .toList();
        assertEquals(IntStream.range(0, threads * iterations).boxed().toList(), values);
    }

    @Test
    void repeatedClearAndRepopulate() {
        DefaultListModel<String> model = new DefaultListModel<>();
        ListDataEventLog listener = new ListDataEventLog();
        model.addListDataListener(listener);
        for (int cycle = 0; cycle < 20; cycle++) {
            for (int i = 0; i < 10; i++) {
                model.addElement("cycle-" + cycle + "-" + i);
            }
            model.clear();
        }
        assertEquals(0, model.getSize());
        assertEquals(20, listener.contentsChanged.size());
    }
}
