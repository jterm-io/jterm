package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BoxExpandTransition}: a rectangle that grows from the
 * center outward to reveal the new screen content.
 */
class BoxExpandTransitionTest {

    private static final TerminalSize SIZE = new TerminalSize(40, 12);
    private static final char OLD_CHAR = 'O';
    private static final char NEW_CHAR = 'N';

    /** Creates an old screen buffer filled with 'O' cells. */
    private ScreenBuffer oldScreen() {
        var buf = new ScreenBuffer(SIZE);
        for (int r = 0; r < SIZE.rows(); r++)
            for (int c = 0; c < SIZE.columns(); c++)
                buf.setCell(c, r, new TextCell(OLD_CHAR,
                        AnsiColor.WHITE, AnsiColor.BLACK));
        return buf;
    }

    /** Creates a graphics buffer (simulating new content) filled with 'N' cells. */
    private TextGraphics newContentGraphics() {
        var buf = new ScreenBuffer(SIZE);
        for (int r = 0; r < SIZE.rows(); r++)
            for (int c = 0; c < SIZE.columns(); c++)
                buf.setCell(c, r, new TextCell(NEW_CHAR,
                        AnsiColor.WHITE, AnsiColor.BLACK));
        return new TextGraphics(buf);
    }

    @Test
    @DisplayName("durationMs returns the configured value")
    void durationMsReturnsConfiguredValue() {
        var t = new BoxExpandTransition(800, oldScreen());
        assertEquals(800, t.durationMs());
    }

    @Test
    @DisplayName("targetFps returns 30")
    void targetFpsReturns30() {
        var t = new BoxExpandTransition(800, oldScreen());
        assertEquals(30, t.targetFps());
    }

    @Test
    @DisplayName("At progress 0.0, the entire old screen is visible")
    void progressZeroShowsOldScreen() {
        var old = oldScreen();
        var t = new BoxExpandTransition(800, old);
        var g = newContentGraphics();
        t.renderFrame(g, SIZE, 0.0);

        for (int r = 0; r < SIZE.rows(); r++)
            for (int c = 0; c < SIZE.columns(); c++)
                assertEquals(String.valueOf(OLD_CHAR), g.getCell(c, r).character(),
                        "Cell at (" + c + "," + r + ") should show old content at p=0");
    }

    @Test
    @DisplayName("At progress 1.0, the entire new screen is visible")
    void progressOneShowsNewScreen() {
        var old = oldScreen();
        var t = new BoxExpandTransition(800, old);
        var g = newContentGraphics();
        t.renderFrame(g, SIZE, 1.0);

        for (int r = 0; r < SIZE.rows(); r++)
            for (int c = 0; c < SIZE.columns(); c++)
                assertEquals(String.valueOf(NEW_CHAR), g.getCell(c, r).character(),
                        "Cell at (" + c + "," + r + ") should show new content at p=1");
    }

    @Test
    @DisplayName("At progress 0.5, center cell shows new content")
    void progressHalfCenterShowsNewContent() {
        var old = oldScreen();
        var t = new BoxExpandTransition(800, old);
        var g = newContentGraphics();
        t.renderFrame(g, SIZE, 0.5);

        int centerCol = SIZE.columns() / 2;
        int centerRow = SIZE.rows() / 2;
        // The center cell should be inside the box → new content
        assertEquals(String.valueOf(NEW_CHAR), g.getCell(centerCol, centerRow).character(),
                "Center cell should show new content at p=0.5");
    }

    @Test
    @DisplayName("At progress 0.5, corner cells show old content")
    void progressHalfCornersShowOldContent() {
        var old = oldScreen();
        var t = new BoxExpandTransition(800, old);
        var g = newContentGraphics();
        t.renderFrame(g, SIZE, 0.5);

        // Corners should be outside the box → old content
        assertEquals(String.valueOf(OLD_CHAR), g.getCell(0, 0).character(),
                "Top-left corner should show old content at p=0.5");
        assertEquals(String.valueOf(OLD_CHAR), g.getCell(SIZE.columns() - 1, SIZE.rows() - 1).character(),
                "Bottom-right corner should show old content at p=0.5");
    }

    @Test
    @DisplayName("Box expands symmetrically from center")
    void boxExpandsSymmetrically() {
        var old = oldScreen();
        var t = new BoxExpandTransition(800, old);

        // At low progress, only a small box around the center is new content
        var g = newContentGraphics();
        t.renderFrame(g, SIZE, 0.1);

        int centerCol = SIZE.columns() / 2;
        int centerRow = SIZE.rows() / 2;

        // Cell just above the center should be old (box is very small at p=0.1)
        // The box half-size at p=0.1 is ceil(0.1 * maxHalf) where maxHalf ≈ 20
        // So half ≈ 2, meaning the box spans centerRow-2 to centerRow+2
        // A cell at row 0 should definitely be old content
        assertEquals(String.valueOf(OLD_CHAR), g.getCell(centerCol, 0).character(),
                "Top row should show old content at p=0.1");
        assertEquals(String.valueOf(OLD_CHAR), g.getCell(centerCol, SIZE.rows() - 1).character(),
                "Bottom row should show old content at p=0.1");
    }

