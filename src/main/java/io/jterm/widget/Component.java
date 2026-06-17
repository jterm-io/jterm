package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.LayoutData;

/**
 * UI widget interface. Every interactive or display element in JTerm implements
 * this interface.
 */
public interface Component {
    /** Current bounds size (after layout). */
    TerminalSize getSize();

    /** Current position relative to parent (after layout). */
    TerminalPosition getPosition();

    /** Natural preferred size. */
    TerminalSize getPreferredSize();

    /** Sets position and size, then triggers any internal layout. */
    void setBounds(TerminalPosition position, TerminalSize size);

    /** Renders this component into the supplied graphics context. */
    void draw(TextGraphics graphics);

    /** Marks this component's cached preferred size as stale. */
    void invalidate();

    /** Whether the component should be drawn and laid out. */
    boolean isVisible();

    /** Sets visibility. */
    void setVisible(boolean visible);

    /** Layout-specific constraints, if any. */
    LayoutData getLayoutData();

    /** Sets layout constraints. */
    void setLayoutData(LayoutData data);

    /** Immediate parent container, or null if unattached. */
    Component getParent();

    /** Sets parent container. */
    void setParent(Component parent);

    /** Called when this component receives a keyboard event. */
    void handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke);

    /** Whether this component currently has keyboard focus. */
    boolean isFocused();

    /** Sets focused state. */
    void setFocused(boolean focused);
}
