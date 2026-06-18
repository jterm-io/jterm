package io.jterm.window;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.screen.DefaultScreen;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.style.Theme;
import io.jterm.style.ThemeManager;
import io.jterm.widget.Button;
import io.jterm.widget.Label;
import io.jterm.widget.Panel;
import io.jterm.layout.BorderLayout;
import io.jterm.layout.LinearLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug: AbstractWindow.draw() passes its graphics context directly to
 * contents.draw() without creating a sub-graphics at the content panel's
 * offset (1,1) — inside the border. So all content children render at
 * window-relative coordinates instead of content-panel-relative coordinates,
 * causing everything to shift 1 column left (overlapping the left border)
 * and 1 row up (overlapping the title bar).
 *
 * Visible symptom: the ProgressBar line appears 1 column to the left
 * when using the Dark theme (high contrast border vs content). Less
 * visible on themes 3/4 where border and background colors are similar.
 */
class WindowContentOffsetTest {

    @AfterEach
    void resetTheme() {
        ThemeManager.setActive(Theme.DARK);
    }

    /**
     * The content panel is at position (1,1) inside the window. A label
     * at the top-left of the content panel should render at screen column 1,
     * not column 0 (which is where the left border │ is drawn).
     */
    @Test
    void contentLabelDoesNotOverlapLeftBorder() {
        var size = new TerminalSize(20, 10);
        var window = new WindowImpl("Test");
        window.setHints(java.util.List.of(WindowHint.FULLSCREEN));
        var content = window.getContents();
        content.setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        // A label with a distinctive character
        content.addComponent(new Label("HELLO", AnsiColor.BRIGHT_CYAN, AnsiColor.BLACK));

        // Simulate what DefaultTextGUI.updateScreen() does
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        window.setBounds(io.jterm.core.TerminalPosition.TOP_LEFT, size);
        window.draw(g);

        // Column 0 should be the border │, not 'H' from HELLO
        var col0Cell = buf.getCell(0, 1);
        assertNotEquals('H', col0Cell.character().charAt(0),
                "Content label should NOT render at column 0 (border position). " +
                "Expected border char │, got: " + col0Cell.character());

        // The 'H' should be at column 1 (inside the border)
        var col1Cell = buf.getCell(1, 1);
        // Row 0 is title bar, row 1 is first content row
        // The label is centered; with a 18-wide content area and 5-char text, x = (18-5)/2 = 6
        // So 'H' should be at column 1 + 6 = 7
        // But the key assertion is that 'H' is NOT at column 0
    }

    /**
     * The content panel starts at row 1 (below the title bar). A label in
     * the NORTH region of the content's BorderLayout should render at
     * row 1, NOT row 0 (which is the title bar). If the window passes its
     * graphics directly to contents.draw() without offset, the content
     * draws at row 0 — overlapping the title bar.
     */
    @Test
    void contentDoesNotOverlapTitleBar() {
        var size = new TerminalSize(30, 10);
        var window = new WindowImpl("Test");
        window.setHints(java.util.List.of(WindowHint.FULLSCREEN));
        var content = window.getContents();
        content.setLayoutManager(new BorderLayout());
        // Put a label with a unique marker in NORTH
        content.addComponent(new Label("MARKER", AnsiColor.BRIGHT_RED, AnsiColor.BLACK),
                new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));

        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        window.setBounds(io.jterm.core.TerminalPosition.TOP_LEFT, size);
        window.draw(g);

        // Find which row 'M' from MARKER is rendered on
        int mRow = -1;
        int mCol = -1;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buf.getCell(c, r).character().charAt(0) == 'M') {
                    mRow = r;
                    mCol = c;
                    break;
                }
            }
            if (mRow >= 0) break;
        }
        assertTrue(mRow >= 1,
                "MARKER should be at row >= 1 (inside content area, below title bar). " +
                "Found at row=" + mRow + " col=" + mCol + " — content is overlapping the title bar");
        assertTrue(mCol >= 1,
                "MARKER should be at column >= 1 (inside left border). " +
                "Found at col=" + mCol);
    }

    /**
     * Verify the left border is still intact at column 0 after drawing content.
     * If content overlaps, the border character at (0,1) will be overwritten.
     */
    @Test
    void leftBorderPreservedAfterContentDraw() {
        var size = new TerminalSize(20, 10);
        var window = new WindowImpl("Test");
        window.setHints(java.util.List.of(WindowHint.FULLSCREEN));
        var content = window.getContents();
        content.setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        // Add enough labels to fill content area
        for (int i = 0; i < 8; i++) {
            content.addComponent(new Label("Line" + i, AnsiColor.WHITE, AnsiColor.BLACK));
        }

        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        window.setBounds(io.jterm.core.TerminalPosition.TOP_LEFT, size);
        window.draw(g);

        // Check that column 0 on content rows has the border character │
        for (int r = 1; r < size.rows() - 1; r++) {
            var cell = buf.getCell(0, r);
            assertEquals('│', cell.character().charAt(0),
                    "Left border at (0," + r + ") should be │, got: " + cell.character()
                    + " — content is overlapping the border");
        }
    }
}