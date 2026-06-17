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
    private final List<RadioButton> buttons = new ArrayList<>();
    private RadioButton selected;
    private final List<Runnable> selectionListeners = new ArrayList<>();

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

    public RadioButton getSelected() { return selected; }

    public void addSelectionListener(Runnable listener) { selectionListeners.add(listener); }

    public List<RadioButton> getButtons() { return new ArrayList<>(buttons); }

    void notifySelectionListeners() {
        for (var l : selectionListeners) l.run();
    }
}
