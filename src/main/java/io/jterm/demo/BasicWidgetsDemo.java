package io.jterm.demo;

import io.jterm.core.AnsiTerminal;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.screen.DefaultScreen;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.widget.Button;
import io.jterm.widget.CheckBox;
import io.jterm.widget.DataGrid;
import io.jterm.widget.Borders;
import io.jterm.widget.Label;
import io.jterm.widget.ListBox;
import io.jterm.widget.Panel;
import io.jterm.widget.ProgressBar;
import io.jterm.widget.RadioButton;
import io.jterm.widget.RadioGroup;
import io.jterm.widget.Separator;
import io.jterm.widget.Table;
import io.jterm.layout.BorderLayout;
import io.jterm.layout.LinearLayout;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowImpl;
import io.jterm.window.WindowHint;
import io.jterm.style.CellStyle;
import io.jterm.widget.model.DefaultGridModel;
import io.jterm.widget.model.GridColumn;

import java.io.IOException;
import java.util.List;

/** Basic widgets demo: CheckBox, RadioButton, Separator, ProgressBar, ListBox, Table, DataGrid. */
public class BasicWidgetsDemo {

    /** Prevents instantiation; run the demo via {@link #main(String[])} instead. */
    private BasicWidgetsDemo() {}

    /** Sample row type for the DataGrid demo. */
    record StockRow(String ticker, String name, double price, double change, int volume) {}

    /**
     * Runs the basic widgets demo.
     *
     * @param args ignored
     * @throws IOException if the terminal cannot be initialized
     */
    public static void main(String[] args) throws IOException {
        var terminal = new AnsiTerminal();
        var screen = new DefaultScreen(terminal);
        var gui = new DefaultTextGUI(screen);
        gui.getScreen().startScreen();

        var window = new WindowImpl("Basic Widgets");
        window.setHints(List.of(WindowHint.FULLSCREEN));
        var content = window.getContents();
        content.setLayoutManager(new BorderLayout());

        // North: title bar
        var title = new Label(" Basic Widgets Demo ",
                new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLUE, SGR.BOLD));
        content.addComponent(title, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));

        // Center: two-column layout
        var center = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));

        // Left column
        var left = new Panel(new LinearLayout(LinearLayout.Direction.VERTICAL));
        left.addComponent(new Label("CheckBox:"));
        var checkBox = new CheckBox("Enable turbo mode");
        checkBox.addListener(() -> System.out.println("Turbo: " + checkBox.isSelected()));
        left.addComponent(checkBox);
        left.addComponent(new Separator(false));

        left.addComponent(new Label("RadioGroup:"));
        var radioGroup = new RadioGroup();
        var optionA = new RadioButton("Option A");
        var optionB = new RadioButton("Option B");
        var optionC = new RadioButton("Option C");
        radioGroup.add(optionA);
        radioGroup.add(optionB);
        radioGroup.add(optionC);
        left.addComponent(optionA);
        left.addComponent(optionB);
        left.addComponent(optionC);
        left.addComponent(new Separator(false));

        left.addComponent(new Label("Progress:"));
        var progressBar = new ProgressBar(100);
        progressBar.setValue(67);
        left.addComponent(progressBar);

        // Right column
        var right = new Panel(new LinearLayout(LinearLayout.Direction.VERTICAL));
        right.addComponent(new Label("ListBox:"));
        var listBox = new ListBox<String>();
        for (int i = 1; i <= 15; i++) {
            listBox.addItem("Item " + i);
        }
        right.addComponent(listBox);
        right.addComponent(new Label("Table:"));
        var table = new Table("Name", "Score", "Rank");
        table.addRow("Alice", "980", "1");
        table.addRow("Bob", "850", "2");
        table.addRow("Carol", "720", "3");
        table.addRow("Dave", "690", "4");
        right.addComponent(table);
        right.addComponent(new Separator(false));

        right.addComponent(new Label("DataGrid:"));
        var dataGrid = createStockGrid();
        var gridBorder = Borders.titled(Borders.singleLine(dataGrid), " Stock Quotes ");
        right.addComponent(gridBorder);

        center.addComponent(left);
        center.addComponent(new Separator(true));
        center.addComponent(right);

        content.addComponent(center, new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));

        // South: status + quit button
        var south = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        var status = new Label(" Tab/arrow keys to navigate, Ctrl+C or q to quit ");
        var quit = new Button("Quit");
        quit.addListener(() -> gui.stopRunning());
        south.addComponent(status);
        south.addComponent(quit);
        content.addComponent(south, new BorderLayout.BorderLayoutData(BorderLayout.Region.SOUTH));

        gui.addWindow(window);
        gui.updateScreen();

        try {
            while (gui.isRunning()) {
                var ks = screen.getTerminal().pollInput().orElse(null);
                if (ks != null) {
                    if (ks.type() == KeyType.CHARACTER && (ks.character() == 'q' || ks.character() == 'Q')) {
                        break;
                    }
                    gui.processInput(ks);
                }
                gui.updateScreen();
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            gui.close();
        }
    }

    /**
     * Creates a DataGrid showing a sample stock portfolio with per-column
     * formatting and a styler that colors the change column green/red.
     */
    private static DataGrid<StockRow> createStockGrid() {
        var columns = List.of(
                GridColumn.text("Tk", StockRow::ticker),
                GridColumn.text("Name", StockRow::name),
                GridColumn.doubleCol("Price", "$%,.2f", StockRow::price),
                GridColumn.doubleCol("Chg", "%+.2f", StockRow::change)
                        .withStyler(v -> ((Double) v) >= 0 ? CellStyle.GREEN : CellStyle.RED),
                GridColumn.intCol("Vol", StockRow::volume)
        );

        var model = new DefaultGridModel<StockRow>();
        model.addRows(List.of(
                new StockRow("AAPL", "Apple Inc.",       189.45,  +2.35,  52_000_000),
                new StockRow("MSFT", "Microsoft Corp.",  412.78,  -1.12,  23_400_000),
                new StockRow("GOOG", "Alphabet Inc.",    172.34,  +0.89,  18_700_000),
                new StockRow("AMZN", "Amazon.com Inc.",  178.22,  -3.45,  41_200_000),
                new StockRow("TSLA", "Tesla Inc.",      248.91,  +5.67,  89_300_000),
                new StockRow("NVDA", "NVIDIA Corp.",     875.30,  +12.44, 67_800_000),
                new StockRow("META", "Meta Platforms",  502.66,  -2.78,  15_900_000),
                new StockRow("NFLX", "Netflix Inc.",    612.15,  +1.34,   8_200_000),
                new StockRow("JPM",  "JPMorgan Chase",  198.77,  -0.45,  10_100_000),
                new StockRow("V",    "Visa Inc.",       276.54,  +0.67,   7_600_000)
        ));

        var grid = new DataGrid<>(columns, model);
        grid.addSelectionListener(() -> {
            var selected = grid.getSelectedItem();
            if (selected != null) {
                System.out.println("Selected: " + selected.ticker() + " @ $" + selected.price());
            }
        });
        return grid;
    }
}
