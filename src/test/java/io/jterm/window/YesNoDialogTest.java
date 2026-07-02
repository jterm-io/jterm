package io.jterm.window;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.screen.DefaultScreen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class YesNoDialogTest {

    private DefaultTextGUI gui;

    @BeforeEach
    void setUp() {
        var terminal = new MockTerminal(new TerminalSize(80, 24));
        var screen = new DefaultScreen(terminal);
        gui = new DefaultTextGUI(screen);
    }

    @Test
    @DisplayName("Dialog opens with title and prompt, visible in GUI")
    void opensWithTitleAndPrompt() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete this thread?");
        dialog.open();

        assertTrue(gui.containsWindow(dialog));
        assertEquals("Confirm", dialog.getTitle());
        assertEquals("Delete this thread?", dialog.getPromptText());
    }

    @Test
    @DisplayName("Dialog has MODAL and CENTERED hints")
    void hasModalAndCenteredHints() {
        var dialog = new YesNoDialog(gui, "Confirm", "Are you sure?");

        assertTrue(dialog.getHints().contains(WindowHint.MODAL));
        assertTrue(dialog.getHints().contains(WindowHint.CENTERED));
    }

    @Test
    @DisplayName("'y' key fires onYes and closes dialog")
    void yKeyFiresYesAndCloses() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        AtomicBoolean yesFired = new AtomicBoolean(false);
        dialog.onYes(() -> yesFired.set(true));

        dialog.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, 'y', false, false, false));
        gui.updateScreen();

        assertTrue(yesFired.get());
        assertFalse(gui.containsWindow(dialog));
    }

    @Test
    @DisplayName("'Y' key fires onYes and closes dialog")
    void upperYKeyFiresYes() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        AtomicBoolean yesFired = new AtomicBoolean(false);
        dialog.onYes(() -> yesFired.set(true));

        dialog.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, 'Y', false, false, false));
        gui.updateScreen();

        assertTrue(yesFired.get());
    }

    @Test
    @DisplayName("'n' key fires onNo and closes dialog")
    void nKeyFiresNoAndCloses() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        AtomicBoolean noFired = new AtomicBoolean(false);
        dialog.onNo(() -> noFired.set(true));

        dialog.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, 'n', false, false, false));
        gui.updateScreen();

        assertTrue(noFired.get());
        assertFalse(gui.containsWindow(dialog));
    }

    @Test
    @DisplayName("'N' key fires onNo and closes dialog")
    void upperNKeyFiresNo() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        AtomicBoolean noFired = new AtomicBoolean(false);
        dialog.onNo(() -> noFired.set(true));

        dialog.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, 'N', false, false, false));
        gui.updateScreen();

        assertTrue(noFired.get());
    }

    @Test
    @DisplayName("Escape fires onCancel and closes dialog")
    void escapeFiresCancelAndCloses() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        AtomicBoolean cancelFired = new AtomicBoolean(false);
        dialog.onCancel(() -> cancelFired.set(true));

        dialog.handleKeyStroke(new KeyStroke(KeyType.ESCAPE));
        gui.updateScreen();

        assertTrue(cancelFired.get());
        assertFalse(gui.containsWindow(dialog));
    }

    @Test
    @DisplayName("'y' does not fire onNo or onCancel")
    void yKeyDoesNotFireNoOrCancel() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        AtomicBoolean noFired = new AtomicBoolean(false);
        AtomicBoolean cancelFired = new AtomicBoolean(false);
        dialog.onNo(() -> noFired.set(true));
        dialog.onCancel(() -> cancelFired.set(true));

        dialog.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, 'y', false, false, false));
        gui.updateScreen();

        assertFalse(noFired.get());
        assertFalse(cancelFired.get());
    }

    @Test
    @DisplayName("'n' does not fire onYes or onCancel")
    void nKeyDoesNotFireYesOrCancel() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        AtomicBoolean yesFired = new AtomicBoolean(false);
        AtomicBoolean cancelFired = new AtomicBoolean(false);
        dialog.onYes(() -> yesFired.set(true));
        dialog.onCancel(() -> cancelFired.set(true));

        dialog.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, 'n', false, false, false));
        gui.updateScreen();

        assertFalse(yesFired.get());
        assertFalse(cancelFired.get());
    }

    @Test
    @DisplayName("No callbacks set — 'y' still closes without throwing")
    void yesWithNoCallbackDoesNotThrow() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        assertDoesNotThrow(() ->
                dialog.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, 'y', false, false, false)));
        gui.updateScreen();
        assertFalse(gui.containsWindow(dialog));
    }

    @Test
    @DisplayName("No callbacks set — escape still closes without throwing")
    void escapeWithNoCallbackDoesNotThrow() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        assertDoesNotThrow(() ->
                dialog.handleKeyStroke(new KeyStroke(KeyType.ESCAPE)));
        gui.updateScreen();
        assertFalse(gui.containsWindow(dialog));
    }

    @Test
    @DisplayName("Other character keys do not close the dialog or fire callbacks")
    void otherKeysDoNothing() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        AtomicBoolean anyFired = new AtomicBoolean(false);
        dialog.onYes(() -> anyFired.set(true));
        dialog.onNo(() -> anyFired.set(true));
        dialog.onCancel(() -> anyFired.set(true));

        dialog.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, 'x', false, false, false));
        dialog.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, 'a', false, false, false));
        dialog.handleKeyStroke(new KeyStroke(KeyType.ENTER));

        assertFalse(anyFired.get());
        assertTrue(gui.containsWindow(dialog));
    }

    @Test
    @DisplayName("Multiple onYes callbacks are all notified")
    void multipleYesCallbacksNotified() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        AtomicBoolean first = new AtomicBoolean(false);
        AtomicBoolean second = new AtomicBoolean(false);
        dialog.onYes(() -> first.set(true));
        dialog.onYes(() -> second.set(true));

        dialog.handleKeyStroke(new KeyStroke(KeyType.CHARACTER, 'y', false, false, false));

        assertTrue(first.get());
        assertTrue(second.get());
    }

    @Test
    @DisplayName("Default prompt text is used when none provided")
    void defaultPromptText() {
        var dialog = new YesNoDialog(gui, "Confirm");
        assertEquals("Are you sure?", dialog.getPromptText());
    }

    @Test
    @DisplayName("Programmatic yes() fires onYes and closes")
    void programmaticYes() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        AtomicBoolean yesFired = new AtomicBoolean(false);
        dialog.onYes(() -> yesFired.set(true));

        dialog.yes();
        gui.updateScreen();

        assertTrue(yesFired.get());
        assertFalse(gui.containsWindow(dialog));
    }

    @Test
    @DisplayName("Programmatic no() fires onNo and closes")
    void programmaticNo() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        AtomicBoolean noFired = new AtomicBoolean(false);
        dialog.onNo(() -> noFired.set(true));

        dialog.no();
        gui.updateScreen();

        assertTrue(noFired.get());
        assertFalse(gui.containsWindow(dialog));
    }

    @Test
    @DisplayName("Programmatic cancel() fires onCancel and closes")
    void programmaticCancel() throws Exception {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        dialog.open();

        AtomicBoolean cancelFired = new AtomicBoolean(false);
        dialog.onCancel(() -> cancelFired.set(true));

        dialog.cancel();
        gui.updateScreen();

        assertTrue(cancelFired.get());
        assertFalse(gui.containsWindow(dialog));
    }

    @Test
    @DisplayName("getStatusText returns the key hint string")
    void statusTextContainsKeyHints() {
        var dialog = new YesNoDialog(gui, "Confirm", "Delete?");
        String status = dialog.getStatusText();
        assertTrue(status.contains("Y"));
        assertTrue(status.contains("N"));
        assertTrue(status.toLowerCase().contains("esc"));
    }
}