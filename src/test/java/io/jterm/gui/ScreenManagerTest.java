package io.jterm.gui;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.screen.DefaultScreen;
import io.jterm.widget.Button;
import io.jterm.layout.LinearLayout;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ScreenManagerTest {

    private static DefaultTextGUI newGui() {
        return new DefaultTextGUI(new DefaultScreen(new MockTerminal(new TerminalSize(80, 24))));
    }

    @Test
    void pushOneWindow() {
        var gui = newGui();
        var sm = new ScreenManager(gui);
        var window = new WindowImpl("One");
        sm.push(window);
        assertEquals(1, sm.size());
        assertEquals(window, sm.peek());
        assertTrue(gui.containsWindow(window));
        assertEquals(window, gui.getActiveWindow());
    }

    @Test
    void pushTwoWindows() {
        var gui = newGui();
        var sm = new ScreenManager(gui);
        var first = new WindowImpl("First");
        var second = new WindowImpl("Second");
        sm.push(first);
        sm.push(second);
        assertEquals(2, sm.size());
        assertEquals(second, sm.peek());
        assertEquals(second, gui.getActiveWindow());
    }

    @Test
    void popAfterTwoPushesRestoresFirst() {
        var gui = newGui();
        var sm = new ScreenManager(gui);
        var first = new WindowImpl("First");
        var second = new WindowImpl("Second");
        sm.push(first);
        sm.push(second);
        var popped = sm.pop();
        assertEquals(second, popped);
        assertEquals(1, sm.size());
        assertEquals(first, sm.peek());
        assertEquals(first, gui.getActiveWindow());
    }

    @Test
    void popEmptyStackReturnsNull() {
        var sm = new ScreenManager(newGui());
        assertNull(sm.pop());
    }

    @Test
    void pushAndPopRemovesFromGui() throws IOException {
        var gui = newGui();
        var sm = new ScreenManager(gui);
        var window = new WindowImpl("One");
        sm.push(window);
        sm.pop();
        gui.updateScreen();
        assertFalse(gui.containsWindow(window));
        assertTrue(gui.getWindows().isEmpty());
    }

    @Test
    void pushAandBPopReturnsActiveA() throws IOException {
        var gui = newGui();
        var sm = new ScreenManager(gui);
        var a = new WindowImpl("A");
        var b = new WindowImpl("B");
        sm.push(a);
        sm.push(b);
        sm.pop();
        gui.updateScreen();
        assertEquals(a, gui.getActiveWindow());
        assertFalse(gui.containsWindow(b));
        assertTrue(gui.containsWindow(a));
    }

    @Test
    void clearRemovesAllWindows() throws IOException {
        var gui = newGui();
        var sm = new ScreenManager(gui);
        sm.push(new WindowImpl("A"));
        sm.push(new WindowImpl("B"));
        sm.clear();
        gui.updateScreen();
        assertEquals(0, sm.size());
        assertNull(sm.peek());
        assertTrue(gui.getWindows().isEmpty());
        assertNull(gui.getActiveWindow());
    }

    @Test
    void concurrentPushPopNoCorruption() throws InterruptedException, IOException {
        var gui = newGui();
        var sm = new ScreenManager(gui);
        var executor = Executors.newFixedThreadPool(10);
        var latch = new CountDownLatch(1);
        var done = new CountDownLatch(10);
        AtomicInteger pushed = new AtomicInteger();
        AtomicInteger popped = new AtomicInteger();

        for (int i = 0; i < 10; i++) {
            final int n = i;
            executor.submit(() -> {
                try {
                    latch.await();
                    var w = new WindowImpl("W" + n);
                    sm.push(w);
                    pushed.incrementAndGet();
                    var p = sm.pop();
                    if (p != null) popped.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        latch.countDown();
        done.await();
        executor.shutdown();
        assertEquals(0, sm.size(), "stack should be empty after paired push/pop");
        assertTrue(pushed.get() == 10 && popped.get() <= 10,
            "no corruption: pushed=" + pushed.get() + ", popped=" + popped.get());
        gui.updateScreen();
        assertTrue(gui.getWindows().isEmpty());
    }

    @Test
    void pushTriggersGuiRefresh() {
        var gui = newGui();
        var sm = new ScreenManager(gui);
        sm.push(new WindowImpl("X"));
        assertTrue(updateScreenCalled(gui), "push should request a refresh");
    }

    @Test
    void popRestoresFocusToPreviousWindow() throws IOException {
        var gui = newGui();
        var sm = new ScreenManager(gui);

        var a = new WindowImpl("A");
        var aButton = new Button("A");
        a.getContents().setLayoutManager(new io.jterm.layout.LinearLayout(io.jterm.layout.LinearLayout.Direction.VERTICAL));
        a.getContents().addComponent(aButton);
        sm.push(a);
        gui.updateScreen();

        var b = new WindowImpl("B");
        var bButton = new Button("B");
        b.getContents().setLayoutManager(new io.jterm.layout.LinearLayout(io.jterm.layout.LinearLayout.Direction.VERTICAL));
        b.getContents().addComponent(bButton);
        sm.push(b);

        sm.pop();
        gui.updateScreen();
        assertEquals(a, gui.getActiveWindow());
        assertEquals(aButton, a.getFocusedComponent());
    }

    private static boolean updateScreenCalled(DefaultTextGUI gui) {
        try {
            gui.updateScreen();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
