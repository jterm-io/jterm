package io.jterm.window;

import io.jterm.animation.StarfieldBackground;
import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.screen.DefaultScreen;
import io.jterm.widget.Button;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers public methods in the window package with zero line coverage.
 */
class WindowCoverageGapsTest {

    @Test
    void windowDefaultGetPreferredSizeReturnsSize() {
        Window window = new WindowImpl("test");
        window.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 10));
        assertEquals(window.getSize(), window.getPreferredSize());
    }

    @Test
    void animatedBackgroundWindowGetBackground() {
        var bg = new StarfieldBackground(new TerminalSize(80, 24));
        var gui = new DefaultTextGUI(new DefaultScreen(new MockTerminal(new TerminalSize(80, 24))));
        var window = new AnimatedBackgroundWindow(bg, gui);
        assertSame(bg, window.getBackground());
    }

    @Test
    void defaultTextGUIRunEventLoopStopsOnCtrlC() throws Exception {
        var term = new MockTerminal(new TerminalSize(40, 20));
        var screen = new DefaultScreen(term);
        var gui = new DefaultTextGUI(screen);

        var window = new WindowImpl("test");
        window.setHints(java.util.List.of(WindowHint.CENTERED));
        window.getContents().addComponent(new Button("OK"));
        gui.addWindow(window);

        // Inject Ctrl+C into the screen's terminal input queue via a custom terminal wrapper
        var injectingTerm = new MockTerminal(new TerminalSize(40, 20)) {
            boolean injected = false;
            @Override
            public java.util.Optional<io.jterm.core.input.KeyStroke> pollInput() {
                if (!injected) {
                    injected = true;
                    return java.util.Optional.of(KeyStroke.character('C', true, false, false));
                }
                gui.stopRunning();
                return java.util.Optional.empty();
            }
        };
        var injectingScreen = new DefaultScreen(injectingTerm);
        var injectingGui = new DefaultTextGUI(injectingScreen);
        injectingGui.addWindow(window);

        assertDoesNotThrow(() -> injectingGui.runEventLoop());
        assertFalse(injectingGui.isRunning());
    }
}
