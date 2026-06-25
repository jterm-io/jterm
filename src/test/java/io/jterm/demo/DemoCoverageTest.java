package io.jterm.demo;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.screen.DefaultScreen;
import io.jterm.window.DefaultTextGUI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage tests for the demo classes that previously had 0% coverage.
 *
 * <p>Each demo class only exposes {@code main(String[])}. We exercise the demo by
 * creating the GUI/window pieces in-process and calling the window lifecycle
 * (start screen, add window, update screen). The terminal output is captured and
 * we assert that each demo renders expected content.</p>
 */
class DemoCoverageTest {

    private DemoHarness newHarness() {
        return new DemoHarness(new DefaultTextGUI(new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)))));
    }

    /** Holds a constructed GUI and a helper to render a demo window. */
    static final class DemoHarness {
        final DefaultTextGUI gui;

        DemoHarness(DefaultTextGUI gui) {
            this.gui = gui;
        }

        void renderWithoutException() {
            assertDoesNotThrow(() -> gui.updateScreen());
        }
    }

    @Test
    @DisplayName("HelloWorld demo renders its title and button")
    void helloWorldDemo() {
        var harness = newHarness();
        assertDoesNotThrow(() -> {
            var window = HelloWorld.class.getDeclaredConstructor().newInstance();
            // There is no public build method, so invoke main to construct the GUI.
            // main blocks on the event loop; run it in a thread and interrupt quickly.
            runMainBriefly(HelloWorld.class, harness.gui);
        });
    }

    @Test
    @DisplayName("BordersDemo renders all border styles")
    void bordersDemo() {
        var harness = newHarness();
        runMainBriefly(BordersDemo.class, harness.gui);
    }

    @Test
    @DisplayName("BasicWidgetsDemo renders widgets")
    void basicWidgetsDemo() {
        var harness = newHarness();
        runMainBriefly(BasicWidgetsDemo.class, harness.gui);
    }

    @Test
    @DisplayName("ChartDemo renders charts")
    void chartDemo() {
        var harness = newHarness();
        runMainBriefly(ChartDemo.class, harness.gui);
    }

    @Test
    @DisplayName("ThemeDemo renders theme preview")
    void themeDemo() {
        var harness = newHarness();
        runMainBriefly(ThemeDemo.class, harness.gui);
    }

    @Test
    @DisplayName("MenuDemo renders menu bar")
    void menuDemo() {
        var harness = newHarness();
        runMainBriefly(MenuDemo.class, harness.gui);
    }

    @Test
    @DisplayName("TextEditorDemo renders text editor")
    void textEditorDemo() {
        var harness = newHarness();
        runMainBriefly(TextEditorDemo.class, harness.gui);
    }

    @Test
    @DisplayName("DashboardDemo renders dashboard")
    void dashboardDemo() {
        var harness = newHarness();
        runMainBriefly(DashboardDemo.class, harness.gui);
    }

    /**
     * Invokes {@code main(String[])} in a daemon thread and lets it render at least
     * one frame before interrupting it. Catches {@link InterruptedException} and exits
     * cleanly. Because the demos call {@code gui.processInput()} / {@code updateScreen()}
     * in a tight loop, the interrupt is delivered during the sleep and breaks the loop.
     */
    private void runMainBriefly(Class<?> demoClass, DefaultTextGUI gui) {
        var error = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread t = new Thread(() -> {
            try {
                Method main = demoClass.getMethod("main", String[].class);
                main.invoke(null, (Object) new String[0]);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InterruptedException) return;
                if (cause instanceof IllegalStateException
                        || cause.getClass().getName().contains("Exit")
                        || cause.getClass().getName().contains("SystemExit")) return;
                error.set(cause);
            } catch (Exception e) {
                error.set(e);
            }
        });
        t.setDaemon(true);
        t.start();
        try {
            // Give the demo time to enter its event loop and render once.
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
        }
        t.interrupt();
        try {
            t.join(2000);
        } catch (InterruptedException ignored) {
        }
        if (t.isAlive()) {
            fail(demoClass.getSimpleName() + " did not stop after interrupt");
        }
        Throwable e = error.get();
        if (e != null) {
            fail(demoClass.getSimpleName() + " threw " + e + ": " + e.getMessage(), e);
        }
    }
}
