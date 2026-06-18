package io.jterm.widget;

import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.screen.ScreenBuffer;
import io.jterm.core.TerminalSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Thread-safety tests for widgets and screen components.
 * These verify that concurrent reads/writes don't cause data corruption
 * or ConcurrentModificationExceptions.
 */
class ThreadSafetyTest {

    // ── ProgressBar: concurrent setValue + read ───────────────

    @Test
    @Timeout(5)
    void progressBarConcurrentSetAndRead() throws Exception {
        var bar = new ProgressBar(100);
        bar.setBounds(new io.jterm.core.TerminalPosition(0, 0), new TerminalSize(20, 1));
        int iterations = 1000;
        var done = new CountDownLatch(1);
        var errors = new AtomicReference<Throwable>(null);

        // Writer thread
        var writer = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i <= iterations; i++) {
                    bar.setValue(i);
                }
            } catch (Throwable t) {
                errors.set(t);
            } finally {
                done.countDown();
            }
        });

        // Reader thread: read value while writer is updating
        while (!done.await(10, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            int v = bar.getValue(); // should never throw
            assertTrue(v >= 0 && v <= 100, "Value out of range: " + v);
        }
        writer.join();
        assertNull(errors.get(), "Writer thread threw exception");
        assertEquals(100, bar.getValue());
    }

    // ── TextBox: concurrent type + getValue ────────────────────

    @Test
    @Timeout(5)
    void textBoxConcurrentWriteAndRead() throws Exception {
        var box = new TextBox(20);
        var done = new CountDownLatch(1);
        var errors = new AtomicReference<Throwable>(null);

        // Writer: type characters
        var writer = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    box.handleKeyStroke(KeyStroke.character('A', false, false, false));
                }
            } catch (Throwable t) {
                errors.set(t);
            } finally {
                done.countDown();
            }
        });

        // Reader: getValue while writer types
        while (!done.await(1, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            String val = box.getValue();
            assertNotNull(val);
            int len = val.length();
            assertTrue(len <= 100, "Value length exceeded iterations: " + len);
        }
        writer.join();
        assertNull(errors.get());
        assertEquals(100, box.getValue().length());
    }

    // ── TextArea: concurrent edit + getText ───────────────────

    @Test
    @Timeout(5)
    void textAreaConcurrentEditAndRead() throws Exception {
        var ta = new TextArea("Hello\nWorld");
        var done = new CountDownLatch(1);
        var errors = new AtomicReference<Throwable>(null);

        // Writer: type characters on line 0
        var writer = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 50; i++) {
                    ta.handleKeyStroke(KeyStroke.character('X', false, false, false));
                }
            } catch (Throwable t) {
                errors.set(t);
            } finally {
                done.countDown();
            }
        });

        // Reader: getText while writer types
        while (!done.await(1, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            String text = ta.getText();
            assertNotNull(text);
        }
        writer.join();
        assertNull(errors.get(), "Writer threw");
        // Should have 50 X's inserted into "Hello" → "XXXXX...Hello"
        assertTrue(ta.getText().contains("Hello"));
    }

    // ── ScreenBuffer: concurrent setCell + getCell ────────────

    @Test
    @Timeout(5)
    void screenBufferConcurrentSetAndGet() throws Exception {
        var buf = new ScreenBuffer(new TerminalSize(10, 10));
        var done = new CountDownLatch(1);
        var errors = new AtomicReference<Throwable>(null);
        var cell = new io.jterm.style.TextCell('X',
                io.jterm.style.AnsiColor.DEFAULT, io.jterm.style.AnsiColor.DEFAULT);

        // Writer: set cells
        var writer = Thread.ofVirtual().start(() -> {
            try {
                for (int r = 0; r < 10; r++) {
                    for (int c = 0; c < 10; c++) {
                        buf.setCell(c, r, cell);
                    }
                }
            } catch (Throwable t) {
                errors.set(t);
            } finally {
                done.countDown();
            }
        });

        // Reader: get cells while writer writes
        while (!done.await(1, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            for (int r = 0; r < 10; r++) {
                buf.getCell(0, r); // should never throw
            }
        }
        writer.join();
        assertNull(errors.get());
    }

    // ── ScreenBuffer: concurrent diffFrom ─────────────────────

    @Test
    @Timeout(5)
    void screenBufferConcurrentDiffAndWrite() throws Exception {
        var buf1 = new ScreenBuffer(new TerminalSize(5, 5));
        var buf2 = new ScreenBuffer(new TerminalSize(5, 5));
        var done = new CountDownLatch(1);
        var errors = new AtomicReference<Throwable>(null);
        var cell = new io.jterm.style.TextCell('Z',
                io.jterm.style.AnsiColor.DEFAULT, io.jterm.style.AnsiColor.DEFAULT);

        // Writer: update buf1
        var writer = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    buf1.setCell(i % 5, (i / 5) % 5, cell);
                }
            } catch (Throwable t) {
                errors.set(t);
            } finally {
                done.countDown();
            }
        });

        // Reader: diff while writer writes
        while (!done.await(1, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            buf1.diffFrom(buf2); // should not throw CME
        }
        writer.join();
        assertNull(errors.get());
    }

    // ── ListBox: concurrent addItem + getItems ─────────────────

    @Test
    @Timeout(5)
    void listBoxConcurrentAddAndRead() throws Exception {
        var list = new ListBox<String>();
        list.setBounds(new io.jterm.core.TerminalPosition(0, 0), new TerminalSize(20, 5));
        var done = new CountDownLatch(1);
        var errors = new AtomicReference<Throwable>(null);

        // Writer: add items
        var writer = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    list.addItem("Item" + i);
                }
            } catch (Throwable t) {
                errors.set(t);
            } finally {
                done.countDown();
            }
        });

        // Reader: getItems while writer adds
        while (!done.await(1, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            var items = list.getItems();
            assertNotNull(items);
            assertTrue(items.size() <= 100);
        }
        writer.join();
        assertNull(errors.get());
        assertEquals(100, list.getItems().size());
    }

    // ── Table: concurrent addRow + draw ────────────────────────

    @Test
    @Timeout(5)
    void tableConcurrentAddAndDraw() throws Exception {
        var table = new Table("A", "B");
        table.setBounds(new io.jterm.core.TerminalPosition(0, 0), new TerminalSize(20, 5));
        var done = new CountDownLatch(1);
        var errors = new AtomicReference<Throwable>(null);

        // Writer: add rows
        var writer = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 50; i++) {
                    table.addRow("R" + i + "A", "R" + i + "B");
                }
            } catch (Throwable t) {
                errors.set(t);
            } finally {
                done.countDown();
            }
        });

        // Reader: draw while writer adds (iterates rows)
        var buf = new ScreenBuffer(new TerminalSize(20, 5));
        var g = new io.jterm.graphics.TextGraphics(buf);
        while (!done.await(1, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            table.draw(g); // should not throw CME
        }
        writer.join();
        assertNull(errors.get());
    }

    // ── AbstractComponent: concurrent invalidate + getPreferredSize ──

    @Test
    @Timeout(5)
    void componentConcurrentInvalidateAndPreferredSize() throws Exception {
        var bar = new ProgressBar(100);
        bar.setBounds(new io.jterm.core.TerminalPosition(0, 0), new TerminalSize(20, 1));
        var done = new CountDownLatch(1);
        var errors = new AtomicReference<Throwable>(null);

        // Writer: setValue (calls invalidate)
        var writer = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    bar.setValue(i % 100);
                }
            } catch (Throwable t) {
                errors.set(t);
            } finally {
                done.countDown();
            }
        });

        // Reader: getPreferredSize while writer invalidates
        while (!done.await(1, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            var ps = bar.getPreferredSize();
            assertNotNull(ps);
        }
        writer.join();
        assertNull(errors.get());
    }
}