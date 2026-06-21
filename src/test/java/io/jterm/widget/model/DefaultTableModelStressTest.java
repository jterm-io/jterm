package io.jterm.widget.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTableModelStressTest {

    private static final class TableModelEventLog implements TableModelListener {
        private final List<TableModelEvent> events = new ArrayList<>();

        @Override
        public void tableChanged(TableModelEvent e) {
            events.add(e);
        }
    }

    @Test
    void addFiveHundredRowsByFiveColumns() {
        DefaultTableModel model = new DefaultTableModel("A", "B", "C", "D", "E");
        TableModelEventLog listener = new TableModelEventLog();
        model.addTableModelListener(listener);
        for (int r = 0; r < 500; r++) {
            model.addRow(String.valueOf(r), "b" + r, "c" + r, "d" + r, "e" + r);
        }
        assertEquals(500, model.getRowCount());
        assertEquals(5, model.getColumnCount());
        assertEquals(500, listener.events.size());
        for (int r = 0; r < 500; r++) {
            assertEquals(String.valueOf(r), model.getValueAt(r, 0));
            assertEquals("b" + r, model.getValueAt(r, 1));
            assertEquals("c" + r, model.getValueAt(r, 2));
            assertEquals("d" + r, model.getValueAt(r, 3));
            assertEquals("e" + r, model.getValueAt(r, 4));
        }
    }

    @Test
    void removeRowsFromHeadMiddleTail() {
        DefaultTableModel model = new DefaultTableModel("A");
        for (int i = 0; i < 10; i++) {
            model.addRow(String.valueOf(i));
        }
        model.removeRow(0); // 1,2,3,4,5,6,7,8,9
        model.removeRow(4); // 1,2,3,4,6,7,8,9 (removes 5)
        model.removeRow(6); // 1,2,3,4,6,7,9 (removes 8)
        assertEquals(7, model.getRowCount());
        assertEquals("1", model.getValueAt(0, 0));
        assertEquals("2", model.getValueAt(1, 0));
        assertEquals("6", model.getValueAt(4, 0));
        assertEquals("9", model.getValueAt(6, 0));
    }

    @Test
    void updateIndividualCellFiresOnlyCellsChanged() {
        DefaultTableModel model = new DefaultTableModel("A", "B");
        model.addRow("a", "b");
        model.addRow("c", "d");
        TableModelEventLog listener = new TableModelEventLog();
        model.addTableModelListener(listener);
        model.setValueAt(1, 0, "x");
        assertEquals(1, listener.events.size());
        TableModelEvent event = listener.events.get(0);
        assertEquals(TableModelEventType.CELLS_CHANGED, event.type());
        assertEquals(1, event.firstRow());
        assertEquals(1, event.lastRow());
        assertEquals(0, event.column());
        assertEquals("x", model.getValueAt(1, 0));
    }

    @Test
    void clearAndRepopulate() {
        DefaultTableModel model = new DefaultTableModel("A");
        TableModelEventLog listener = new TableModelEventLog();
        model.addTableModelListener(listener);
        for (int i = 0; i < 50; i++) {
            model.addRow(String.valueOf(i));
        }
        model.clear();
        for (int i = 0; i < 30; i++) {
            model.addRow(String.valueOf(100 + i));
        }
        assertEquals(30, model.getRowCount());
        for (int i = 0; i < 30; i++) {
            assertEquals(String.valueOf(100 + i), model.getValueAt(i, 0));
        }
        assertEquals(1, listener.events.stream().filter(e -> e.type() == TableModelEventType.ROWS_CHANGED).count());
    }

    @Test
    void columnCountMismatchHandling() {
        DefaultTableModel model = new DefaultTableModel("A", "B", "C");
        model.addRow("a", "b");
        assertEquals(3, model.getColumnCount());
        assertEquals("", model.getValueAt(0, 2));
        model.addRow("x", "y", "z");
        assertEquals("z", model.getValueAt(1, 2));
    }

    @Test
    void concurrentReadWrite() throws InterruptedException {
        DefaultTableModel model = new DefaultTableModel("A");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < 200; i++) {
                    model.addRow("writer-" + i);
                }
                done.countDown();
            });
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < 500; i++) {
                    if (model.getRowCount() > 0) {
                        try {
                            model.getValueAt(0, 0);
                        } catch (IndexOutOfBoundsException ignored) {
                            // acceptable race if row count changes between check and access
                        }
                    }
                }
                done.countDown();
            });
            ready.await();
            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS));
        }
        assertEquals(200, model.getRowCount());
    }
}
