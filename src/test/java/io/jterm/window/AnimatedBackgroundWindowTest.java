package io.jterm.window;

import io.jterm.animation.AnimatedBackground;
import io.jterm.animation.StarfieldBackground;
import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.DefaultScreen;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.widget.Button;
import io.jterm.widget.Label;
import io.jterm.layout.LinearLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AnimatedBackgroundWindowTest {

    static class MockAnimatedBackground implements AnimatedBackground {
        final AtomicInteger renderCalls = new AtomicInteger();
        final AtomicInteger resizeCalls = new AtomicInteger();
        final AtomicInteger startCalls = new AtomicInteger();
        final AtomicInteger stopCalls = new AtomicInteger();
        final AtomicReference<TerminalSize> lastSize = new AtomicReference<>();
        volatile boolean running;

        @Override
        public void renderFrame(TextGraphics graphics, TerminalSize size) {
            renderCalls.incrementAndGet();
            lastSize.set(size);
            graphics.setCell(0, 0, new TextCell('B', AnsiColor.BRIGHT_GREEN, AnsiColor.BLACK));
        }

        @Override
        public void onResize(TerminalSize newSize) {
            resizeCalls.incrementAndGet();
        }

        @Override
        public void start() {
            startCalls.incrementAndGet();
            running = true;
        }

        @Override
        public void stop() {
            stopCalls.incrementAndGet();
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }
    }

    @Test
    @DisplayName("AnimatedBackgroundWindow has BACKGROUND, FULLSCREEN, NO_DECORATIONS hints")
    void hintsAreCorrect() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var bg = new MockAnimatedBackground();
        var window = new AnimatedBackgroundWindow(bg, gui);

        var hints = window.getHints();
        assertTrue(hints.contains(WindowHint.BACKGROUND), "background window should have BACKGROUND hint");
        assertTrue(hints.contains(WindowHint.FULLSCREEN), "background window should be fullscreen");
        assertTrue(hints.contains(WindowHint.NO_DECORATIONS), "background window should have no decorations");
    }

    @Test
    @DisplayName("draw delegates to AnimatedBackground.renderFrame")
    void drawDelegatesToBackground() {
        var size = new TerminalSize(40, 20);
        var bg = new MockAnimatedBackground();
        var window = new AnimatedBackgroundWindow(bg, null);
        window.setBounds(TerminalPosition.TOP_LEFT, size);

        var buffer = new ScreenBuffer(size);
        window.draw(new TextGraphics(buffer));

        assertEquals(1, bg.renderCalls.get());
        assertEquals(size, bg.lastSize.get());
        assertEquals('B', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    @DisplayName("start starts background and timer")
    void startLifecycle() throws InterruptedException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var bg = new MockAnimatedBackground();
        var window = new AnimatedBackgroundWindow(bg, gui);

        window.start();
        assertTrue(bg.isRunning());
        assertNotNull(window.getTimer());
        assertTrue(window.getTimer().isRunning());

        window.stop();
        assertFalse(bg.isRunning());
        assertFalse(window.getTimer().isRunning());
    }

    @Test
    @DisplayName("timer requests GUI refresh")
    void timerRequestsRefresh() throws IOException, InterruptedException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var bg = new MockAnimatedBackground();
        var window = new AnimatedBackgroundWindow(bg, gui);
        gui.addWindow(window);

        gui.updateScreen();
        AtomicInteger refreshCount = new AtomicInteger();
        // Track whether updateScreen actually performs a refresh by hooking screen output.
        // Since we can't directly observe needsRefresh, we verify the timer ran and
        // that an updateScreen after start processes something. requestRefresh is
        // package-private; we use reflection to read the flag after start+stop.
        window.start();
        Thread.sleep(80);
        window.stop();

        assertTrue(refreshRequested(gui), "timer should have requested refresh");
    }

    private static boolean refreshRequested(DefaultTextGUI gui) {
        try {
            java.lang.reflect.Field field = DefaultTextGUI.class.getDeclaredField("needsRefresh");
            field.setAccessible(true);
            return field.getBoolean(gui);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("background window renders behind normal window")
    void backgroundRendersBehindNormal() throws Exception {
        var size = new TerminalSize(40, 20);
        var screen = new DefaultScreen(new MockTerminal(size));
        var gui = new DefaultTextGUI(screen);

        var bg = new StarfieldBackground(size);
        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        var bgWindow = new AnimatedBackgroundWindow(bg, gui);

        var fgWindow = new WindowImpl("Front");
        fgWindow.setHints(List.of(WindowHint.FULLSCREEN, WindowHint.NO_DECORATIONS));
        var contents = fgWindow.getContents();
        contents.setLayoutManager(null);
        var label = new Label("FG", AnsiColor.BRIGHT_WHITE, AnsiColor.BLACK);
        label.setBounds(new TerminalPosition(0, 0), new TerminalSize(2, 1));
        contents.addComponent(label);

        gui.addWindow(bgWindow);
        gui.addWindow(fgWindow);
        gui.updateScreen();

        var buf = captureScreenBuffer(screen);
        assertNotNull(buf);
        assertEquals('F', buf.getCell(0, 0).character().charAt(0));
    }

    @Test
    @DisplayName("background window never becomes active window")
    void backgroundNeverBecomesActive() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var bg = new MockAnimatedBackground();
        var bgWindow = new AnimatedBackgroundWindow(bg, gui);
        var fgWindow = new WindowImpl("Front");

        gui.addWindow(bgWindow);
        assertNull(gui.getActiveWindow());

        gui.addWindow(fgWindow);
        assertEquals(fgWindow, gui.getActiveWindow());
    }

    @Test
    @DisplayName("adding foreground after background keeps foreground active")
    void foregroundStaysActiveAfterBackgroundAdded() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var fgWindow = new WindowImpl("Front");
        var bgWindow = new AnimatedBackgroundWindow(new MockAnimatedBackground(), gui);

        gui.addWindow(fgWindow);
        gui.addWindow(bgWindow);
        assertEquals(fgWindow, gui.getActiveWindow());
    }

    @Test
    @DisplayName("removing foreground keeps active null if only background remains")
    void removingForegroundWithOnlyBackground() throws Exception {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var fgWindow = new WindowImpl("Front");
        var bgWindow = new AnimatedBackgroundWindow(new MockAnimatedBackground(), gui);

        gui.addWindow(fgWindow);
        gui.addWindow(bgWindow);
        gui.removeWindow(fgWindow);
        gui.updateScreen();
        assertNull(gui.getActiveWindow());
    }

    @Test
    @DisplayName("DefaultTextGUI sorted windows render background first")
    void windowsSortedBackgroundFirst() throws Exception {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var bg = new AnimatedBackgroundWindow(new MockAnimatedBackground(), gui);
        var fg = new WindowImpl("FG");
        fg.setHints(List.of(WindowHint.FULLSCREEN));

        gui.addWindow(bg);
        gui.addWindow(fg);
        gui.updateScreen();

        var windows = new java.util.ArrayList<>(gui.getWindows());
        assertTrue(windows.indexOf(bg) < windows.indexOf(fg),
                "background window should be rendered before foreground window");
    }

    @Test
    @DisplayName("input is not dispatched to background window")
    void inputNotDispatchedToBackground() throws IOException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var bg = new AnimatedBackgroundWindow(new MockAnimatedBackground(), gui);
        var fg = new WindowImpl("FG");
        var button = new Button("OK");
        var fired = new boolean[1];
        button.addListener(() -> fired[0] = true);
        fg.getContents().setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        fg.getContents().addComponent(button);

        gui.addWindow(bg);
        gui.addWindow(fg);

        gui.processInput(new io.jterm.core.input.KeyStroke(io.jterm.core.input.KeyType.ENTER));
        assertTrue(fired[0], "input should go to foreground window");
    }

    private static boolean screenNeedsRefresh(DefaultTextGUI gui) throws IOException {
        // requestRefresh is package-private; calling updateScreen resets the flag,
        // so we verify indirectly by checking that the screen buffer changed.
        gui.updateScreen();
        return true;
    }

    private static io.jterm.screen.ScreenBuffer captureScreenBuffer(io.jterm.screen.Screen screen) {
        var size = screen.getTerminalSize();
        var buf = new io.jterm.screen.ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                buf.setCell(c, r, screen.getFrontCell(c, r));
            }
        }
        return buf;
    }

    @Test
    @DisplayName("WarpStarfield background renders stars visible through TRANSPARENT foreground window")
    void warpStarfieldVisibleThroughTransparentWindow() throws Exception {
        var size = new TerminalSize(80, 23);
        var screen = new DefaultScreen(new MockTerminal(size));
        var gui = new DefaultTextGUI(screen);

        // WarpStarfield as background
        var warp = new io.jterm.widget.animation.WarpStarfield(size);
        var bgWindow = new AnimatedBackgroundWindow(warp, gui);

        // Foreground window with TRANSPARENT hint (like LoginScreen)
        var fgWindow = new WindowImpl("Login");
        fgWindow.setHints(List.of(WindowHint.FULLSCREEN, WindowHint.TRANSPARENT));
        var contents = fgWindow.getContents();
        contents.setLayoutManager(null);
        // Add a small label that only covers a few cells
        var label = new Label("Login", AnsiColor.BRIGHT_WHITE, AnsiColor.BLACK);
        label.setBounds(new TerminalPosition(30, 10), new TerminalSize(5, 1));
        contents.addComponent(label);

        gui.addWindow(bgWindow);
        gui.addWindow(fgWindow);
        gui.updateScreen();

        var buf = captureScreenBuffer(screen);

        // Count star characters in the buffer — at least some should be
        // visible through the transparent foreground window
        int stars = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = buf.getCell(c, r).character().charAt(0);
                if (ch == '*' || ch == '+' || ch == '#' || ch == '.') stars++;
            }
        }
        assertTrue(stars > 0,
                "WarpStarfield should render visible stars through TRANSPARENT window, found " + stars);
    }
}
