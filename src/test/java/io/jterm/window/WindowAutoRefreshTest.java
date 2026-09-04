package io.jterm.window;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.screen.DefaultScreen;
import io.jterm.widget.Panel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Window-level auto-refresh (Sep 2026): windows that display time-derived
 * data (idle timers, clocks, uptime) must be able to declare a refresh
 * interval and have the GUI repaint them periodically — otherwise the data
 * goes stale until the next keystroke or event. Verified against the real
 * {@code SessionLogScreen} behavior: idle times only updated on
 * connect/disconnect or re-open.
 *
 * <p>The contract:</p>
 * <ul>
 *   <li>{@code Window.autoRefreshIntervalMillis()} defaults to 0 (off).</li>
 *   <li>{@code DefaultTextGUI.runEventLoop()} repaits windows whose interval
 *       has elapsed even with zero input, by setting needsRefresh.</li>
 *   <li>Zero-input spins stay cheap: the check is a nanoTime compare.</li>
 * </ul>
 */
class WindowAutoRefreshTest {

    private DefaultScreen screen;
    private DefaultTextGUI gui;
    private TestLoop loop;

    /** Minimal window that counts draw() calls. */
    private static final class CountingWindow extends AbstractWindow {
        final AtomicInteger draws = new AtomicInteger();
        volatile long intervalMillis;

        CountingWindow() { super("counting"); }

        @Override
        public long autoRefreshIntervalMillis() { return intervalMillis; }

        @Override
        public void draw(io.jterm.graphics.TextGraphics graphics) {
            draws.incrementAndGet();
            super.draw(graphics);
        }
    }

    /** Runs the production event loop; stopped via gui.stopRunning(). */
    private static final class TestLoop extends Thread {
        final DefaultTextGUI gui;
        TestLoop(DefaultTextGUI gui) { this.gui = gui; }
        @Override
        public void run() {
            try {
                gui.runEventLoop();
            } catch (IOException e) {
                // terminal closed — expected on shutdown
            }
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        screen.startScreen();
        gui = new DefaultTextGUI(screen);
    }

    @AfterEach
    void tearDown() throws IOException, InterruptedException {
        gui.stopRunning();
        if (loop != null) loop.join(500);
        gui.close();
    }

    @Test
    void defaultIntervalIsZero() {
        var w = new AbstractWindow("plain") {};
        assertEquals(0L, w.autoRefreshIntervalMillis(),
                "windows without a declared interval must not auto-refresh");
    }

    @Test
    void zeroIntervalWindowIsNeverRedrawnWithoutInput() throws Exception {
        var w = new CountingWindow();
        w.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        gui.addWindow(w);
        gui.updateScreen(); // initial paint
        int afterInitial = w.draws.get();
        assertTrue(afterInitial > 0, "initial paint happened");

        Thread.sleep(250);
        gui.processInput();
        gui.updateScreen();
        gui.processInput();
        gui.updateScreen();

        assertEquals(afterInitial, w.draws.get(),
                "no new draws without input and without an interval");
    }

    @Test
    void intervalElapsedWindowIsRedrawnWithoutInput() throws Exception {
        // Drive the REAL loop (runEventLoop = processInput + checkAutoRefresh
        // + updateScreen), not hand-rolled calls — production dispatches
        // through this loop, so the test must too.
        var w = new CountingWindow();
        w.intervalMillis = 50; // fast for the test
        w.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        gui.addWindow(w);
        gui.updateScreen(); // initial paint
        int afterInitial = w.draws.get();

        loop = new TestLoop(gui);
        loop.start();
        long deadline = System.nanoTime() + 3_000_000_000L;
        while (w.draws.get() <= afterInitial && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        loop.join(1000);

        assertTrue(w.draws.get() > afterInitial,
                "a window with an interval must be repainted while idle");
    }

    @Test
    void runEventLoopRepaintsPeriodicallyWithoutInput() throws Exception {
        var w = new CountingWindow();
        w.intervalMillis = 40;
        w.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        gui.addWindow(w);
        gui.updateScreen();
        int afterInitial = w.draws.get();

        loop = new TestLoop(gui);
        loop.start();
        Thread.sleep(300); // ~7 intervals with zero user input
        gui.stopRunning();
        loop.join(1000);

        assertTrue(w.draws.get() >= afterInitial + 2,
                "runEventLoop must repaint interval windows while idle, draws="
                + w.draws.get() + " initial=" + afterInitial);
    }

    @Test
    void intervalWindowStillRedrawsImmediatelyOnKey() throws Exception {
        var w = new CountingWindow();
        w.intervalMillis = 60_000; // effectively never auto-refreshes
        w.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        gui.addWindow(w);
        gui.updateScreen();
        int before = w.draws.get();

        gui.processInput(new KeyStroke(KeyType.CHARACTER, 'x', false, false, false));
        gui.updateScreen();

        assertEquals(before + 1, w.draws.get(),
                "key-driven repaints are unaffected by auto-refresh");
    }
}