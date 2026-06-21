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
 * A read-only menu panel that aligns shortcut keys in a left column and
 * right-aligns each description in the remaining width.
 *
 * <p>Layout (inside a {@link Border} the panel size excludes the border):
 * <pre>
 *   │ M    Message Boards   │
 *   │ C    Live Chat        │
 * </pre>
 * The key column is padded to the width of the widest key. A fixed gap
 * separates the key column from the description column. The description is
 * right-aligned inside the remaining columns.
 */
public class MenuPanel extends AbstractComponent {

    /** Immutable menu entry. */
    public record MenuEntry(String key, String description) {}

    private static final int GAP = 3;
    private static final int MIN_DESC_WIDTH = 4;

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
            return new TerminalSize(2, 0);
        }
        int keyColWidth = keyColumnWidth();
        int maxDescWidth = 0;
        for (var item : items) {
            maxDescWidth = Math.max(maxDescWidth, TerminalTextUtils.getTrueWidth(item.description()));
        }
        int columns = keyColWidth + GAP + Math.max(maxDescWidth, MIN_DESC_WIDTH);
        return new TerminalSize(columns, items.size());
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        var theme = ThemeManager.active();
        var normal = new TextCell(' ', theme.foreground(), theme.background());
        var bold = new TextCell(' ', theme.foreground(), theme.background(), SGR.BOLD);

        int keyColWidth = keyColumnWidth();
        int availableDescWidth = Math.max(0, size.columns() - keyColWidth - GAP);

        graphics.fillRectangle(0, 0, size.columns(), size.rows(), normal);

        for (int i = 0; i < items.size() && i < size.rows(); i++) {
            var item = items.get(i);
            int row = i;

            // Key column: right-pad to keyColWidth by appending spaces before the key
            int keyWidth = TerminalTextUtils.getTrueWidth(item.key());
            int keyPadding = Math.max(0, keyColWidth - keyWidth);
            String keyText = " ".repeat(keyPadding) + item.key();
            graphics.drawString(0, row, keyText, bold);

            // Description: right-aligned in remaining columns
            String desc = TerminalTextUtils.truncate(item.description(), availableDescWidth);
            int descWidth = TerminalTextUtils.getTrueWidth(desc);
            int descX = keyColWidth + GAP + Math.max(0, availableDescWidth - descWidth);
            graphics.drawString(descX, row, desc, normal);
        }
    }

    private int keyColumnWidth() {
        int max = 0;
        for (var item : items) {
            max = Math.max(max, TerminalTextUtils.getTrueWidth(item.key()));
        }
        return max;
    }
}
