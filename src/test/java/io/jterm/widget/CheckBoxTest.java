package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.ThemeManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CheckBoxTest {
    @Test
    void initialStateUnselected() {
        var cb = new CheckBox("Option");
        assertFalse(cb.isSelected());
    }

    @Test
    void setSelectedUpdatesState() {
        var cb = new CheckBox("Option");
        cb.setSelected(true);
        assertTrue(cb.isSelected());
    }

    @Test
    void toggleFlipsState() {
        var cb = new CheckBox("Option");
        cb.toggle();
        assertTrue(cb.isSelected());
        cb.toggle();
        assertFalse(cb.isSelected());
    }

    @Test
    void toggleFiresListener() {
        var cb = new CheckBox("Option");
        var fired = new boolean[1];
        cb.addListener(() -> fired[0] = !fired[0]);
        cb.toggle();
        assertTrue(fired[0]);
        cb.toggle();
        assertFalse(fired[0]);
    }

    @Test
    void spaceKeyToggles() {
        var cb = new CheckBox("Option");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        cb.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        assertTrue(cb.isSelected());
    }

    @Test
    void enterKeyDoesNotToggle() {
        // Enter is intentionally NOT handled by CheckBox — in dialog contexts,
        // Enter means "submit" and is handled by the dialog, not the checkbox.
        var cb = new CheckBox("Option");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        cb.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertFalse(cb.isSelected());
    }

    @Test
    void preferredSizeIncludesLabel() {
        var cb = new CheckBox("Yes");
        assertEquals(new TerminalSize(7, 1), cb.getPreferredSize());
    }

    // ── Expanded coverage ──────────────────────────────────────────

    @Test
    void setSelectedFalseAfterTrue() {
        var cb = new CheckBox("Option");
        cb.setSelected(true);
        assertTrue(cb.isSelected());
        cb.setSelected(false);
        assertFalse(cb.isSelected());
    }

    @Test
    void setSelectedDoesNotFireListener() {
        // setSelected() directly sets state without notifying listeners
        var cb = new CheckBox("Option");
        var count = new int[1];
        cb.addListener(() -> count[0]++);
        cb.setSelected(true);
        assertEquals(0, count[0], "setSelected should not fire listeners");
    }

    @Test
    void toggleFiresMultipleListeners() {
        var cb = new CheckBox("Option");
        var count = new int[1];
        cb.addListener(() -> count[0]++);
        cb.addListener(() -> count[0] += 10);
        cb.toggle();
        assertEquals(11, count[0]);
    }

    @Test
    void spaceKeyTogglesOff() {
        var cb = new CheckBox("Option");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        cb.handleKeyStroke(KeyStroke.character(' ', false, false, false)); // on
        assertTrue(cb.isSelected());
        cb.handleKeyStroke(KeyStroke.character(' ', false, false, false)); // off
        assertFalse(cb.isSelected());
    }

    @Test
    void enterKeyDoesNotToggleOff() {
        // Enter does not toggle — see enterKeyDoesNotToggle for rationale
        var cb = new CheckBox("Option");
        cb.setSelected(true);
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        cb.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertTrue(cb.isSelected(), "Enter should not toggle checkbox state");
    }

    @Test
    void otherCharacterKeyDoesNotToggle() {
        var cb = new CheckBox("Option");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        cb.handleKeyStroke(KeyStroke.character('x', false, false, false));
        assertFalse(cb.isSelected());
    }

    @Test
    void arrowKeysDoNotToggle() {
        var cb = new CheckBox("Option");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        cb.handleKeyStroke(new KeyStroke(KeyType.ARROW_UP));
        cb.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        cb.handleKeyStroke(new KeyStroke(KeyType.ARROW_LEFT));
        cb.handleKeyStroke(new KeyStroke(KeyType.ARROW_RIGHT));
        assertFalse(cb.isSelected());
    }

    @Test
    void unsupportedKeysDoNotToggle() {
        var cb = new CheckBox("Option");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        for (var kt : new KeyType[]{
                KeyType.HOME, KeyType.END, KeyType.PAGE_UP, KeyType.PAGE_DOWN,
                KeyType.TAB, KeyType.ESCAPE, KeyType.DELETE, KeyType.BACKSPACE,
                KeyType.INSERT, KeyType.F1, KeyType.EOF, KeyType.UNKNOWN}) {
            cb.handleKeyStroke(new KeyStroke(kt));
        }
        assertFalse(cb.isSelected());
    }

    @Test
    void drawUncheckedShowsEmptyBracket() {
        var cb = new CheckBox("Test");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(12, 1));
        var buf = new ScreenBuffer(new TerminalSize(12, 1));
        cb.draw(new TextGraphics(buf));
        // "[ ] Test" -> '[', ' ', ']', ' ', 'T'...
        assertTrue(buf.getCell(0, 0).is('['));
        assertTrue(buf.getCell(1, 0).is(' '));
        assertTrue(buf.getCell(2, 0).is(']'));
        assertTrue(buf.getCell(4, 0).is('T'));
    }

    @Test
    void drawCheckedShowsCheckMark() {
        var cb = new CheckBox("Test");
        cb.setSelected(true);
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(12, 1));
        var buf = new ScreenBuffer(new TerminalSize(12, 1));
        cb.draw(new TextGraphics(buf));
        // "[✓] Test" -> '[', '✓', ']', ' ', 'T'...
        assertTrue(buf.getCell(0, 0).is('['));
        assertEquals("✓", buf.getCell(1, 0).character());
        assertTrue(buf.getCell(2, 0).is(']'));
    }

    @Test
    void drawRendersLabelText() {
        var cb = new CheckBox("MyLabel");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        var buf = new ScreenBuffer(new TerminalSize(20, 1));
        cb.draw(new TextGraphics(buf));
        // "[ ] MyLabel" -> label starts at col 4
        assertTrue(buf.getCell(4, 0).is('M'));
        assertTrue(buf.getCell(5, 0).is('y'));
        assertTrue(buf.getCell(6, 0).is('L'));
    }

    @Test
    void preferredSizeEmptyLabel() {
        var cb = new CheckBox("");
        assertEquals(new TerminalSize(4, 1), cb.getPreferredSize());
    }

    @Test
    void preferredSizeSingleCharLabel() {
        var cb = new CheckBox("X");
        assertEquals(new TerminalSize(5, 1), cb.getPreferredSize());
    }

    // ── Focus highlight tests ──────────────────────────────────────

    @Test
    void drawUnfocusedUsesNormalColors() {
        var cb = new CheckBox("Test");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(12, 1));
        // Ensure not focused
        cb.setFocused(false);
        var buf = new ScreenBuffer(new TerminalSize(12, 1));
        cb.draw(new TextGraphics(buf));
        var theme = ThemeManager.active();
        var cell = buf.getCell(0, 0);
        assertEquals(theme.foreground(), cell.fg(), "unfocused checkbox should use theme foreground");
        assertEquals(theme.background(), cell.bg(), "unfocused checkbox should use theme background");
    }

    @Test
    void drawFocusedUsesFocusColors() {
        var cb = new CheckBox("Test");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(12, 1));
        cb.setFocused(true);
        var buf = new ScreenBuffer(new TerminalSize(12, 1));
        cb.draw(new TextGraphics(buf));
        var theme = ThemeManager.active();
        var cell = buf.getCell(0, 0);
        assertEquals(theme.focusFg(), cell.fg(), "focused checkbox should use theme focusFg");
        assertEquals(theme.focusBg(), cell.bg(), "focused checkbox should use theme focusBg");
    }

    @Test
    void drawFocusedCheckedStillUsesFocusColors() {
        var cb = new CheckBox("Test");
        cb.setSelected(true);
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(12, 1));
        cb.setFocused(true);
        var buf = new ScreenBuffer(new TerminalSize(12, 1));
        cb.draw(new TextGraphics(buf));
        var theme = ThemeManager.active();
        var cell = buf.getCell(0, 0);
        assertEquals(theme.focusFg(), cell.fg(), "focused+checked checkbox should use theme focusFg");
        assertEquals(theme.focusBg(), cell.bg(), "focused+checked checkbox should use theme focusBg");
        // Content should still be the checkmark marker
        assertTrue(buf.getCell(0, 0).is('['));
        assertEquals("✓", buf.getCell(1, 0).character());
    }

    @Test
    void drawFocusedLabelCellsUseFocusColors() {
        var cb = new CheckBox("MyLabel");
        cb.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        cb.setFocused(true);
        var buf = new ScreenBuffer(new TerminalSize(20, 1));
        cb.draw(new TextGraphics(buf));
        var theme = ThemeManager.active();
        // Label starts at col 4: "[ ] MyLabel"
        var cell = buf.getCell(4, 0);
        assertTrue(cell.is('M'));
        assertEquals(theme.focusFg(), cell.fg(), "focused checkbox label should use theme focusFg");
        assertEquals(theme.focusBg(), cell.bg(), "focused checkbox label should use theme focusBg");
    }
}