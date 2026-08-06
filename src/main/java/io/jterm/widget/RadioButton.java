package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;

/** Mutually-exclusive radio button. */
public class RadioButton extends AbstractComponent {
    private String label;
    private boolean selected;
    private RadioGroup group;

    /**
     * Creates a radio button with the given label.
     *
     * @param label the text shown next to the radio button
     */
    public RadioButton(String label) {
        this.label = label;
    }

    /** Returns the label text. */
    public String getLabel() { return label; }
    /** Sets the label text and invalidates the component. */
    public void setLabel(String label) { this.label = label; invalidate(); }

    /** Returns whether this radio button is the selected one in its group. */
    public boolean isSelected() { return selected; }

    void setSelected(boolean selected) {
        this.selected = selected;
        invalidate();
    }

    void setGroup(RadioGroup group) { this.group = group; }

    /** Returns the radio group this button belongs to, or {@code null}. */
    public RadioGroup getGroup() { return group; }

    /** Selects this button, delegating to the group when attached. */
    public void select() {
        if (group != null) {
            group.select(this);
        } else {
            setSelected(true);
        }
    }

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
     * Renders the radio marker and label, using focus colors when focused.
     *
     * @param graphics the text-graphics target
     */
    @Override
    protected void drawComponent(TextGraphics graphics) {
        var theme = ThemeManager.active();
        var marker = selected ? "(●)" : "(○)";
        var style = isFocused()
                ? new TextCell(' ', theme.focusFg(), theme.focusBg())
                : new TextCell(' ', theme.foreground(), theme.background());
        graphics.drawString(0, 0, marker + " " + label, style);
    }

    /**
     * Selects this radio button on Space; ignores Enter (handled by parent dialogs).
     *
     * @param keyStroke the keystroke to handle
     * @return {@code true} if the keystroke was consumed
     */
    @Override
    public boolean handleKeyStroke(KeyStroke keyStroke) {
        if (keyStroke.type() == KeyType.CHARACTER && keyStroke.character() == ' ') {
            select();
            return true;
        }
        // Enter is intentionally NOT handled here. In dialog contexts, Enter
        // means "submit" — the dialog handles it. If RadioButton also selected
        // on Enter, the focused radio would change state before the dialog's
        // submit() runs.
        return false;
    }
}
