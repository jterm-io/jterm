package io.jterm.demo;

import io.jterm.core.AnsiTerminal;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.screen.DefaultScreen;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.widget.Label;
import io.jterm.widget.Menu;
import io.jterm.widget.MenuBar;
import io.jterm.widget.Panel;
import io.jterm.layout.BorderLayout;
import io.jterm.layout.LinearLayout;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowImpl;
import io.jterm.window.WindowHint;

import java.io.IOException;
import java.util.List;

/**
 * Menu demo: menu bar with File, Edit, and Help menus.
 *
 * <p><b>Keyboard</b></p>
 * <ul>
 *   <li><b>Ctrl+F/E/H</b> or <b>Alt+F/E/H</b> — open File / Edit / Help menu</li>
 *   <li><b>Arrow Up/Down</b> — navigate menu items</li>
 *   <li><b>Arrow Left/Right</b> — switch between open menus</li>
 *   <li><b>Enter</b> — activate selected item</li>
 *   <li><b>Escape</b> — close menu</li>
 *   <li><b>q</b> — quit</li>
 * </ul>
 */
public class MenuDemo {

    /** Creates the menu demo (no state; run via {@link #main}). */
    public MenuDemo() {}

    /**
     * Runs the menu demo.
     *
     * @param args ignored command-line arguments
     * @throws IOException if the terminal cannot be initialized
     */
    public static void main(String[] args) throws IOException {
        var terminal = new AnsiTerminal();
        var screen = new DefaultScreen(terminal);
        var gui = new DefaultTextGUI(screen);
        gui.getScreen().startScreen();

        var window = new WindowImpl("Menu Demo");
        window.setHints(List.of(WindowHint.FULLSCREEN));
        var content = window.getContents();
        content.setLayoutManager(new BorderLayout());

        // Status label that menu actions update
        var statusLabel = new Label(" Press Ctrl+F/E/H or Alt+F/E/H to open a menu. q to quit. ");

        // Build the menu bar
        var menuBar = new MenuBar();

        // File menu
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("New", () -> statusLabel.setText(" Action: New File "));
        fileMenu.addMenuItem("Open", () -> statusLabel.setText(" Action: Open File "));
        fileMenu.addSeparator();
        fileMenu.addMenuItem("Save", () -> statusLabel.setText(" Action: Save "));
        fileMenu.addMenuItem("Save As…", () -> statusLabel.setText(" Action: Save As… "));
        fileMenu.addSeparator();
        fileMenu.addMenuItem("Quit", () -> {
            try { gui.close(); } catch (IOException e) { throw new RuntimeException(e); }
            System.exit(0);
        });
        menuBar.addMenu(fileMenu);

        // Edit menu
        var editMenu = new Menu("Edit");
        editMenu.addMenuItem("Undo", () -> statusLabel.setText(" Action: Undo "));
        editMenu.addMenuItem("Redo", () -> statusLabel.setText(" Action: Redo "));
        editMenu.addSeparator();
        editMenu.addMenuItem("Cut", () -> statusLabel.setText(" Action: Cut "));
        editMenu.addMenuItem("Copy", () -> statusLabel.setText(" Action: Copy "));
        editMenu.addMenuItem("Paste", () -> statusLabel.setText(" Action: Paste "));
        menuBar.addMenu(editMenu);

        // Help menu
        var helpMenu = new Menu("Help");
        helpMenu.addMenuItem("About", () -> statusLabel.setText(" Action: About JTerm v0.1 "));
        helpMenu.addMenuItem("Shortcuts", () -> statusLabel.setText(" Ctrl/Alt+letter opens menu, arrows navigate, Enter activates "));
        helpMenu.addSeparator();
        helpMenu.addMenuItem("Documentation", () -> statusLabel.setText(" Action: Open docs "));
        menuBar.addMenu(helpMenu);

        // Layout: menu bar at top, status label in center, help text at bottom
        content.addComponent(menuBar, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));
        content.addComponent(statusLabel, new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));

        var helpPanel = new Panel(new LinearLayout(LinearLayout.Direction.VERTICAL));
        helpPanel.addComponent(new Label(" ── Menu Demo ── "));
        helpPanel.addComponent(new Label(" Ctrl/Alt+F = File   Ctrl/Alt+E = Edit   Ctrl/Alt+H = Help "));
        helpPanel.addComponent(new Label(" ↑↓ navigate   ←→ switch menus   Enter activate "));
        helpPanel.addComponent(new Label(" Esc = close menu   q = quit "));
        content.addComponent(helpPanel, new BorderLayout.BorderLayoutData(BorderLayout.Region.SOUTH));

        gui.addWindow(window);
        gui.updateScreen();

        var running = true;

        try {
            while (running) {
                // Read ALL input in a single drain loop — don't let gui.processInput()
                // steal the first keystroke before the menu bar sees it.
                boolean anyInput = false;
                while (true) {
                    var ks = screen instanceof DefaultScreen ds
                            ? ds.getTerminal().pollInput().orElse(null)
                            : null;
                    if (ks == null) break;
                    anyInput = true;

                    // Check for quit keys first
                    if (ks.type() == KeyType.CHARACTER) {
                        char ch = ks.character();
                        if (ch == 'q' || ch == 'Q') {
                            running = false;
                            break;
                        }
                        if (ks.ctrl() && (ch == 'C' || ch == 'c')) {
                            running = false;
                            break;
                        }
                    }

                    // Route menu keystrokes to the menu bar
                    boolean menuHandled = false;
                    if (menuBar.hasOpenMenu()
                            || (ks.type() == KeyType.CHARACTER && (ks.ctrl() || ks.alt()))) {
                        menuBar.handleKeyStroke(ks);
                        menuHandled = true;
                    }

                    if (!menuHandled) {
                        // Don't pass Escape to gui — it quits. Let MenuBar handle it.
                        if (ks.type() == KeyType.ESCAPE && !menuBar.hasOpenMenu()) {
                            running = false;
                            break;
                        }
                        gui.processInput(ks);
                    }
                }
                if (anyInput) {
                    gui.requestRefresh();
                    gui.updateScreen();
                }
                Thread.sleep(16);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            gui.close();
        }
    }
}