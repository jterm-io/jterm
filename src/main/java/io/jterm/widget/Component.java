package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.LayoutData;

/** UI widget interface. */
public interface Component {
    TerminalSize getSize();
    TerminalPosition getPosition();
    TerminalSize getPreferredSize();
    void setBounds(TerminalPosition position, TerminalSize size);
    void draw(TextGraphics graphics);
    void invalidate();
    boolean isVisible();
    void setVisible(boolean visible);
    LayoutData getLayoutData();
    void setLayoutData(LayoutData data);
    Component getParent();
    void setParent(Component parent);
    void handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke);
    boolean isFocused();
    void setFocused(boolean focused);
}
