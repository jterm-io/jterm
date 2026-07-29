package io.jterm.widget;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.layout.LinearLayout;
import io.jterm.screen.DefaultScreen;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowImpl;
import io.jterm.window.Window;
import io.jterm.widget.Component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for the keystroke consumption contract.
 *
 * <p>These tests verify that {@link Component#handleKeyStroke} returns
 * {@code true} when the keystroke is consumed and {@code false} when it is not,
 * and that {@link DefaultTextGUI} only forwards unconsumed keystrokes to the
 * window.
 */
class KeyStrokeConsumptionTest {

    // ===== TextBox returns true for consumed keystrokes =====

    @Test
    @DisplayName("TextBox returns true when typing a character")
    void textBoxReturnsTrueForCharacter() {
        var tb = new TextBox();
        tb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        boolean consumed = tb.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, 'a', false, false, false));
        assertTrue(consumed, "TextBox should consume character keys");
        assertEquals("a", tb.getValue());
    }

    @Test
    @DisplayName("TextBox returns true for Backspace")
    void textBoxReturnsTrueForBackspace() {
        var tb = new TextBox();
        tb.setValue("abc");
        boolean consumed = tb.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertTrue(consumed, "TextBox should consume Backspace");
    }

    @Test
    @DisplayName("TextBox returns true for Arrow keys")
    void textBoxReturnsTrueForArrows() {
        var tb = new TextBox();
        tb.setValue("abc");
        boolean consumed = tb.handleKeyStroke(new KeyStroke(KeyType.ARROW_LEFT));
        assertTrue(consumed, "TextBox should consume Arrow Left");
        consumed = tb.handleKeyStroke(new KeyStroke(KeyType.ARROW_RIGHT));
        assertTrue(consumed, "TextBox should consume Arrow Right");
    }

    @Test
    @DisplayName("TextBox returns true for Home and End")
    void textBoxReturnsTrueForHomeEnd() {
        var tb = new TextBox();
        tb.setValue("abc");
        assertTrue(tb.handleKeyStroke(new KeyStroke(KeyType.HOME)));
        assertTrue(tb.handleKeyStroke(new KeyStroke(KeyType.END)));
    }

    @Test
    @DisplayName("TextBox returns true for Ctrl+A (emacs beginning-of-line)")
    void textBoxReturnsTrueForCtrlA() {
        var tb = new TextBox();
        tb.setValue("abc");
        boolean consumed = tb.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, 'a', true, false, false));
        assertTrue(consumed, "TextBox should consume Ctrl+A");
    }

    @Test
    @DisplayName("TextBox returns false for unrecognized key (e.g. F1)")
    void textBoxReturnsFalseForUnrecognized() {
        var tb = new TextBox();
        boolean consumed = tb.handleKeyStroke(new KeyStroke(KeyType.F1));
        assertFalse(consumed, "TextBox should not consume F1");
    }

    // ===== Button returns true only for Enter/Space =====

    @Test
    @DisplayName("Button returns true for Enter")
    void buttonReturnsTrueForEnter() {
        var btn = new Button("OK");
        boolean consumed = btn.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertTrue(consumed, "Button should consume Enter");
    }

    @Test
    @DisplayName("Button returns true for Space")
    void buttonReturnsTrueForSpace() {
        var btn = new Button("OK");
        boolean consumed = btn.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, ' ', false, false, false));
        assertTrue(consumed, "Button should consume Space");
    }

    @Test
    @DisplayName("Button returns false for arrow keys")
    void buttonReturnsFalseForArrows() {
        var btn = new Button("OK");
        assertFalse(btn.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN)));
    }

    // ===== CheckBox returns true only for Space =====

    @Test
    @DisplayName("CheckBox returns true for Space toggle")
    void checkBoxReturnsTrueForSpace() {
        var cb = new CheckBox("Option");
        boolean consumed = cb.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, ' ', false, false, false));
        assertTrue(consumed, "CheckBox should consume Space");
        assertTrue(cb.isSelected());
    }

    @Test
    @DisplayName("CheckBox returns false for Enter")
    void checkBoxReturnsFalseForEnter() {
        var cb = new CheckBox("Option");
        assertFalse(cb.handleKeyStroke(new KeyStroke(KeyType.ENTER)));
    }

    // ===== DefaultTextGUI does not forward consumed keystrokes to window =====

    @Test
    @DisplayName("DefaultTextGUI does not call window.handleKeyStroke when focused component consumes the key")
    void guiDoesNotForwardConsumedKey() throws Exception {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);

        // Create a window that tracks whether handleKeyStroke was called
        var window = new WindowImpl("Test") {
            boolean windowKeyCalled = false;

            @Override
            public boolean handleKeyStroke(io.jterm.core.input.KeyStroke ks) {
                windowKeyCalled = true;
                return true;
            }
        };

        var textBox = new TextBox();
        window.getContents().setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        window.getContents().addComponent(textBox);
        gui.addWindow(window);

        // Type a character — TextBox should consume it, window should NOT receive it
        gui.processInput(new KeyStroke(KeyType.CHARACTER, 'a', false, false, false));

        assertEquals("a", textBox.getValue(), "TextBox should have the character");
        // The window's handleKeyStroke should not have been called because the
        // focused TextBox consumed the keystroke
        assertFalse(window.windowKeyCalled,
                "Window should NOT receive keystroke when focused component consumed it");
    }

    @Test
    @DisplayName("DefaultTextGUI forwards unconsumed keystrokes to the window")
    void guiForwardsUnconsumedKey() throws Exception {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);

        var window = new WindowImpl("Test") {
            boolean windowKeyCalled = false;

            @Override
            public boolean handleKeyStroke(io.jterm.core.input.KeyStroke ks) {
                windowKeyCalled = true;
                return true;
            }
        };

        // Use a Button which consumes Enter but NOT arrows
        var button = new Button("OK");
        window.getContents().setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        window.getContents().addComponent(button);
        gui.addWindow(window);

        // Send ArrowDown — Button does NOT consume it, so window should receive it
        gui.processInput(new KeyStroke(KeyType.ARROW_DOWN));

        assertTrue(window.windowKeyCalled,
                "Window should receive unconsumed keystroke");
    }

    // ===== Double-echo regression test =====

    @Test
    @DisplayName("Typing 'a' into a TextBox via GUI produces single 'a', not 'aa'")
    void noDoubleEchoWhenTyping() throws Exception {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);

        // Use a window that would cause double-echo by routing the character
        // to the focused TextBox again in its handleKeyStroke (simulating a
        // screen that manually dispatches to the focused component)
        var textBox = new TextBox();
        var window = new WindowImpl("Test") {
            @Override
            public boolean handleKeyStroke(io.jterm.core.input.KeyStroke ks) {
                // Simulate a screen that manually routes to the focused component
                // (this is the bug scenario — the window re-dispatches to the TextBox)
                var focused = getFocusedComponent();
                if (focused != null) {
                    focused.handleKeyStroke(ks);
                }
                return true;
            }
        };

        window.getContents().setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        window.getContents().addComponent(textBox);
        gui.addWindow(window);

        gui.processInput(new KeyStroke(KeyType.CHARACTER, 'a', false, false, false));

        assertEquals("a", textBox.getValue(),
                "TextBox should contain 'a' (not 'aa') — the window should not have received the keystroke");
    }
}