package io.jterm.demo;

import io.jterm.core.AnsiTerminal;
import io.jterm.screen.DefaultScreen;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.widget.Borders;
import io.jterm.widget.Label;
import io.jterm.widget.Panel;
import io.jterm.layout.BorderLayout;
import io.jterm.layout.LinearLayout;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowImpl;
import io.jterm.window.WindowHint;

import java.io.IOException;
import java.util.List;

/**
 * Borders demo: shows all five border styles side by side, plus a titled border.
 * Press q or Escape to quit.
 */
public class BordersDemo {
    /** Prevents instantiation; run the demo via {@link #main(String[])} instead. */
    private BordersDemo() {}

    /**
     * Runs the borders demo in fullscreen mode.
     *
     * @param args command-line arguments (ignored)
     * @throws IOException if the terminal screen cannot be initialized
     */
    public static void main(String[] args) throws IOException {
        var terminal = new AnsiTerminal();
        var screen = new DefaultScreen(terminal);
        var gui = new DefaultTextGUI(screen);
        gui.getScreen().startScreen();

        var window = new WindowImpl("JTerm Borders Demo");
        window.setHints(List.of(WindowHint.FULLSCREEN));
        var content = window.getContents();
        content.setLayoutManager(new BorderLayout());

        // North: title bar
        var titleStyle = new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLUE, SGR.BOLD);
        content.addComponent(new Label(" JTerm Border Styles ", titleStyle),
                new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));

        // Center: grid of bordered panels showing each style
        var center = new Panel(new LinearLayout(LinearLayout.Direction.VERTICAL));
        center.addComponent(makeBorderDemo("Single Line", Borders.singleLine(makeContent("Single-line border using ─│┌┐└┘"))));
        center.addComponent(makeBorderDemo("Double Line", Borders.doubleLine(makeContent("Double-line border using ═║╔╗╚╝"))));
        center.addComponent(makeBorderDemo("Rounded", Borders.rounded(makeContent("Rounded corners using ╭╮╰╯"))));
        center.addComponent(makeBorderDemo("Bevel", Borders.bevel(makeContent("Bevel-style mixed corners"))));
        center.addComponent(makeBorderDemo("Empty", Borders.empty(makeContent("Empty border — no visible frame"))));
        content.addComponent(center, new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));

        // South: titled border demo + quit hint
        var south = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        var titled = Borders.titled(Borders.singleLine(makeContent("A titled border with a label")), " Title ");
        south.addComponent(titled);
        south.addComponent(new Label("  q/Esc to quit "));
        content.addComponent(south, new BorderLayout.BorderLayoutData(BorderLayout.Region.SOUTH));

        gui.addWindow(window);
        gui.updateScreen();

        try {
            while (gui.processInput()) {
                while (true) {
                    var ks = screen instanceof DefaultScreen ds
                            ? ds.getTerminal().pollInput().orElse(null)
                            : null;
                    if (ks == null) break;
                    gui.processInput(ks);
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

    /** Wraps a bordered component with a small label above it. */
    private static Panel makeBorderDemo(String label, io.jterm.widget.Component bordered) {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.VERTICAL));
        panel.addComponent(new Label(" " + label));
        panel.addComponent(bordered);
        return panel;
    }

    /** Creates a small labeled content panel to sit inside a border. */
    private static Panel makeContent(String text) {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.VERTICAL));
        panel.addComponent(new Label(" " + text + " "));
        return panel;
    }
}