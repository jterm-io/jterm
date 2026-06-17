package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.util.TerminalTextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A dropdown menu with a title (shown in the {@link MenuBar}) and a list of
 * {@link MenuItem}s and {@link MenuSeparator}s.
 *
 * <h3>Keyboard interaction</h3>
 * <ul>
 *   <li><b>Arrow Up/Down</b> — move selection within the dropdown</li>
 *   <li><b>Enter</b> — activate the selected item and close the menu</li>
 *   <li><b>Escape</b> — close the menu without activating</li>
 *   <li><b>Arrow Left/Right</b> — close this menu (MenuBar handles switching)</li>
 * </ul>
 */
public class Menu extends AbstractComponent {

    private final String title;
    private final char mnemonic;
    private final List<Object> entries = new ArrayList<>();
    private boolean open = false;
    private int selectedIndex = 0;
    private Runnable closeCallback;

    // Styles
    private static final TextCell TITLE_NORMAL = new TextCell(' ', AnsiColor.WHITE, AnsiColor.DEFAULT);
    private static final TextCell TITLE_OPEN = new TextCell(' ', AnsiColor.BLACK, AnsiColor.WHITE, SGR.BOLD);
    private static final TextCell ITEM_NORMAL = new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT);
    private static final TextCell ITEM_SELECTED = new TextCell(' ', AnsiColor.BLACK, AnsiColor.WHITE);
    private static final TextCell SEP_CELL = new TextCell('─', AnsiColor.DEFAULT, AnsiColor.DEFAULT);

    public Menu(String title) {
        this(title, title.isEmpty() ? '\0' : Character.toLowerCase(title.charAt(0)));
    }

    public Menu(String title, char mnemonic) {
        this.title = title;
        this.mnemonic = mnemonic;
    }

    // ── Configuration ──────────────────────────────────────────────

    public String getTitle() { return title; }

    public char getMnemonic() { return mnemonic; }

    public void addMenuItem(MenuItem item) {
        entries.add(item);
        invalidate();
    }

    public void addMenuItem(String label, Runnable action) {
        entries.add(new MenuItem(label, action));
        invalidate();
    }

    public void addSeparator() {
        entries.add(new MenuSeparator());
        invalidate();
    }

    public List<Object> getEntries() { return new ArrayList<>(entries); }

    public boolean isOpen() { return open; }

    public void setOpen(boolean open) {
        this.open = open;
        if (open) {
            selectedIndex = firstSelectableIndex();
        }
        invalidate();
    }

    void setCloseCallback(Runnable cb) { this.closeCallback = cb; }

    // ── Layout ─────────────────────────────────────────────────────

    @Override
    protected TerminalSize calculatePreferredSize() {
        // In the bar, the title is shown as " Title " — 2 chars padding
        return new TerminalSize(title.length() + 2, 1);
    }

    /** Width of the dropdown panel (widest item label + padding). */
    int dropdownWidth() {
        int max = title.length() + 2;
        for (var e : entries) {
            if (e instanceof MenuItem mi) {
                max = Math.max(max, mi.getLabel().length() + 2);
            } else if (e instanceof MenuSeparator) {
                max = Math.max(max, 10);
            }
        }
        return max;
    }

    /** Height of the dropdown panel (one row per entry). */
    int dropdownHeight() {
        return entries.size();
    }

    // ── Rendering ─────────────────────────────────────────────────

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        // Draw the title in the bar
        var style = open ? TITLE_OPEN : TITLE_NORMAL;
        String label = " " + title + " ";
        int pad = size.columns() - label.length();
        if (pad > 0) label += " ".repeat(pad);
        graphics.drawString(0, 0, label, style);

        // If open, draw the dropdown below the title
        if (open && !entries.isEmpty()) {
            int w = dropdownWidth();
            int h = dropdownHeight();
            // Draw dropdown items starting at row 1 (below the bar)
            for (int i = 0; i < h; i++) {
                int row = 1 + i;
                if (row >= size.rows()) break;
                var entry = entries.get(i);
                if (entry instanceof MenuSeparator) {
                    String sep = "─".repeat(Math.max(0, w - 2));
                    graphics.drawString(0, row, " " + sep + " ", SEP_CELL);
                } else if (entry instanceof MenuItem mi) {
                    boolean selected = i == selectedIndex;
                    var itemStyle = selected ? ITEM_SELECTED : ITEM_NORMAL;
                    String text = " " + mi.getLabel() + " ";
                    text = TerminalTextUtils.truncate(text, w);
                    int pp = w - TerminalTextUtils.getTrueWidth(text);
                    if (pp > 0) text += " ".repeat(pp);
                    graphics.drawString(0, row, text, itemStyle);
                }
            }
        }
    }

    // ── Input handling ─────────────────────────────────────────────

    @Override
    public void handleKeyStroke(KeyStroke keyStroke) {
        if (!open) return;
        switch (keyStroke.type()) {
            case ARROW_DOWN -> moveSelection(1);
            case ARROW_UP -> moveSelection(-1);
            case ENTER -> activateSelected();
            case ESCAPE -> close();
            case ARROW_LEFT, ARROW_RIGHT -> close();
            default -> {}
        }
    }

    private void moveSelection(int direction) {
        if (entries.isEmpty()) return;
        int next = selectedIndex;
        for (int i = 0; i < entries.size(); i++) {
            next = next + direction;
            if (next < 0) next = entries.size() - 1;
            if (next >= entries.size()) next = 0;
            if (isSelectable(next)) {
                selectedIndex = next;
                invalidate();
                return;
            }
        }
    }

    private void activateSelected() {
        if (selectedIndex >= 0 && selectedIndex < entries.size()) {
            var entry = entries.get(selectedIndex);
            if (entry instanceof MenuItem mi) {
                mi.activate();
            }
        }
        close();
    }

    private void close() {
        open = false;
        invalidate();
        if (closeCallback != null) closeCallback.run();
    }

    private boolean isSelectable(int index) {
        if (index < 0 || index >= entries.size()) return false;
        return entries.get(index) instanceof MenuItem;
    }

    private int firstSelectableIndex() {
        for (int i = 0; i < entries.size(); i++) {
            if (isSelectable(i)) return i;
        }
        return 0;
    }
}