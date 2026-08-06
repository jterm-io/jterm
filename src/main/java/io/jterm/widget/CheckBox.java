package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;

/** Toggle checkbox. */
public class CheckBox extends AbstractComponent {
    private boolean selected;
    private String label;
    private final java.util.List<Runnable> listeners = new java.util.ArrayList<>();

    /**
     * Creates a checkbox with the given label.
     *
     * @param label the text shown next to the checkbox
     */
    public CheckBox(String label) { this.label = label; }

    /** Returns the label text. */
    public String getLabel() { return label; }
    /** Returns whether the checkbox is currently checked. */
    public boolean isSelected() { return selected; }
    /** Sets the checked state without firing listeners. */
    public void setSelected(boolean selected) { this.selected = selected; invalidate(); }

    /** Toggles the checked state and fires all registered listeners. */
    public void toggle() {
        selected = !selected;
        for (var l : listeners) l.run();
        invalidate();
    }

    /** Adds a listener fired whenever the state is toggled. */
    public void addListener(Runnable listener) { listeners.add(listener); }

    /**
     * Returns the preferred size: label width plus 4 columns for the marker.
     *
     * @return the preferred terminal size
     */
    @Override
    protected TerminalSize calculatePreferredSize() {
        return new TerminalSize(label.length() + 4, 1);
    }

    /**
     * Renders the checkbox marker and label, using focus colors when focused.
     *
     * @param graphics the text-graphics target
     */
    @Override
    protected void drawComponent(TextGraphics graphics) {
        var theme = ThemeManager.active();
        var marker = selected ? "[✓]" : "[ ]";
        var style = isFocused()
                ? new TextCell(' ', theme.focusFg(), theme.focusBg())
                : new TextCell(' ', theme.foreground(), theme.background());
        graphics.drawString(0, 0, marker + " " + label, style);
    }

    /**
     * Toggles on Space; ignores Enter (handled by parent dialogs).
     *
     * @param keyStroke the keystroke to handle
     * @return {@code true} if the keystroke was consumed
     */
    @Override
    public boolean handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke) {
        if (keyStroke.type() == io.jterm.core.input.KeyType.CHARACTER && keyStroke.character() == ' ') {
            toggle();
            return true;
        }
        // Enter is intentionally NOT handled here. In dialog contexts, Enter
        // means "submit" — the dialog handles it. If CheckBox also toggled on
        // Enter, the focused checkbox would flip state before the dialog's
        // submit() runs, corrupting the saved state.
        return false;
    }
}
