package io.jterm.demo;

import io.jterm.core.AnsiTerminal;
import io.jterm.screen.DefaultScreen;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.widget.Button;
import io.jterm.widget.Label;
import io.jterm.widget.ListBox;
import io.jterm.widget.Panel;
import io.jterm.widget.ProgressBar;
import io.jterm.widget.Separator;
import io.jterm.widget.Table;
import io.jterm.layout.BorderLayout;
import io.jterm.layout.LinearLayout;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowImpl;
import io.jterm.window.WindowHint;

import java.io.IOException;
import java.util.List;

/** Dashboard demo: list, table, progress bar, focus navigation. */
public class DashboardDemo {
    public static void main(String[] args) throws IOException {
        var terminal = new AnsiTerminal();
        var screen = new DefaultScreen(terminal);
        var gui = new DefaultTextGUI(screen);
        gui.getScreen().startScreen();

        var window = new WindowImpl("Dashboard");
        window.setHints(List.of(WindowHint.FULLSCREEN));
        var content = window.getContents();
        content.setLayoutManager(new BorderLayout());

        // North: title
        var title = new Label(" JTerm Dashboard Demo ", new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLUE, SGR.BOLD));
        content.addComponent(title, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));

        // Center: split left/right
        var center = new Panel(new BorderLayout());

        // Left: list box (30%)
        var listBox = new ListBox<String>();
        for (int i = 1; i <= 20; i++) {
            listBox.addItem("Item " + i);
        }
        listBox.addSelectionListener(() -> {
            // could update status
        });
        center.addComponent(listBox, new BorderLayout.BorderLayoutData(BorderLayout.Region.WEST));

        // Right: table (70%)
        var table = new Table("Metric", "Value", "Status");
        table.addRow("CPU", "12%", "OK");
        table.addRow("Memory", "45%", "OK");
        table.addRow("Disk", "78%", "WARN");
        table.addRow("Network", "1.2 Gbps", "OK");
        table.addRow("Load", "0.45", "OK");
        table.addRow("Temp", "62 C", "OK");
        center.addComponent(table, new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));

        content.addComponent(center, new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));

        // South: status bar
        var status = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        var progressBar = new ProgressBar(100);
        var statusLabel = new Label(" Use arrows to navigate, Tab to switch panels, Ctrl+C or q to quit ");
        status.addComponent(progressBar);
        status.addComponent(statusLabel);
        content.addComponent(status, new BorderLayout.BorderLayoutData(BorderLayout.Region.SOUTH));

        gui.addWindow(window);
        gui.updateScreen();

        // Real-time progress updates in a virtual thread
        Thread.startVirtualThread(() -> {
            int value = 0;
            boolean increasing = true;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
                value += increasing ? 5 : -5;
                if (value >= 100) { value = 100; increasing = false; }
                if (value <= 0) { value = 0; increasing = true; }
                final int v = value;
                progressBar.setValue(v);
                try {
                    gui.updateScreen();
                } catch (IOException e) {
                    break;
                }
            }
        });

        try {
            while (gui.processInput()) {
                gui.updateScreen();
                Thread.sleep(20);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            gui.close();
        }
    }
}
