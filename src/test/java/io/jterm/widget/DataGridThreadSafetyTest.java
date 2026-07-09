package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.Theme;
import io.jterm.style.ThemeManager;
import io.jterm.widget.model.DefaultGridModel;
import io.jterm.widget.model.GridColumn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M5 thread-safety tests for {@link DataGrid}.
 *
 * <p>Follows the same pattern as {@link ThreadSafetyTest#tableConcurrentAddAndDraw}:
 * spawn a writer thread that mutates the model while a reader thread repeatedly
 * draws the widget. The grid must not throw {@link java.util.ConcurrentModificationException}
 * or any other exception.
 */
class DataGridThreadSafetyTest {

    /** Simple record used as the row type. */
    record TestRow(String name, int age, double score) {}

    private Theme savedTheme;

    @BeforeEach
    void setUp() {
        savedTheme = ThemeManager.active();
        ThemeManager.setActive(Theme.DARK);
    }

    @AfterEach
    void tearDown() {
        ThemeManager.setActive(savedTheme);
    }

    private DataGrid<TestRow> newGrid(int width, int height, DefaultGridModel<TestRow> model) {
        var columns = List.of(
                GridColumn.text("Name", TestRow::name),
                GridColumn.intCol("Age", TestRow::age),
                GridColumn.doubleCol("Score", "%.2f", TestRow::score)
        );
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(width, height));
        return grid;
    }

    private void drawGrid(DataGrid<?> grid, int width, int height) {
        var buf = new ScreenBuffer(new TerminalSize(width, height));
        grid.draw(new TextGraphics(buf));
    }

    // ---- 1. Concurrent add + draw -----------------------------------------

    @Test
    @Timeout(10)
    void concurrentAddAndDraw() throws Exception {
        var model = new DefaultGridModel<TestRow>();
        var grid = newGrid(30, 6, model);
        var done = new CountDownLatch(1);
        var errors = new AtomicReference<Throwable>(null);

        // Writer: add rows
        var writer = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    model.addRow(new TestRow("R" + i, i, i * 1.5));
                }
            } catch (Throwable t) {
                errors.set(t);
            } finally {
                done.countDown();
            }
        });

        // Reader: draw while writer adds
        while (!done.await(1, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            drawGrid(grid, 30, 6);
        }
        writer.join();
        assertNull(errors.get(), "Writer thread threw: " + errors.get());
        assertEquals(100, model.getRowCount());
    }

    // ---- 2. Concurrent setRows + draw -------------------------------------

    @Test
    @Timeout(10)
    void concurrentSetRowsAndDraw() throws Exception {
        var model = new DefaultGridModel<TestRow>();
        var grid = newGrid(30, 6, model);
        var done = new CountDownLatch(2);
        var errors = new AtomicReference<Throwable>(null);

        // Two writer threads calling setRows repeatedly
        Runnable writerTask = () -> {
            try {
                for (int i = 0; i < 50; i++) {
                    final int iter = i;
                    var rows = java.util.stream.IntStream.range(0, 10)
                            .mapToObj(j -> new TestRow("T" + iter + "_" + j, j, j * 2.0))
                            .toList();
                    model.setRows(rows);
                }
            } catch (Throwable t) {
                errors.set(t);
            } finally {
                done.countDown();
            }
        };

        var w1 = Thread.ofVirtual().start(writerTask);
        var w2 = Thread.ofVirtual().start(writerTask);

        // Reader: draw while writers replace rows
        while (done.getCount() > 0) {
            drawGrid(grid, 30, 6);
        }
        w1.join();
        w2.join();
        assertNull(errors.get(), "Writer thread threw: " + errors.get());
        assertEquals(10, model.getRowCount(), "setRows replaces; final count should be 10");
    }

    // ---- 3. Concurrent selection + draw -----------------------------------

    @Test
    @Timeout(10)
    void concurrentSelectionAndDraw() throws Exception {
        var model = new DefaultGridModel<TestRow>();
        for (int i = 0; i < 20; i++) {
            model.addRow(new TestRow("R" + i, i, i * 1.5));
        }
        var grid = newGrid(30, 6, model);
        var done = new CountDownLatch(1);
        var errors = new AtomicReference<Throwable>(null);

        // Writer: move selection up and down
        var writer = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 200; i++) {
                    grid.setSelectedRow(i % 20);
                }
            } catch (Throwable t) {
                errors.set(t);
            } finally {
                done.countDown();
            }
        });

        // Reader: draw while selection moves
        while (!done.await(1, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            drawGrid(grid, 30, 6);
        }
        writer.join();
        assertNull(errors.get(), "Selection thread threw: " + errors.get());
    }

    // ---- 4. Concurrent model swap + draw ----------------------------------

    @Test
    @Timeout(10)
    void concurrentModelSwapAndDraw() throws Exception {
        var model = new DefaultGridModel<TestRow>();
        for (int i = 0; i < 10; i++) {
            model.addRow(new TestRow("R" + i, i, i * 1.5));
        }
        var grid = newGrid(30, 6, model);
        var done = new CountDownLatch(1);
        var errors = new AtomicReference<Throwable>(null);

        // Writer: repeatedly swap models
        var writer = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 50; i++) {
                    var newModel = new DefaultGridModel<TestRow>();
                    for (int j = 0; j < 5; j++) {
                        newModel.addRow(new TestRow("S" + i + "_" + j, j, j * 3.0));
                    }
                    grid.setModel(newModel);
                }
            } catch (Throwable t) {
                errors.set(t);
            } finally {
                done.countDown();
            }
        });

        // Reader: draw while models are swapped
        while (!done.await(1, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            drawGrid(grid, 30, 6);
        }
        writer.join();
        assertNull(errors.get(), "Model-swap thread threw: " + errors.get());
    }
}