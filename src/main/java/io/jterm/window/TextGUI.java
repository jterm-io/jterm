package io.jterm.window;

import io.jterm.screen.Screen;
import io.jterm.core.input.KeyStroke;

import java.io.IOException;
import java.util.Collection;

/** GUI interface. */
public interface TextGUI extends AutoCloseable {
    Screen getScreen();
    void addWindow(Window window);
    void removeWindow(Window window);
    Window getActiveWindow();
    void setActiveWindow(Window window);
    boolean processInput() throws IOException;
    void waitForInput() throws IOException;
    void updateScreen() throws IOException;
    void close() throws IOException;
    Collection<Window> getWindows();
}
