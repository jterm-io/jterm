package io.jterm.widget.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GridModelTest {

    @Test
    void addRowIncrementsRowCount() {
        DefaultGridModel<String> model = new DefaultGridModel<>();
        assertEquals(0, model.getRowCount());
        model.addRow("a");
        assertEquals(1, model.getRowCount());
        model.addRow("b");
        assertEquals(2, model.getRowCount());
    }

    @Test
    void addRowFiresGridChanged() {
        DefaultGridModel<String> model = new DefaultGridModel<>();
        AtomicInteger callCount = new AtomicInteger(0);
        model.addGridListener(callCount::incrementAndGet);
        model.addRow("x");
        assertEquals(1, callCount.get());
    }

    @Test
    void addRowsAddsAllElementsAndFiresOnce() {
        DefaultGridModel<String> model = new DefaultGridModel<>();
        AtomicInteger callCount = new AtomicInteger(0);
        model.addGridListener(callCount::incrementAndGet);
        model.addRows(List.of("a", "b", "c"));
        assertEquals(3, model.getRowCount());
        assertEquals(1, callCount.get(), "addRows should fire gridChanged exactly once");
    }

    @Test
    void setRowsReplacesExistingRowsAndFiresOnce() {
        DefaultGridModel<String> model = new DefaultGridModel<>();
        model.addRows(List.of("old1", "old2", "old3"));
        AtomicInteger callCount = new AtomicInteger(0);
        model.addGridListener(callCount::incrementAndGet);
        model.setRows(List.of("new1", "new2"));
        assertEquals(2, model.getRowCount());
        assertEquals("new1", model.getRow(0));
        assertEquals("new2", model.getRow(1));
        assertEquals(1, callCount.get(), "setRows should fire gridChanged exactly once");
    }

    @Test
    void clearEmptiesAndFires() {
        DefaultGridModel<String> model = new DefaultGridModel<>();
        model.addRows(List.of("a", "b", "c"));
        AtomicInteger callCount = new AtomicInteger(0);
        model.addGridListener(callCount::incrementAndGet);
        model.clear();
        assertEquals(0, model.getRowCount());
        assertEquals(1, callCount.get(), "clear should fire gridChanged exactly once");
    }

    @Test
    void getRowReturnsCorrectElement() {
        DefaultGridModel<Integer> model = new DefaultGridModel<>();
        model.addRows(List.of(10, 20, 30));
        assertEquals(10, model.getRow(0));
        assertEquals(20, model.getRow(1));
        assertEquals(30, model.getRow(2));
    }

    @Test
    void getRowThrowsIndexOutOfBoundsForInvalidIndex() {
        DefaultGridModel<String> model = new DefaultGridModel<>();
        model.addRow("only");
        assertThrows(IndexOutOfBoundsException.class, () -> model.getRow(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> model.getRow(1));
        assertThrows(IndexOutOfBoundsException.class, () -> model.getRow(100));
    }

    @Test
    void addAndRemoveGridListenerWorkCorrectly() {
        DefaultGridModel<String> model = new DefaultGridModel<>();
        AtomicInteger callCount = new AtomicInteger(0);
        GridListener listener = callCount::incrementAndGet;
        model.addGridListener(listener);
        model.addRow("a");
        assertEquals(1, callCount.get());

        model.removeGridListener(listener);
        model.addRow("b");
        assertEquals(1, callCount.get(), "removed listener should not be called");
    }

    @Test
    void multipleListenersAllFire() {
        DefaultGridModel<String> model = new DefaultGridModel<>();
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);
        AtomicInteger count3 = new AtomicInteger(0);
        model.addGridListener(count1::incrementAndGet);
        model.addGridListener(count2::incrementAndGet);
        model.addGridListener(count3::incrementAndGet);
        model.addRow("x");
        assertEquals(1, count1.get());
        assertEquals(1, count2.get());
        assertEquals(1, count3.get());
    }

    @Test
    void removeGridListenerNeverAddedIsNoOp() {
        DefaultGridModel<String> model = new DefaultGridModel<>();
        GridListener listener = () -> {};
        assertDoesNotThrow(() -> model.removeGridListener(listener));
        // Verify model still works
        model.addRow("x");
        assertEquals(1, model.getRowCount());
    }

    @Test
    void setRowsOnEmptyCollectionWorks() {
        DefaultGridModel<String> model = new DefaultGridModel<>();
        model.addRows(List.of("a", "b"));
        AtomicInteger callCount = new AtomicInteger(0);
        model.addGridListener(callCount::incrementAndGet);
        model.setRows(List.of());
        assertEquals(0, model.getRowCount());
        assertEquals(1, callCount.get(), "setRows with empty collection should still fire once");
    }

    @Test
    void concurrentAddAndReadIsThreadSafe() throws InterruptedException {
        DefaultGridModel<Integer> model = new DefaultGridModel<>();
        int numThreads = 8;
        int rowsPerThread = 500;
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(numThreads);

        // Half the threads add rows, half read getRowCount concurrently
        for (int t = 0; t < numThreads; t++) {
            final int tid = t;
            pool.submit(() -> {
                try {
                    start.await();
                    if (tid % 2 == 0) {
                        for (int i = 0; i < rowsPerThread; i++) {
                            model.addRow(i);
                        }
                    } else {
                        // Reader threads: repeatedly read row count
                        for (int i = 0; i < rowsPerThread; i++) {
                            model.getRowCount(); // must not throw
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "Concurrent test timed out");
        pool.shutdown();

        // 4 writer threads × 500 rows each = 2000 total
        int expectedRows = (numThreads / 2) * rowsPerThread;
        assertEquals(expectedRows, model.getRowCount(),
            "All added rows should be present after concurrent writes");
    }
}