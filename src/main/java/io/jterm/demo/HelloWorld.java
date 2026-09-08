package io.jterm.demo;

import io.jterm.core.AnsiTerminal;
import io.jterm.screen.DefaultScreen;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.widget.Button;
import io.jterm.widget.Label;
import io.jterm.widget.Panel;
import io.jterm.layout.BorderLayout;
import io.jterm.layout.LinearLayout;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowImpl;
import io.jterm.window.WindowHint;

import java.io.IOException;
import java.util.List;

/** HelloWorld demo: styled label, counter button, quit button. */
public class HelloWorld {

    /** Creates the hello world demo (no state; run via {@link #main}). */
    public HelloWorld() {}

    /**
     * Runs the hello world demo.
     *
     * @param args ignored command-line arguments
     * @throws IOException if the terminal cannot be initialized
     */
    public static void main(String[] args) throws IOException {
        var terminal = new AnsiTerminal();
        var screen = new DefaultScreen(terminal);
        var gui = new DefaultTextGUI(screen);
        gui.getScreen().startScreen();

        var window = new WindowImpl("Hello World");
        window.setHints(List.of(WindowHint.FIT_TERMINAL_WINDOW));
        var content = window.getContents();
        content.setLayoutManager(new BorderLayout());

        // North: styled title label
        var headerStyle = new TextCell(' ', AnsiColor.YELLOW, AnsiColor.BLUE, SGR.BOLD);
        var header = new Label("  JTerm Hello World  ", headerStyle);
        content.addComponent(header, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));

        // Center: button + counter label
        var centerPanel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        var counterLabel = new Label("Count: 0", AnsiColor.GREEN, AnsiColor.DEFAULT);
        var counter = new int[1];
        var pressButton = new Button("Press Me");
        pressButton.addListener(() -> {
            counter[0]++;
            counterLabel.setText("Count: " + counter[0]);
            try {
                gui.updateScreen();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        centerPanel.addComponent(pressButton);
        centerPanel.addComponent(new Label("  "));
        centerPanel.addComponent(counterLabel);
        content.addComponent(centerPanel, new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));

        // South: quit button
        var quitButton = new Button("Quit");
        quitButton.addListener(() -> {
            try {
                gui.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.exit(0);
        });
        content.addComponent(quitButton, new BorderLayout.BorderLayoutData(BorderLayout.Region.SOUTH));

        gui.addWindow(window);
        gui.updateScreen();

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
