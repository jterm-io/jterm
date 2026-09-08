package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

import java.util.ArrayList;
import java.util.List;

/** Mutually-exclusive group of radio buttons. */
public class RadioGroup {

    /** Creates an empty radio group with no buttons and no selection. */
    public RadioGroup() {}

    private final List<RadioButton> buttons = new ArrayList<>();
    private RadioButton selected;
    private final List<Runnable> selectionListeners = new ArrayList<>();

    /**
     * Adds a radio button to this group. The first button added becomes selected.
     *
     * @param button the button to add
     */
    public void add(RadioButton button) {
        if (!buttons.contains(button)) {
            buttons.add(button);
            button.setGroup(this);
            if (selected == null) {
                select(button);
            } else {
                button.setSelected(false);
            }
        }
    }

    /**
     * Removes a radio button from this group, selecting another if needed.
     *
     * @param button the button to remove
     */
    public void remove(RadioButton button) {
        buttons.remove(button);
        button.setGroup(null);
        if (selected == button) {
            selected = buttons.isEmpty() ? null : buttons.get(0);
            if (selected != null) selected.setSelected(true);
            notifySelectionListeners();
        }
    }

    void select(RadioButton button) {
        if (selected == button) return;
        if (button != null && !buttons.contains(button)) return;
        if (selected != null) selected.setSelected(false);
        selected = button;
        if (selected != null) selected.setSelected(true);
        notifySelectionListeners();
    }

    /**
     * Returns the currently selected radio button, or {@code null} if none.
     *
     * @return the selected button, or {@code null} if none
     */
    public RadioButton getSelected() { return selected; }

    /**
     * Adds a listener that fires whenever the selection changes.
     *
     * @param listener the listener to add
     */
    public void addSelectionListener(Runnable listener) { selectionListeners.add(listener); }

    /**
     * Returns a defensive copy of the buttons in this group.
     *
     * @return the buttons in this group
     */
    public List<RadioButton> getButtons() { return new ArrayList<>(buttons); }

    void notifySelectionListeners() {
        for (var l : selectionListeners) l.run();
    }
}
