package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.util.TerminalTextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A dropdown menu with a title (shown in the {@link MenuBar}) and a list of
 * {@link MenuItem}s and {@link MenuSeparator}s.
 *
 * <p><b>Keyboard interaction</b></p>
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

    // Styles — resolved from ThemeManager at draw time
    private TextCell titleNormalStyle() {
        var t = ThemeManager.active();
        return new TextCell(' ', t.foreground(), t.background());
    }
    private TextCell titleOpenStyle() {
        var t = ThemeManager.active();
        return new TextCell(' ', t.focusFg(), t.focusBg(), SGR.BOLD);
    }
    private TextCell itemNormalStyle() {
        var t = ThemeManager.active();
        return new TextCell(' ', t.foreground(), t.background());
    }
    private TextCell itemSelectedStyle() {
        var t = ThemeManager.active();
        return new TextCell(' ', t.selectionFg(), t.selectionBg());
    }
    private TextCell sepCellStyle() {
        var t = ThemeManager.active();
        return new TextCell('─', t.border(), t.background());
    }

    /**
     * Creates a menu whose mnemonic defaults to the first character of the title.
     *
     * @param title the menu title shown in the menu bar
     */
    public Menu(String title) {
        this(title, title.isEmpty() ? '\0' : Character.toLowerCase(title.charAt(0)));
    }

    /**
     * Creates a menu with an explicit mnemonic.
     *
     * @param title    the menu title shown in the menu bar
     * @param mnemonic the mnemonic character used for keyboard activation
     */
    public Menu(String title, char mnemonic) {
        this.title = title;
        this.mnemonic = mnemonic;
    }

    // ── Configuration ──────────────────────────────────────────────

    /**
     * Returns the menu title.
     *
     * @return the menu title
     */
    public String getTitle() { return title; }

    /**
     * Returns the mnemonic character.
     *
     * @return the mnemonic character
     */
    public char getMnemonic() { return mnemonic; }

    /**
     * Adds a {@link MenuItem} to the dropdown.
     *
     * @param item the item to add
     */
    public void addMenuItem(MenuItem item) {
        entries.add(item);
        invalidate();
    }

    /**
     * Convenience method: creates and adds a {@link MenuItem} with the given label and action.
     *
     * @param label  the item label
     * @param action the action to run when activated
     */
    public void addMenuItem(String label, Runnable action) {
        entries.add(new MenuItem(label, action));
        invalidate();
    }

    /** Adds a separator line to the dropdown. */
    public void addSeparator() {
        entries.add(new MenuSeparator());
        invalidate();
    }

    /**
     * Returns a defensive copy of the entries (items and separators).
     *
     * @return a copy of the entries
     */
    public List<Object> getEntries() { return new ArrayList<>(entries); }

    /**
     * Returns whether the dropdown is currently open.
     *
     * @return true if open
     */
    public boolean isOpen() { return open; }

    /**
     * Returns the index of the currently selected dropdown entry.
     *
     * @return the selected index
     */
    public int getSelectedIndex() { return selectedIndex; }

    /**
     * Opens or closes the dropdown. Opening resets the selection to the first
     * selectable entry.
     *
     * @param open {@code true} to open, {@code false} to close
     */
    public void setOpen(boolean open) {
        this.open = open;
        if (open) {
            selectedIndex = firstSelectableIndex();
        }
        invalidate();
    }

    void setCloseCallback(Runnable cb) { this.closeCallback = cb; }

    // ── Layout ─────────────────────────────────────────────────────

    /**
     * Returns the preferred size of the title bar cell.
     *
     * @return the preferred terminal size (title length + 2 padding, 1 row)
     */
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

    /**
     * Draws the title bar cell and, when open, the dropdown panel below it.
     *
     * @param graphics the text-graphics target
     */
    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        // Draw the title in the bar
        var style = open ? titleOpenStyle() : titleNormalStyle();
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
                    graphics.drawString(0, row, " " + sep + " ", sepCellStyle());
                } else if (entry instanceof MenuItem mi) {
                    boolean selected = i == selectedIndex;
                    var itemStyle = selected ? itemSelectedStyle() : itemNormalStyle();
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

    /**
     * Handles arrow navigation, activation (Enter), and close (Escape/arrows).
     *
     * @param keyStroke the keystroke to handle
     * @return {@code true} if the keystroke was consumed
     */
    @Override
    public boolean handleKeyStroke(KeyStroke keyStroke) {
        if (!open) return false;
        switch (keyStroke.type()) {
            case ARROW_DOWN -> { moveSelection(1); return true; }
            case ARROW_UP -> { moveSelection(-1); return true; }
            case ENTER -> { activateSelected(); return true; }
            case ESCAPE -> { close(); return true; }
            case ARROW_LEFT, ARROW_RIGHT -> { close(); return true; }
            default -> { return false; }
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