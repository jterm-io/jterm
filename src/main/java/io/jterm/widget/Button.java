package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;

import java.util.ArrayList;
import java.util.List;

import io.jterm.util.TerminalTextUtils;

/** Clickable button with label. */
public class Button extends AbstractComponent {
    private String label;
    private final List<Runnable> listeners = new ArrayList<>();

    public Button(String label) {
        this.label = label;
    }

    public void addListener(Runnable listener) { listeners.add(listener); }

    public void click() {
        for (var l : listeners) l.run();
    }

    public void setLabel(String label) {
        this.label = label;
        invalidate();
    }

    public String getLabel() { return label; }

    @Override
    protected TerminalSize calculatePreferredSize() {
        return new TerminalSize(label.length() + 4, 1);
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        // Query theme at draw time so buttons always use the current theme
        // colors, even after runtime theme switching.
        var t = ThemeManager.active();
        var style = isFocused()
                ? new TextCell(' ', t.focusFg(), t.focusBg())
                : new TextCell(' ', t.foreground(), t.background());
        String rendered = "[ " + TerminalTextUtils.truncate(label, Math.max(0, size.columns() - 4)) + " ]";
        // pad to width
        int pad = size.columns() - rendered.length();
        if (pad > 0) rendered += " ".repeat(pad);
        graphics.drawString(0, 0, rendered, style);
    }

    @Override
    public void handleKeyStroke(KeyStroke keyStroke) {
        if (keyStroke.type() == KeyType.ENTER || (keyStroke.type() == KeyType.CHARACTER && keyStroke.character() == ' ')) {
            click();
        }
    }
}
