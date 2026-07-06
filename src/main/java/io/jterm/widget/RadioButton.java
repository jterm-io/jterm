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

    public RadioButton(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; invalidate(); }

    public boolean isSelected() { return selected; }

    void setSelected(boolean selected) {
        this.selected = selected;
        invalidate();
    }

    void setGroup(RadioGroup group) { this.group = group; }

    public RadioGroup getGroup() { return group; }

    public void select() {
        if (group != null) {
            group.select(this);
        } else {
            setSelected(true);
        }
    }

    @Override
    protected TerminalSize calculatePreferredSize() {
        return new TerminalSize(label.length() + 4, 1);
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var theme = ThemeManager.active();
        var marker = selected ? "(●)" : "(○)";
        var style = isFocused()
                ? new TextCell(' ', theme.focusFg(), theme.focusBg())
                : new TextCell(' ', theme.foreground(), theme.background());
        graphics.drawString(0, 0, marker + " " + label, style);
    }

    @Override
    public void handleKeyStroke(KeyStroke keyStroke) {
        if (keyStroke.type() == KeyType.CHARACTER && keyStroke.character() == ' ') {
            select();
        } else if (keyStroke.type() == KeyType.ENTER) {
            select();
        }
    }
}
