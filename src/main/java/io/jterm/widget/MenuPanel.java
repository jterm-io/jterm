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
        invalidate();
    }

    /** Returns a copy of the current items. */
    public List<MenuEntry> getItems() {
        return new ArrayList<>(items);
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

            // Key in brackets: "[M]" — right-padded to keyColWidth
            String keyInBrackets = "[" + item.key() + "]";
            int keyWidth = TerminalTextUtils.getTrueWidth(keyInBrackets);
            int keyPadding = Math.max(0, keyColWidth - keyWidth);
            String keyText = " ".repeat(keyPadding) + keyInBrackets;
            // Start at column 1 (leading space)
            graphics.drawString(1, row, keyText, bold);

            // Description: left-aligned after the gap
            String desc = TerminalTextUtils.truncate(item.description(), availableDescWidth);
            int descX = 1 + keyColWidth + GAP;
            graphics.drawString(descX, row, desc, normal);
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
