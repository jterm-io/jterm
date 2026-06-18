package io.jterm.demo;

import io.jterm.core.AnsiTerminal;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.layout.BorderLayout;
import io.jterm.layout.LinearLayout;
import io.jterm.screen.DefaultScreen;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.style.Theme;
import io.jterm.style.ThemeManager;
import io.jterm.widget.Button;
import io.jterm.widget.Label;
import io.jterm.widget.ListBox;
import io.jterm.widget.Panel;
import io.jterm.widget.ProgressBar;
import io.jterm.widget.Table;
import io.jterm.widget.TextArea;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowImpl;
import io.jterm.window.WindowHint;

import java.io.IOException;
import java.util.List;

/**
 * Theme demo: shows all 4 built-in color themes with live switching.
 *
 * <h3>Keyboard</h3>
 * <ul>
 *   <li><b>t</b> — cycle to next theme</li>
 *   <li><b>1</b> — Dark (white on black)</li>
 *   <li><b>2</b> — Yellow on Blue</li>
 *   <li><b>3</b> — Green on Black</li>
 *   <li><b>4</b> — White on Green</li>
 *   <li><b>q</b> — quit</li>
 * </ul>
 */
public class ThemeDemo {

    public static void main(String[] args) throws IOException {
        var terminal = new AnsiTerminal();
        var screen = new DefaultScreen(terminal);
        var gui = new DefaultTextGUI(screen);
        gui.getScreen().startScreen();

        var window = new WindowImpl("Theme Demo — Press t to cycle, 1-4 to select, q to quit");
        window.setHints(List.of(WindowHint.FULLSCREEN));
        var content = window.getContents();
        content.setLayoutManager(new BorderLayout());

        // Header
        var header = new Label("", AnsiColor.BRIGHT_CYAN, AnsiColor.DEFAULT);

        // Left panel: ListBox + ProgressBar
        var leftPanel = new Panel(new LinearLayout(LinearLayout.Direction.VERTICAL));
        leftPanel.addComponent(new Label("List Items:"));
        var listBox = new ListBox<String>();
        listBox.addItem("Apple");
        listBox.addItem("Banana");
        listBox.addItem("Cherry");
        listBox.addItem("Date");
        listBox.addItem("Elderberry");
        leftPanel.addComponent(listBox);
        leftPanel.addComponent(new Label("Progress:"));
        var progressBar = new ProgressBar(100);
        progressBar.setValue(65);
        leftPanel.addComponent(progressBar);

        // Right panel: Table + TextArea
        var rightPanel = new Panel(new LinearLayout(LinearLayout.Direction.VERTICAL));
        rightPanel.addComponent(new Label("Table:"));
        var table = new Table("Name", "Value", "Status");
        table.addRow("AAPL", "195.32", "Buy");
        table.addRow("NVDA", "845.10", "Hold");
        table.addRow("MSFT", "412.78", "Buy");
        table.addRow("GOOG", "178.90", "Sell");
        rightPanel.addComponent(table);
        rightPanel.addComponent(new Label("Text Editor:"));
        var textArea = new TextArea("Hello from\nJTerm Theme Demo\n\nEdit this text!", 30, 5);
        rightPanel.addComponent(textArea);

        // Bottom: buttons
        var buttonPanel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        var btn1 = new Button("Theme 1");
        var btn2 = new Button("Theme 2");
        var btn3 = new Button("Theme 3");
        buttonPanel.addComponent(btn1);
        buttonPanel.addComponent(btn2);
        buttonPanel.addComponent(btn3);

        content.addComponent(header, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));

        var split = new Panel(new BorderLayout());
        split.addComponent(leftPanel, new BorderLayout.BorderLayoutData(BorderLayout.Region.WEST));
        split.addComponent(rightPanel, new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));
        content.addComponent(split, new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));
        content.addComponent(buttonPanel, new BorderLayout.BorderLayoutData(BorderLayout.Region.SOUTH));

        // Button actions
        btn1.addListener(() -> { ThemeManager.setActive(Theme.DARK); gui.requestRefresh(); });
        btn2.addListener(() -> { ThemeManager.setActive(Theme.YELLOW_ON_BLUE); gui.requestRefresh(); });
        btn3.addListener(() -> { ThemeManager.setActive(Theme.GREEN_ON_BLACK); gui.requestRefresh(); });

        // Theme change listener updates header
        ThemeManager.addListener(theme -> {
            header.setText(" ── Theme: " + theme.name() + " ── Press t to cycle, 1-4 to select, q to quit ");
            gui.requestRefresh();
        });
        // Set initial header
        header.setText(" ── Theme: " + ThemeManager.active().name() + " ── Press t to cycle, 1-4 to select, q to quit ");

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

                    if (ks.type() == KeyType.CHARACTER && !ks.ctrl()) {
                        char ch = ks.character();
                        if (ch == 'q' || ch == 'Q') {
                            running = false;
                            break;
                        }
                        switch (ch) {
                            case 't', 'T' -> {
                                var next = ThemeManager.cycle();
                                gui.requestRefresh();
                            }
                            case '1' -> { ThemeManager.setActive(Theme.DARK); gui.requestRefresh(); }
                            case '2' -> { ThemeManager.setActive(Theme.YELLOW_ON_BLUE); gui.requestRefresh(); }
                            case '3' -> { ThemeManager.setActive(Theme.GREEN_ON_BLACK); gui.requestRefresh(); }
                            case '4' -> { ThemeManager.setActive(Theme.WHITE_ON_GREEN); gui.requestRefresh(); }
                            default -> {
                                // Route to focused component for normal typing
                                gui.processInput(ks);
                            }
                        }
                        continue;
                    }
                    // Route other keys (arrows, enter, etc.) to focused component
                    gui.processInput(ks);
                }
                gui.updateScreen();
                Thread.sleep(16);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            ThemeManager.setActive(Theme.DARK); // reset for other demos
            gui.close();
        }
    }
}