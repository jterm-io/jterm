package io.jterm.widget;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.style.Theme;
import io.jterm.style.ThemeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug: Button.applyTheme() is called only in the constructor, caching the
 * theme colors at construction time. When the theme changes at runtime,
 * buttons keep their old theme colors, causing visual mismatches.
 *
 * Visible symptom: When switching themes in ThemeDemo, the bottom buttons
 * retain the colors of the previous theme, creating a visible offset/patch
 * that looks like the buttons are shifted.
 */
class ButtonThemeTest {

    @AfterEach
    void resetTheme() {
        ThemeManager.setActive(Theme.DARK);
    }

    @Test
    void buttonUsesCurrentThemeColors() {
        ThemeManager.setActive(Theme.DARK);
        var button = new Button("Test");
        button.setBounds(new io.jterm.core.TerminalPosition(0, 0),
                new io.jterm.core.TerminalSize(10, 1));

        var buf = new ScreenBuffer(new TerminalSize(10, 1));
        var g = new TextGraphics(buf);
        button.draw(g);

        // With DARK theme, fg should be WHITE, bg should be BLACK
        var cell = buf.getCell(1, 0); // "[ T" - the 'T' in "[ Test ]"
        assertEquals(AnsiColor.WHITE, cell.fg(),
                "Button fg should be DARK theme foreground (WHITE)");
        assertEquals(AnsiColor.BLACK, cell.bg(),
                "Button bg should be DARK theme background (BLACK)");
    }

    @Test
    void buttonUpdatesColorsWhenThemeChanges() {
        ThemeManager.setActive(Theme.DARK);
        var button = new Button("Test");
        button.setBounds(new io.jterm.core.TerminalPosition(0, 0),
                new io.jterm.core.TerminalSize(10, 1));

        // Switch to YELLOW_ON_BLUE
        ThemeManager.setActive(Theme.YELLOW_ON_BLUE);

        var buf = new ScreenBuffer(new TerminalSize(10, 1));
        var g = new TextGraphics(buf);
        button.draw(g);

        // With YELLOW_ON_BLUE, fg should be BRIGHT_YELLOW, bg should be BLUE
        var cell = buf.getCell(1, 0);
        assertEquals(AnsiColor.BRIGHT_YELLOW, cell.fg(),
                "Button fg should update to YELLOW_ON_BLUE foreground (BRIGHT_YELLOW) after theme change");
        assertEquals(AnsiColor.BLUE, cell.bg(),
                "Button bg should update to YELLOW_ON_BLUE background (BLUE) after theme change");
    }

    @Test
    void buttonFocusColorsUpdateWhenThemeChanges() {
        ThemeManager.setActive(Theme.DARK);
        var button = new Button("Test");
        button.setFocused(true);
        button.setBounds(new io.jterm.core.TerminalPosition(0, 0),
                new io.jterm.core.TerminalSize(10, 1));

        // Switch to GREEN_ON_BLACK
        ThemeManager.setActive(Theme.GREEN_ON_BLACK);

        var buf = new ScreenBuffer(new TerminalSize(10, 1));
        var g = new TextGraphics(buf);
        button.draw(g);

        // With GREEN_ON_BLACK, focusFg should be BLACK, focusBg should be BRIGHT_GREEN
        var cell = buf.getCell(1, 0);
        assertEquals(AnsiColor.BLACK, cell.fg(),
                "Button focus fg should update to GREEN_ON_BLACK focusFg (BLACK)");
        assertEquals(AnsiColor.BRIGHT_GREEN, cell.bg(),
                "Button focus bg should update to GREEN_ON_BLACK focusBg (BRIGHT_GREEN)");
    }
}