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

    public CheckBox(String label) { this.label = label; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; invalidate(); }

    public void toggle() {
        selected = !selected;
        for (var l : listeners) l.run();
        invalidate();
    }

    public void addListener(Runnable listener) { listeners.add(listener); }

    @Override
    protected TerminalSize calculatePreferredSize() {
        return new TerminalSize(label.length() + 4, 1);
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var theme = ThemeManager.active();
        var marker = selected ? "[✓]" : "[ ]";
        var style = isFocused()
                ? new TextCell(' ', theme.focusFg(), theme.focusBg())
                : new TextCell(' ', theme.foreground(), theme.background());
        graphics.drawString(0, 0, marker + " " + label, style);
    }

    @Override
    public void handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke) {
        if (keyStroke.type() == io.jterm.core.input.KeyType.CHARACTER && keyStroke.character() == ' ') {
            toggle();
        } else if (keyStroke.type() == io.jterm.core.input.KeyType.ENTER) {
            toggle();
        }
    }
}
