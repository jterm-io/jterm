package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal menu bar containing {@link Menu}s. Sits at the top of a window.
 *
 * <h3>Keyboard interaction</h3>
 * <ul>
 *   <li><b>Ctrl+mnemonic</b> or <b>Alt+mnemonic</b> — open the menu whose title starts with that char</li>
 *   <li><b>Arrow Left/Right</b> — switch between open menus</li>
 *   <li><b>Escape</b> — close the active menu</li>
 *   <li><b>Arrow Down</b> — move focus into the open dropdown</li>
 *   <li><b>Enter</b> — activate the focused menu item</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * var bar = new MenuBar();
 * var fileMenu = new Menu("File");
 * fileMenu.addMenuItem("Open", () -> openFile());
 * fileMenu.addMenuItem("Quit", () -> System.exit(0));
 * bar.addMenu(fileMenu);
 * }</pre>
 */
public class MenuBar extends AbstractComponent {

    private final List<Menu> menus = new ArrayList<>();
    private int activeMenuIndex = -1;  // which menu is currently open

    private TextCell barBgStyle() {
        var t = ThemeManager.active();
        return new TextCell(' ', t.foreground(), t.background());
    }

    @Override
    protected TerminalSize calculatePreferredSize() {
        int width = 0;
        for (var m : menus) width += m.getPreferredSize().columns();
        // When a menu is open, we need height for the bar + the dropdown
        int height = 1;
        for (var m : menus) {
            if (m.isOpen()) {
                height = Math.max(height, 1 + m.dropdownHeight());
            }
        }
        return new TerminalSize(Math.max(1, width), height);
    }

    public void addMenu(Menu menu) {
        menus.add(menu);
        menu.setCloseCallback(() -> {
            activeMenuIndex = -1;
            invalidate();
        });
        invalidate();
    }

    public List<Menu> getMenus() { return new ArrayList<>(menus); }

    public int getActiveMenuIndex() { return activeMenuIndex; }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        // Fill bar background
        graphics.fillRectangle(0, 0, size.columns(), 1, barBgStyle());

        int col = 0;
        for (int i = 0; i < menus.size(); i++) {
            var menu = menus.get(i);
            int menuWidth = menu.getPreferredSize().columns();

            if (menu.isOpen()) {
                // Draw the title (highlighted) in the bar
                menu.setBounds(new TerminalPosition(col, 0), new TerminalSize(menuWidth, 1));
                var titleSub = io.jterm.graphics.TextGraphicsExtensions.subGraphics(graphics,
                        new TerminalPosition(col, 0), new TerminalSize(menuWidth, 1));
                // Draw just the title bar via menu.draw (which draws title at row 0)
                menu.draw(titleSub);

                // Draw the dropdown directly to the MenuBar's graphics at absolute
                // coordinates. We can't use a sub-clipped graphics because the MenuBar is
                // only 1 row tall — the dropdown would be clipped. Instead, draw each
                // dropdown cell directly to the parent graphics at (col, 1 + i).
                int dropW = menu.dropdownWidth();
                int dropH = menu.dropdownHeight();
                var entries = menu.getEntries();
                for (int r = 0; r < dropH; r++) {
                    int absY = 1 + r;
                    var entry = entries.get(r);
                    var theme = ThemeManager.active();
                    if (entry instanceof MenuSeparator) {
                        String sep = "─".repeat(Math.max(0, dropW - 2));
                        graphics.drawString(col, absY, " " + sep + " ",
                                new TextCell('─', theme.border(), theme.background()));
                    } else if (entry instanceof MenuItem mi) {
                        boolean selected = r == menu.getSelectedIndex();
                        var style = selected
                                ? new TextCell(' ', theme.selectionFg(), theme.selectionBg())
                                : new TextCell(' ', theme.foreground(), theme.background());
                        String text = " " + mi.getLabel() + " ";
                        int pad = dropW - text.length();
                        if (pad > 0) text += " ".repeat(pad);
                        if (text.length() > dropW) text = text.substring(0, dropW);
                        graphics.drawString(col, absY, text, style);
                    }
                }
            } else {
                // Closed menu — just draw the title
                menu.setBounds(new TerminalPosition(col, 0), new TerminalSize(menuWidth, 1));
                var sub = io.jterm.graphics.TextGraphicsExtensions.subGraphics(graphics,
                        new TerminalPosition(col, 0), new TerminalSize(menuWidth, 1));
                menu.draw(sub);
            }
            col += menuWidth;
        }
    }

    @Override
    public boolean handleKeyStroke(KeyStroke keyStroke) {
        // Ctrl+char or Alt+char → open the menu whose mnemonic matches
        if (keyStroke.type() == KeyType.CHARACTER && (keyStroke.ctrl() || keyStroke.alt())) {
            char ch = Character.toLowerCase(keyStroke.character());
            for (int i = 0; i < menus.size(); i++) {
                if (Character.toLowerCase(menus.get(i).getMnemonic()) == ch) {
                    openMenu(i);
                    return true;
                }
            }
            return false;
        }

        if (activeMenuIndex >= 0) {
            // A menu is open — route input to it, or handle left/right switching
            var active = menus.get(activeMenuIndex);
            switch (keyStroke.type()) {
                case ARROW_LEFT -> {
                    active.setOpen(false);
                    int prev = activeMenuIndex - 1;
                    if (prev < 0) prev = menus.size() - 1;
                    openMenu(prev);
                    return true;
                }
                case ARROW_RIGHT -> {
                    active.setOpen(false);
                    int next = (activeMenuIndex + 1) % menus.size();
                    openMenu(next);
                    return true;
                }
                default -> {
                    return active.handleKeyStroke(keyStroke);
                }
            }
        }
        return false;
    }

    private void openMenu(int index) {
        activeMenuIndex = index;
        for (int i = 0; i < menus.size(); i++) {
            menus.get(i).setOpen(i == index);
        }
        invalidate();
    }

    public void closeAll() {
        for (var m : menus) m.setOpen(false);
        activeMenuIndex = -1;
        invalidate();
    }

    public boolean hasOpenMenu() {
        return activeMenuIndex >= 0;
    }
}