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

    /**
     * Creates a button with the given label.
     *
     * @param label the button text
     */
    public Button(String label) {
        this.label = label;
    }

    /**
     * Adds a listener that fires when the button is activated.
     *
     * @param listener the listener to add
     */
    public void addListener(Runnable listener) { listeners.add(listener); }

    /** Fires all registered action listeners. */
    public void click() {
        for (var l : listeners) l.run();
    }

    /**
     * Sets the button label and invalidates the component.
     *
     * @param label the new label
     */
    public void setLabel(String label) {
        this.label = label;
        invalidate();
    }

    /**
     * Returns the current button label.
     *
     * @return the label text
     */
    public String getLabel() { return label; }

    /** Computes the preferred size as label width plus 4 (for {@code "[ ]"} padding). */
    @Override
    protected TerminalSize calculatePreferredSize() {
        return new TerminalSize(label.length() + 4, 1);
    }

    /** Renders the button with {@code [ label ]} padding, using focus colors when focused. */
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

    /**
     * Handles Enter and Space keys by activating the button.
     *
     * @return true if the keystroke was consumed
     */
    @Override
    public boolean handleKeyStroke(KeyStroke keyStroke) {
        if (keyStroke.type() == KeyType.ENTER || (keyStroke.type() == KeyType.CHARACTER && keyStroke.character() == ' ')) {
            click();
            return true;
        }
        return false;
    }
}
