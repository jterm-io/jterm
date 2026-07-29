package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-line text editing widget with cursor movement, scrolling, and
 * emacs-style key bindings.
 *
 * <p>Supports:
 * <ul>
 *   <li>Multi-line text with {@code \n}-separated lines</li>
 *   <li>Horizontal and vertical scrolling</li>
 *   <li>Arrow keys, Home/End, Page Up/Down</li>
 *   <li>Emacs bindings: Ctrl+A/E/K/F/B/P/N/D</li>
 *   <li>Backspace (with line join), Delete (with line join), Enter (new line)</li>
 * </ul>
 */
public class TextArea extends AbstractComponent {

    private final List<String> lines = new ArrayList<>();
    private volatile int cursorRow = 0;
    private volatile int cursorCol = 0;
    private volatile int viewportRow = 0;   // vertical scroll offset (which text row is at top of viewport)
    private volatile int viewportCol = 0;   // horizontal scroll offset (which column is at left of viewport)
    private volatile int preferredColumns = 20;
    private volatile int preferredRows = 5;

    // ── Construction ───────────────────────────────────────────

    public TextArea() {
        lines.add(""); // Start with one empty line
    }

    public TextArea(String text) {
        setText(text);
    }

    public TextArea(String text, int columns, int rows) {
        this.preferredColumns = columns;
        this.preferredRows = rows;
        setText(text);
    }

    // ── Public API ─────────────────────────────────────────────

    public String getText() {
        if (lines.isEmpty()) return "";
        return String.join("\n", lines);
    }

