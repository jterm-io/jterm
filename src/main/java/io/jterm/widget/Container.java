package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.LayoutManager;

import java.util.List;

/** Container that holds child components. */
public interface Container extends Component {
    List<Component> getChildren();
    void addComponent(Component component);
    void addComponent(Component component, Object layoutData);
    void removeComponent(Component component);
    LayoutManager getLayoutManager();
    void setLayoutManager(LayoutManager layoutManager);
}
