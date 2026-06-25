package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.util.TerminalTextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * A read-only menu panel that displays shortcut keys in brackets and
 * left-aligns descriptions with padding.
 *
 * <p>Layout (each row has a leading and trailing space):
 * <pre>
 *   │  [M]     Message Boards  │
 *   │  [C]     Live Chat       │
 * </pre>
 * The key column is padded to the width of the widest key (including
 * brackets). A fixed gap separates the key column from the description.
 * Each row has 1 space of vertical padding above and below.
 */
public class MenuPanel extends AbstractComponent {

    /** Immutable menu entry. */
    public record MenuEntry(String key, String description) {}

    private static final int GAP = 5;
    private static final int MIN_DESC_WIDTH = 4;
    private static final int VERTICAL_PADDING = 1; // rows above and below items

    private final List<MenuEntry> items = new ArrayList<>();
    private int highlightedIndex = -1;

    public MenuPanel() {}

    /** Adds an item with a shortcut key and description. */
    public void addItem(String key, String description) {
        if (key == null) key = "";
        if (description == null) description = "";
        items.add(new MenuEntry(key, description));
        invalidate();
    }

    /** Removes all items. */
    public void clearItems() {
        items.clear();
        highlightedIndex = -1;
        invalidate();
    }

    /** Returns a copy of the current items. */
    public List<MenuEntry> getItems() {
        return new ArrayList<>(items);
    }

    /** Returns the index of the highlighted item, or -1 if none. */
    public int getHighlightedIndex() {
        return highlightedIndex;
    }

    /** Highlights the item at the given index (rendered in reverse video). Pass -1 to clear. */
    public void setHighlightedIndex(int index) {
        this.highlightedIndex = Math.max(-1, Math.min(index, items.size() - 1));
        invalidate();
    }

    /** Finds the index of the menu item whose key matches (case-insensitive), or -1 if not found. */
    public int indexOfKey(String key) {
        if (key == null) return -1;
        String upper = key.toUpperCase();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).key().equalsIgnoreCase(upper)) return i;
        }
        return -1;
    }

    @Override
    protected TerminalSize calculatePreferredSize() {
        if (items.isEmpty()) {
            return new TerminalSize(2, VERTICAL_PADDING * 2);
        }
        int keyColWidth = keyColumnWidth();
        int maxDescWidth = 0;
        for (var item : items) {
            maxDescWidth = Math.max(maxDescWidth, TerminalTextUtils.getTrueWidth(item.description()));
        }
        // 1 leading space + keyColWidth + GAP + desc + 1 trailing space
        int columns = 1 + keyColWidth + GAP + Math.max(maxDescWidth, MIN_DESC_WIDTH) + 1;
        int rows = items.size() + VERTICAL_PADDING * 2;
        return new TerminalSize(columns, rows);
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        var theme = ThemeManager.active();
        var normal = new TextCell(' ', theme.foreground(), theme.background());
        var bold = new TextCell(' ', theme.foreground(), theme.background(), SGR.BOLD);

        int keyColWidth = keyColumnWidth();
        int availableDescWidth = Math.max(0, size.columns() - 1 - keyColWidth - GAP - 1);

        graphics.fillRectangle(0, 0, size.columns(), size.rows(), normal);

        for (int i = 0; i < items.size() && i < size.rows() - VERTICAL_PADDING; i++) {
            var item = items.get(i);
            int row = i + VERTICAL_PADDING;
            boolean highlighted = (i == highlightedIndex);

            // Key in brackets: "[M]" — right-padded to keyColWidth
            String keyInBrackets = "[" + item.key() + "]";
            int keyWidth = TerminalTextUtils.getTrueWidth(keyInBrackets);
            int keyPadding = Math.max(0, keyColWidth - keyWidth);
            String keyText = " ".repeat(keyPadding) + keyInBrackets;
            // Start at column 1 (leading space)
            // Highlighted cells swap fg/bg directly instead of using SGR.REVERSE.
            // Some CP437 clients (MuffinTerm) don't implement ESC[7m (REVERSE),
            // and on standard ANSI terminals REVERSE would undo the manual swap.
            // Explicit fg/bg color codes work on every terminal.
            var keyCell = highlighted
                    ? new TextCell(' ', theme.background(), theme.foreground(), SGR.BOLD)
                    : bold;
            graphics.drawString(1, row, keyText, keyCell);

            // Description: left-aligned after the gap
            String desc = TerminalTextUtils.truncate(item.description(), availableDescWidth);
            int descX = 1 + keyColWidth + GAP;
            var descCell = highlighted
                    ? new TextCell(' ', theme.background(), theme.foreground())
                    : normal;
            graphics.drawString(descX, row, desc, descCell);

            // If highlighted, fill the rest of the row with inverted spaces
            if (highlighted) {
                int descEnd = descX + TerminalTextUtils.getTrueWidth(desc);
                int rowEnd = size.columns() - 1; // leave 1 trailing space
                if (descEnd < rowEnd) {
                    var fillCell = new TextCell(' ', theme.background(), theme.foreground());
                    graphics.fillRectangle(descEnd, row, rowEnd - descEnd, 1, fillCell);
                }
            }
        }
    }

    private int keyColumnWidth() {
        int max = 0;
        for (var item : items) {
            // Width includes brackets: "[M]" = 3
            int w = TerminalTextUtils.getTrueWidth(item.key()) + 2;
            max = Math.max(max, w);
        }
        return max;
    }
}
