package io.jterm.screen;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.style.TextCell;
import java.io.IOException;

public interface Screen extends AutoCloseable {
    void startScreen() throws IOException;
    void stopScreen() throws IOException;
    void clear();
    void setCell(int col, int row, TextCell cell);
    void setCell(TerminalPosition pos, TextCell cell);
    TextCell getFrontCell(int col, int row);
    TextCell getBackCell(int col, int row);
    void refresh() throws IOException;
    void refresh(RefreshType type) throws IOException;
    TerminalSize getTerminalSize();
    TerminalSize doResizeIfNecessary() throws IOException;
    void setCursorPosition(TerminalPosition position);
    TerminalPosition getCursorPosition();
    @Override void close() throws IOException;
}
