package io.jterm.demo;

import io.jterm.core.AnsiTerminal;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.layout.BorderLayout;
import io.jterm.screen.DefaultScreen;
import io.jterm.style.AnsiColor;
import io.jterm.widget.Label;
import io.jterm.widget.Panel;
import io.jterm.widget.TextArea;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowImpl;
import io.jterm.window.WindowHint;

import java.io.IOException;
import java.util.List;

/**
 * Interactive text editor demo using the TextArea widget.
 *
 * <h3>Keyboard</h3>
 * <ul>
 *   <li>Type to insert text</li>
 *   <li><b>Enter</b> — new line</li>
 *   <li><b>Backspace</b> — delete (joins lines at start)</li>
 *   <li><b>Delete</b> — delete forward (joins lines at end)</li>
 *   <li><b>Arrow keys</b> — move cursor</li>
 *   <li><b>Home / End</b> — line start / end</li>
 *   <li><b>Page Up / Page Down</b> — scroll by viewport</li>
 *   <li><b>Ctrl+A</b> — line start (emacs)</li>
 *   <li><b>Ctrl+E</b> — line end (emacs)</li>
 *   <li><b>Ctrl+K</b> — kill to end of line</li>
 *   <li><b>Ctrl+F/B</b> — forward / backward (emacs)</li>
 *   <li><b>Ctrl+N/P</b> — down / up (emacs)</li>
 *   <li><b>Ctrl+D</b> — delete char forward (emacs)</li>
 *   <li><b>Ctrl+S</b> — save (prints to console)</li>
 *   <li><b>q</b> or <b>Ctrl+C</b> — quit</li>
 * </ul>
 */
public class TextEditorDemo {

    public static void main(String[] args) throws IOException {
        var terminal = new AnsiTerminal();
        var screen = new DefaultScreen(terminal);
        var gui = new DefaultTextGUI(screen);
        gui.getScreen().startScreen();

        var window = new WindowImpl("JTerm Text Editor");
        window.setHints(List.of(WindowHint.FULLSCREEN));
        var content = window.getContents();
        content.setLayoutManager(new BorderLayout());

        // Header
        var header = new Label(" ── JTerm Text Editor — Type to edit, Ctrl+S to save, q to quit ── ",
                AnsiColor.BRIGHT_CYAN, AnsiColor.DEFAULT);

        // Main editor area
        var editor = new TextArea(sampleText(), 80, 20);

        // Footer / status bar
        var statusBar = new Label(" Ln 1, Col 1  |  Lines: " + editor.getLineCount() + "  |  INSERT ",
                AnsiColor.BRIGHT_YELLOW, AnsiColor.DEFAULT);

        content.addComponent(header, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));
        content.addComponent(editor, new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));
        content.addComponent(statusBar, new BorderLayout.BorderLayoutData(BorderLayout.Region.SOUTH));

        gui.addWindow(window);
        gui.updateScreen();

        var running = true;
        try {
            while (running) {
                while (true) {
                    var ks = screen instanceof DefaultScreen ds
                            ? ds.getTerminal().pollInput().orElse(null)
                            : null;
                    if (ks == null) break;

                    // Check quit keys
                    if (ks.type() == KeyType.CHARACTER && !ks.ctrl() && !ks.alt()) {
                        char ch = ks.character();
                        if (ch == 'q' || ch == 'Q') {
                            running = false;
                            break;
                        }
                    }
                    if (ks.type() == KeyType.CHARACTER && ks.ctrl() && ks.character() == 'C') {
                        running = false;
                        break;
                    }
                    if (ks.type() == KeyType.CHARACTER && ks.ctrl() && (ks.character() == 'S' || ks.character() == 's')) {
                        // Save — print to console
                        System.out.println("=== SAVED TEXT ===");
                        System.out.println(editor.getText());
                        System.out.println("=== END ===");
                        statusBar.setText(" Saved! (" + editor.getLineCount() + " lines) — Ln " +
                                getCursorRow(editor) + ", Col " + getCursorCol(editor) +
                                "  |  Lines: " + editor.getLineCount() + "  |  INSERT ");
                        gui.requestRefresh();
                        continue;
                    }
                    if (ks.type() == KeyType.ESCAPE) {
                        running = false;
                        break;
                    }

                    // Route to TextArea
                    editor.handleKeyStroke(ks);

                    // Update status bar
                    statusBar.setText(" Ln " + (getCursorRow(editor) + 1) + ", Col " +
                            (getCursorCol(editor) + 1) + "  |  Lines: " + editor.getLineCount() +
                            "  |  INSERT ");
                    gui.requestRefresh();
                }
                gui.updateScreen();
                Thread.sleep(16);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            gui.close();
        }
    }

    private static String sampleText() {
        return """
            Welcome to JTerm Text Editor
            ================================

            This is a fully functional text editor built with the
            JTerm TTY UI toolkit. It demonstrates the TextArea widget
            with multi-line editing, scrolling, and emacs key bindings.

            Features:
              - Multi-line text editing with Enter for newlines
              - Horizontal and vertical scrolling
              - Arrow keys for cursor movement
              - Home/End for line navigation
              - Page Up/Down for fast scrolling
              - Emacs bindings: Ctrl+A/E/K/F/B/P/N
              - Backspace joins lines at start
              - Delete joins lines at end
              - Ctrl+S to "save" (prints to console)

            Try editing this text! Use arrow keys to move around,
            type to insert, and backspace to delete.

            The quick brown fox jumps over the lazy dog.
            Lorem ipsum dolor sit amet, consectetur adipiscing elit.
            Sed do eiusmod tempor incididunt ut labore et dolore.

            -- End of sample --
            """;
    }

    private static int getCursorRow(TextArea ta) {
        try {
            var f = TextArea.class.getDeclaredField("cursorRow");
            f.setAccessible(true);
            return f.getInt(ta);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getCursorCol(TextArea ta) {
        try {
            var f = TextArea.class.getDeclaredField("cursorCol");
            f.setAccessible(true);
            return f.getInt(ta);
        } catch (Exception e) {
            return 0;
        }
    }
}