package io.jterm.window;

import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.layout.BorderLayout;
import io.jterm.layout.LinearLayout;
import io.jterm.widget.Borders;
import io.jterm.widget.Component;
import io.jterm.widget.Label;
import io.jterm.widget.Panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Modal yes/no confirmation dialog that accepts single-keypress input.
 *
 * <p>Displays a prompt message and responds to three keys:
 * <ul>
 *   <li><b>Y</b> (or y) — confirms, fires {@link #onYes(Runnable)} callbacks</li>
 *   <li><b>N</b> (or n) — denies, fires {@link #onNo(Runnable)} callbacks</li>
 *   <li><b>Esc</b> — cancels, fires {@link #onCancel(Runnable)} callbacks</li>
 * </ul>
 * The dialog closes immediately after any of these keys. No text input or Enter
 * is required — this is a single-keystroke dialog, not a type-to-confirm dialog.
 *
 * <p>The dialog is {@link WindowHint#MODAL} and {@link WindowHint#CENTERED},
 * sized to fit its content rather than filling the whole screen.
 */
public class YesNoDialog extends AbstractWindow {

    private static final String DEFAULT_PROMPT = "Are you sure?";
    private static final String STATUS_HINT = "[Y] Yes  [N] No  [Esc] Cancel";

    private final DefaultTextGUI gui;
    private final Label promptLabel;
    private final Label statusLabel;
    private final List<Runnable> yesListeners = new ArrayList<>();
    private final List<Runnable> noListeners = new ArrayList<>();
    private final List<Runnable> cancelListeners = new ArrayList<>();

    /**
     * Creates a yes/no dialog with the given title and default prompt.
     *
     * @param gui   the GUI to add/remove the dialog window from
     * @param title the window title
     */
    public YesNoDialog(DefaultTextGUI gui, String title) {
        this(gui, title, DEFAULT_PROMPT);
    }

    /**
     * Creates a yes/no dialog with the given title and prompt.
     *
     * @param gui     the GUI to add/remove the dialog window from
     * @param title   the window title
     * @param prompt  the prompt message displayed to the user
     */
    public YesNoDialog(DefaultTextGUI gui, String title, String prompt) {
        super(title);
        this.gui = Objects.requireNonNull(gui, "gui");
        this.promptLabel = new Label(Objects.requireNonNull(prompt, "prompt"));
        this.statusLabel = new Label(STATUS_HINT);

        setHints(List.of(WindowHint.MODAL, WindowHint.CENTERED));
        buildLayout();
    }

    private void buildLayout() {
        var contents = getContents();
        contents.setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL, 1));

        // Prompt text in a bordered panel for visual emphasis
        var promptPanel = new Panel(new LinearLayout(LinearLayout.Direction.VERTICAL, 0));
        promptPanel.addComponent(promptLabel);
        contents.addComponent(Borders.singleLine(promptPanel));

        // Status hint below the prompt
        contents.addComponent(statusLabel);
    }

    /** Shows the dialog in the GUI. */
    public void open() {
        gui.addWindow(this);
    }

    /** Registers a listener to be called when the user presses Y. */
    public YesNoDialog onYes(Runnable listener) {
        yesListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /** Registers a listener to be called when the user presses N. */
    public YesNoDialog onNo(Runnable listener) {
        noListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /** Registers a listener to be called when the user presses Esc. */
    public YesNoDialog onCancel(Runnable listener) {
        cancelListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /** Programmatically triggers the "yes" action and closes the dialog. */
    public void yes() {
        closeDialog();
        for (Runnable listener : yesListeners) {
            listener.run();
        }
    }

    /** Programmatically triggers the "no" action and closes the dialog. */
    public void no() {
        closeDialog();
        for (Runnable listener : noListeners) {
            listener.run();
        }
    }

    /** Cancels the dialog without confirming or denying. */
    public void cancel() {
        closeDialog();
        for (Runnable listener : cancelListeners) {
            listener.run();
        }
    }

    private void closeDialog() {
        gui.removeWindow(this);
    }

    /** Returns the prompt text displayed to the user. */
    public String getPromptText() {
        return promptLabel.getText();
    }

    /** Returns the status hint text shown at the bottom of the dialog. */
    public String getStatusText() {
        return statusLabel.getText();
    }

    @Override
    public TerminalSize getPreferredSize() {
        // Width: prompt length + 4 (border padding) or status hint length, whichever is wider
        int promptWidth = promptLabel.getText().length() + 4;
        int statusWidth = statusLabel.getText().length();
        int width = Math.max(promptWidth, statusWidth) + 4; // +4 for window decorations
        // Height: 1 (title) + 1 (border top) + 1 (prompt) + 1 (border bottom) + 1 (status) + 1 (border bottom)
        int height = 7;
        return new TerminalSize(width, height);
    }

    @Override
    public boolean handleKeyStroke(KeyStroke keyStroke) {
        if (keyStroke.type() == KeyType.ESCAPE) {
            cancel();
            return true;
        }
        if (keyStroke.type() == KeyType.ENTER) {
            yes();
            return true;
        }
        if (keyStroke.type() == KeyType.CHARACTER) {
            char ch = keyStroke.character();
            if (ch == 'y' || ch == 'Y') {
                yes();
                return true;
            }
            if (ch == 'n' || ch == 'N') {
                no();
                return true;
            }
        }
        // All other keys (arrows, other characters, etc.) are ignored.
        // This is a single-keystroke dialog — no text input fields to forward to.
        return false;
    }
}