    public void setText(String text) {
        lines.clear();
        if (text.isEmpty()) {
            lines.add("");
        } else {
            // Split preserving trailing newlines
            int start = 0;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    lines.add(text.substring(start, i));
                    start = i + 1;
                }
            }
            lines.add(text.substring(start));
        }
        cursorRow = 0;
        cursorCol = 0;
        viewportRow = 0;
        viewportCol = 0;
        invalidate();
    }

    public int getLineCount() {
        return lines.size();
    }

    public String getLine(int index) {
        return lines.get(index);
    }

    // ── Layout ────────────────────────────────────────────────

    @Override
    protected TerminalSize calculatePreferredSize() {
        return new TerminalSize(preferredColumns, preferredRows);
    }

    // ── Rendering ──────────────────────────────────────────────

    @Override
    protected void drawComponent(TextGraphics graphics) {
        adjustViewport(); // ensure viewport is correct before drawing
        var size = getSize();
        var theme = ThemeManager.active();
        var blank = new TextCell(' ', theme.foreground(), theme.background());

        // Fill background
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), blank);

        // Draw visible lines
        int maxRow = Math.min(size.rows(), lines.size() - viewportRow);
        for (int r = 0; r < maxRow; r++) {
            String line = lines.get(viewportRow + r);
            int visibleStart = viewportCol;
            int visibleEnd = Math.min(line.length(), viewportCol + size.columns());
            if (visibleStart < visibleEnd) {
                String visible = line.substring(visibleStart, visibleEnd);
                graphics.drawString(0, r, visible, blank);
            }
        }

        // Draw cursor (highlight: black text on white background)
        int cursorScreenRow = cursorRow - viewportRow;
        int cursorScreenCol = cursorCol - viewportCol;
        if (cursorScreenRow >= 0 && cursorScreenRow < size.rows()
                && cursorScreenCol >= 0 && cursorScreenCol < size.columns()) {
            String line = cursorRow < lines.size() ? lines.get(cursorRow) : "";
            char c = cursorCol < line.length() ? line.charAt(cursorCol) : ' ';
            graphics.setCell(cursorScreenCol, cursorScreenRow,
                    new TextCell(c, theme.selectionFg(), theme.selectionBg()));
        }
    }

    // ── Key handling ──────────────────────────────────────────

    @Override
    public boolean handleKeyStroke(KeyStroke keyStroke) {
        // Emacs-style key bindings (Ctrl+letter)
        if (keyStroke.type() == KeyType.CHARACTER && keyStroke.ctrl()) {
            switch (keyStroke.character()) {
                case 'A', 'a' -> { cursorCol = 0; adjustViewport(); return true; }
                case 'E', 'e' -> { cursorCol = currentLineLength(); adjustViewport(); return true; }
                case 'K', 'k' -> { killToEndOfLine(); invalidate(); adjustViewport(); return true; }
                case 'F', 'f' -> { moveRight(); return true; }
                case 'B', 'b' -> { moveLeft(); return true; }
                case 'P', 'p' -> { moveUp(); return true; }
                case 'N', 'n' -> { moveDown(); return true; }
                case 'D', 'd' -> { deleteForward(); invalidate(); adjustViewport(); return true; }
                case 'V', 'v' -> { pageDown(); return true; }
                default -> { return false; } // Ignore other Ctrl combos
            }
        }

        switch (keyStroke.type()) {
            case CHARACTER -> {
                insertChar(keyStroke.character());
                invalidate();
                return true;
            }
            case ENTER -> {
                splitLine();
                invalidate();
                return true;
            }
            case BACKSPACE -> {
                backspace();
                invalidate();
                return true;
            }
            case DELETE -> {
                deleteForward();
                invalidate();
                return true;
            }
            case ARROW_LEFT -> { moveLeft(); return true; }
            case ARROW_RIGHT -> { moveRight(); return true; }
            case ARROW_UP -> { moveUp(); return true; }
            case ARROW_DOWN -> { moveDown(); return true; }
            case HOME -> { cursorCol = 0; adjustViewport(); return true; }
            case END -> { cursorCol = currentLineLength(); adjustViewport(); return true; }
            case PAGE_UP -> { pageUp(); return true; }
            case PAGE_DOWN -> { pageDown(); return true; }
            default -> { return false; }
        }
    }

    // ── Text editing operations ───────────────────────────────

    private void insertChar(char c) {
        String line = lines.get(cursorRow);
        String newLine = line.substring(0, cursorCol) + c + line.substring(cursorCol);
        lines.set(cursorRow, newLine);
        cursorCol++;
        adjustViewport();
    }

    private void splitLine() {
        String line = lines.get(cursorRow);
        String before = line.substring(0, cursorCol);
        String after = line.substring(cursorCol);
        lines.set(cursorRow, before);
        lines.add(cursorRow + 1, after);
        cursorRow++;
        cursorCol = 0;
        adjustViewport();
    }

    private void backspace() {
        if (cursorCol == 0) {
            if (cursorRow == 0) return; // can't backspace at very start
            // Join with previous line
            String prev = lines.get(cursorRow - 1);
            String curr = lines.get(cursorRow);
            cursorCol = prev.length();
            lines.set(cursorRow - 1, prev + curr);
            lines.remove(cursorRow);
            cursorRow--;
        } else {
            String line = lines.get(cursorRow);
            String newLine = line.substring(0, cursorCol - 1) + line.substring(cursorCol);
            lines.set(cursorRow, newLine);
            cursorCol--;
        }
        adjustViewport();
    }

    private void deleteForward() {
        if (cursorCol >= currentLineLength()) {
            if (cursorRow >= lines.size() - 1) return; // last line, end — nothing to delete
            // Join with next line
            String curr = lines.get(cursorRow);
            String next = lines.get(cursorRow + 1);
            lines.set(cursorRow, curr + next);
            lines.remove(cursorRow + 1);
        } else {
            String line = lines.get(cursorRow);
            String newLine = line.substring(0, cursorCol) + line.substring(cursorCol + 1);
            lines.set(cursorRow, newLine);
        }
        adjustViewport();
    }

    private void killToEndOfLine() {
        String line = lines.get(cursorRow);
        if (cursorCol >= line.length()) {
            // At end of line — join with next line if exists
            if (cursorRow < lines.size() - 1) {
                String next = lines.get(cursorRow + 1);
                lines.set(cursorRow, line + next);
                lines.remove(cursorRow + 1);
            }
        } else {
            // Delete from cursor to end of line
            lines.set(cursorRow, line.substring(0, cursorCol));
        }
    }

    // ── Cursor movement ────────────────────────────────────────

    private void moveLeft() {
        if (cursorCol > 0) {
            cursorCol--;
        } else if (cursorRow > 0) {
            cursorRow--;
            cursorCol = currentLineLength();
        }
        adjustViewport();
    }

    private void moveRight() {
        if (cursorCol < currentLineLength()) {
            cursorCol++;
        } else if (cursorRow < lines.size() - 1) {
            cursorRow++;
            cursorCol = 0;
        }
        adjustViewport();
    }

    private void moveUp() {
        if (cursorRow > 0) {
            cursorRow--;
            clampColumn();
        }
        adjustViewport();
    }

    private void moveDown() {
        if (cursorRow < lines.size() - 1) {
            cursorRow++;
            clampColumn();
        }
        adjustViewport();
    }

    private void pageUp() {
        int viewportHeight = Math.max(1, getSize().rows() > 0 ? getSize().rows() : preferredRows);
        cursorRow = Math.max(0, cursorRow - viewportHeight);
        clampColumn();
        adjustViewport();
    }

    private void pageDown() {
        int viewportHeight = Math.max(1, getSize().rows() > 0 ? getSize().rows() : preferredRows);
        cursorRow = Math.min(lines.size() - 1, cursorRow + viewportHeight);
        clampColumn();
        adjustViewport();
    }

    /** Clamp cursor column to the end of the current line. */
    private void clampColumn() {
        int len = currentLineLength();
        if (cursorCol > len) cursorCol = len;
    }

    private int currentLineLength() {
        if (cursorRow >= lines.size()) return 0;
        return lines.get(cursorRow).length();
    }

    // ── Viewport adjustment ───────────────────────────────────

    private void adjustViewport() {
        var size = getSize();
        int viewportHeight = Math.max(1, size.rows() > 0 ? size.rows() : preferredRows);
        int viewportWidth = Math.max(1, size.columns() > 0 ? size.columns() : preferredColumns);

        // Vertical: keep cursor visible
        if (cursorRow < viewportRow) {
            viewportRow = cursorRow;
        } else if (cursorRow >= viewportRow + viewportHeight) {
            viewportRow = cursorRow - viewportHeight + 1;
        }

        // Horizontal: keep cursor visible
        if (cursorCol < viewportCol) {
            viewportCol = cursorCol;
        } else if (cursorCol >= viewportCol + viewportWidth) {
            viewportCol = cursorCol - viewportWidth + 1;
        }

        if (viewportRow < 0) viewportRow = 0;
        if (viewportCol < 0) viewportCol = 0;
    }
}