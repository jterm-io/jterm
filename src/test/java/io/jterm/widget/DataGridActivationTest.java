package io.jterm.widget;

import org.junit.jupiter.api.Test;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.screen.DefaultScreen;
import io.jterm.widget.DataGrid;
import io.jterm.widget.model.DefaultGridModel;
import io.jterm.widget.model.GridColumn;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduction for the BBS portfolio-list bug: DataGrid fires
 * selectionChanged on arrow-key moves, so a screen hooking
 * addSelectionListener as an "open on Enter" hook opened the detail view
 * on every down-arrow press. Enter must be the ONLY trigger.
 */
class DataGridActivationTest {

    private DataGrid<String> gridWithRows() {
        var model = new DefaultGridModel<String>();
        model.addRows(List.of("alpha", "beta", "gamma"));
        return new DataGrid<>(List.of(
                GridColumn.<String>text("Name", s -> s)), model);
    }

    @Test
    void arrowKeysDoNotFireActivation() throws Exception {
        var terminal = new MockTerminal(new TerminalSize(80, 24));
        var gui = new io.jterm.window.DefaultTextGUI(new DefaultScreen(terminal));

        var grid = gridWithRows();
        var activations = new AtomicInteger();
        var selectionChanges = new AtomicInteger();
        grid.addActivationListener(activations::incrementAndGet);
        grid.addSelectionListener(selectionChanges::incrementAndGet);

        grid.setFocused(true);
        grid.handleKeyStroke(new io.jterm.core.input.KeyStroke(io.jterm.core.input.KeyType.ARROW_DOWN));
        grid.handleKeyStroke(new io.jterm.core.input.KeyStroke(io.jterm.core.input.KeyType.ARROW_DOWN));
        grid.handleKeyStroke(new io.jterm.core.input.KeyStroke(io.jterm.core.input.KeyType.ARROW_UP));

        assertEquals(0, activations.get(),
                "arrow keys move the selection but must NOT fire activation");
        assertEquals(3, selectionChanges.get(),
                "selection-changed still fires on moves (its existing contract)");
    }

    @Test
    void enterFiresActivation() throws Exception {
        var terminal = new MockTerminal(new TerminalSize(80, 24));
        new DefaultScreen(terminal);

        var grid = gridWithRows();
        var activations = new AtomicInteger();
        grid.addActivationListener(activations::incrementAndGet);

        grid.handleKeyStroke(new io.jterm.core.input.KeyStroke(io.jterm.core.input.KeyType.ENTER));
        assertEquals(1, activations.get(), "Enter fires activation");
    }

    @Test
    void selectionChangeStillSeparateFromActivation() throws Exception {
        var terminal = new MockTerminal(new TerminalSize(80, 24));
        new DefaultScreen(terminal);

        var grid = gridWithRows();
        var activations = new AtomicInteger();
        var selectionChanges = new AtomicInteger();
        grid.addActivationListener(activations::incrementAndGet);
        grid.addSelectionListener(selectionChanges::incrementAndGet);

        grid.setSelectedRow(1);   // programmatic move: selection only
        grid.handleKeyStroke(new io.jterm.core.input.KeyStroke(io.jterm.core.input.KeyType.ENTER)); // activate

        assertEquals(1, activations.get(), "exactly one activation (the Enter)");
        assertEquals(1, selectionChanges.get(), "exactly one selection change (the move)");
    }
}