    @Test
    @DisplayName("Border cells use the configured border color")
    void borderCellsUseBorderColor() {
        var old = oldScreen();
        var t = new BoxExpandTransition(800, old, AnsiColor.BRIGHT_GREEN);
        var g = newContentGraphics();
        t.renderFrame(g, SIZE, 0.3);

        // At p=0.3, the box should be mid-size. Scan for border cells.
        boolean foundBorder = false;
        for (int r = 0; r < SIZE.rows() && !foundBorder; r++) {
            for (int c = 0; c < SIZE.columns() && !foundBorder; c++) {
                var cell = g.getCell(c, r);
                if (cell.character().equals("\u2588") && cell.fg() == AnsiColor.BRIGHT_GREEN) {
                    foundBorder = true;
                }
            }
        }
        assertTrue(foundBorder, "Should find at least one border cell with BRIGHT_GREEN color");
    }

    @Test
    @DisplayName("Progress beyond 1.0 is clamped to show new content")
    void progressBeyondOneClamped() {
        var old = oldScreen();
        var t = new BoxExpandTransition(800, old);
        var g = newContentGraphics();
        t.renderFrame(g, SIZE, 1.5);

        // Should behave as p=1.0 — all new content
        for (int r = 0; r < SIZE.rows(); r++)
            for (int c = 0; c < SIZE.columns(); c++)
                assertEquals(String.valueOf(NEW_CHAR), g.getCell(c, r).character(),
                        "Cell at (" + c + "," + r + ") should show new content at p>1");
    }

    @Test
    @DisplayName("Progress below 0.0 is clamped to show old content")
    void progressBelowZeroClamped() {
        var old = oldScreen();
        var t = new BoxExpandTransition(800, old);
        var g = newContentGraphics();
        t.renderFrame(g, SIZE, -0.5);

        for (int r = 0; r < SIZE.rows(); r++)
            for (int c = 0; c < SIZE.columns(); c++)
                assertEquals(String.valueOf(OLD_CHAR), g.getCell(c, r).character(),
                        "Cell at (" + c + "," + r + ") should show old content at p<0");
    }

    @Test
    @DisplayName("Default border color is BRIGHT_CYAN")
    void defaultBorderColorIsBrightCyan() {
        var old = oldScreen();
        var t = new BoxExpandTransition(800, old);
        var g = newContentGraphics();
        t.renderFrame(g, SIZE, 0.2);

        // Find a border cell and check its color
        boolean foundCyanBorder = false;
        for (int r = 0; r < SIZE.rows() && !foundCyanBorder; r++) {
            for (int c = 0; c < SIZE.columns() && !foundCyanBorder; c++) {
                var cell = g.getCell(c, r);
                if (cell.character().equals("\u2588") && cell.fg() == AnsiColor.BRIGHT_CYAN) {
                    foundCyanBorder = true;
                }
            }
        }
        assertTrue(foundCyanBorder, "Should find a BRIGHT_CYAN border cell with default constructor");
    }

    @Test
    @DisplayName("New content inside the box is preserved (not just border)")
    void newContentInsideBoxIsPreserved() {
        var old = oldScreen();
        var t = new BoxExpandTransition(800, old);
        var g = newContentGraphics();
        t.renderFrame(g, SIZE, 0.5);

        int centerCol = SIZE.columns() / 2;
        int centerRow = SIZE.rows() / 2;

        // A cell that's well inside the box (not on the border) should show new content
        // At p=0.5, the box half is ~10, so center+1 is inside but not on the border
        var cell = g.getCell(centerCol, centerRow);
        assertEquals(String.valueOf(NEW_CHAR), cell.character(),
                "Center cell should show new content (inside the box, not border)");
    }

    @Test
    @DisplayName("Handles 1x1 terminal without crashing")
    void handlesOneByOneTerminal() {
        var oneByOne = new TerminalSize(1, 1);
        var old = new ScreenBuffer(oneByOne);
        old.setCell(0, 0, new TextCell(OLD_CHAR, AnsiColor.WHITE, AnsiColor.BLACK));
        var t = new BoxExpandTransition(800, old);

        var buf = new ScreenBuffer(oneByOne);
        buf.setCell(0, 0, new TextCell(NEW_CHAR, AnsiColor.WHITE, AnsiColor.BLACK));
        var g = new TextGraphics(buf);

        assertDoesNotThrow(() -> t.renderFrame(g, oneByOne, 0.5));
    }
}