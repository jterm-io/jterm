package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.ThemeManager;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RadioGroupTest {

    @Test
    void addFirstButtonAutoSelectsIt() {
        var group = new RadioGroup();
        var btn = new RadioButton("One");
        group.add(btn);
        assertSame(btn, group.getSelected());
        assertTrue(btn.isSelected());
    }

    @Test
    void addSecondButtonKeepsFirstSelectedAndSecondNotSelected() {
        var group = new RadioGroup();
        var first = new RadioButton("First");
        var second = new RadioButton("Second");
        group.add(first);
        group.add(second);
        assertSame(first, group.getSelected());
        assertTrue(first.isSelected());
        assertFalse(second.isSelected());
    }

    @Test
    void selectOnSecondButtonDeselectsFirstAndSelectsSecond() {
        var group = new RadioGroup();
        var first = new RadioButton("First");
        var second = new RadioButton("Second");
        group.add(first);
        group.add(second);
        second.select();
        assertSame(second, group.getSelected());
        assertFalse(first.isSelected());
        assertTrue(second.isSelected());
    }

    @Test
    void getSelectedReturnsCurrentlySelectedButton() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        var c = new RadioButton("C");
        group.add(a);
        group.add(b);
        group.add(c);
        assertSame(a, group.getSelected());
        b.select();
        assertSame(b, group.getSelected());
        c.select();
        assertSame(c, group.getSelected());
    }

    @Test
    void getButtonsReturnsListOfAllButtons() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        var c = new RadioButton("C");
        group.add(a);
        group.add(b);
        group.add(c);
        var buttons = group.getButtons();
        assertEquals(3, buttons.size());
        assertTrue(buttons.contains(a));
        assertTrue(buttons.contains(b));
        assertTrue(buttons.contains(c));
    }

    @Test
    void getButtonsReturnsDefensiveCopy() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        group.add(a);
        var buttons = group.getButtons();
        buttons.clear();
        // Mutating the returned list must not affect the group's internal state.
        assertEquals(1, group.getButtons().size());
    }

    @Test
    void removeSelectedButtonMovesSelectionToFirstRemaining() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        var c = new RadioButton("C");
        group.add(a);
        group.add(b);
        group.add(c);
        a.select();
        assertSame(a, group.getSelected());
        group.remove(a);
        assertSame(b, group.getSelected());
        assertTrue(b.isSelected());
        assertNull(a.getGroup());
    }

    @Test
    void removeSelectedButtonWhenOnlyOneLeavesSelectionNull() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        group.add(a);
        group.remove(a);
        assertNull(group.getSelected());
        assertNull(a.getGroup());
    }

    @Test
    void removeNonSelectedButtonLeavesSelectionUnchanged() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        var c = new RadioButton("C");
        group.add(a);
        group.add(b);
        group.add(c);
        b.select();
        assertSame(b, group.getSelected());
        group.remove(c);
        assertSame(b, group.getSelected());
        assertTrue(b.isSelected());
    }

    @Test
    void addSelectionListenerFiresOnSelectionChange() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        group.add(a);
        group.add(b);
        var fired = new AtomicInteger(0);
        group.addSelectionListener(fired::incrementAndGet);
        int before = fired.get();
        b.select();
        assertTrue(fired.get() > before, "selection listener should fire on selection change");
    }

    @Test
    void addSelectionListenerDoesNotFireWhenSelectionUnchanged() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        group.add(a);
        group.add(b);
        var fired = new AtomicInteger(0);
        group.addSelectionListener(fired::incrementAndGet);
        int before = fired.get();
        // Selecting the already-selected button should be a no-op (no listener fire).
        a.select();
        assertEquals(before, fired.get());
    }

    @Test
    void selectNullDeselectsAll() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        group.add(a);
        group.add(b);
        b.select();
        assertTrue(b.isSelected());
        group.select(null);
        assertNull(group.getSelected());
        assertFalse(a.isSelected());
        assertFalse(b.isSelected());
    }

    @Test
    void selectButtonNotInGroupIsIgnored() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        group.add(a);
        var orphan = new RadioButton("Orphan");
        group.select(orphan);
        assertSame(a, group.getSelected());
        assertFalse(orphan.isSelected());
        assertNull(orphan.getGroup());
    }

    @Test
    void addSameButtonTwiceOnlyAddsOnce() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        group.add(a);
        group.add(a);
        assertEquals(1, group.getButtons().size());
        assertSame(a, group.getSelected());
    }

    @Test
    void radioButtonSelectWithGroupCallsGroupSelect() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        group.add(a);
        group.add(b);
        b.select();
        assertSame(b, group.getSelected());
        assertFalse(a.isSelected());
        assertTrue(b.isSelected());
    }

    @Test
    void radioButtonSelectWithoutGroupSetsSelectedDirectly() {
        var orphan = new RadioButton("Solo");
        assertFalse(orphan.isSelected());
        orphan.select();
        assertTrue(orphan.isSelected());
        assertNull(orphan.getGroup());
    }

    @Test
    void radioButtonHandleKeyStrokeSpaceSelects() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        group.add(a);
        group.add(b);
        var spaceStroke = new KeyStroke(KeyType.CHARACTER, ' ', false, false, false);
        b.handleKeyStroke(spaceStroke);
        assertSame(b, group.getSelected());
        assertTrue(b.isSelected());
        assertFalse(a.isSelected());
    }

    @Test
    void radioButtonHandleKeyStrokeEnterDoesNotSelect() {
        // Enter is intentionally NOT handled by RadioButton — in dialog contexts,
        // Enter means "submit" and is handled by the dialog, not the radio.
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        group.add(a);
        group.add(b);
        var enterStroke = new KeyStroke(KeyType.ENTER);
        b.handleKeyStroke(enterStroke);
        assertSame(a, group.getSelected());
        assertFalse(b.isSelected());
    }

    @Test
    void radioButtonHandleKeyStrokeOtherKeyDoesNotSelect() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        group.add(a);
        group.add(b);
        // An unrelated character should not trigger selection.
        var otherStroke = new KeyStroke(KeyType.CHARACTER, 'x', false, false, false);
        b.handleKeyStroke(otherStroke);
        assertSame(a, group.getSelected());
        assertFalse(b.isSelected());
    }

    @Test
    void radioButtonGetLabelReturnsConstructorLabel() {
        var btn = new RadioButton("Hello");
        assertEquals("Hello", btn.getLabel());
    }

    @Test
    void radioButtonSetLabelUpdatesLabel() {
        var btn = new RadioButton("Old");
        btn.setLabel("New");
        assertEquals("New", btn.getLabel());
    }

    @Test
    void radioButtonGetGroupReturnsTheGroup() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        group.add(a);
        assertSame(group, a.getGroup());
    }

    @Test
    void radioButtonGetGroupNullWhenNotInGroup() {
        var orphan = new RadioButton("Orphan");
        assertNull(orphan.getGroup());
    }

    @Test
    void addButtonAlreadyInAnotherGroupWorks() {
        var group1 = new RadioGroup();
        var group2 = new RadioGroup();
        var a = new RadioButton("A");
        var b = new RadioButton("B");
        group1.add(a);
        group1.add(b);
        // Move b to group2 by adding it; the edge case is that b's group reference
        // is overwritten. The button should end up associated with group2.
        group2.add(b);
        assertSame(group2, b.getGroup());
        // Selecting b should now affect group2, not group1.
        assertSame(a, group1.getSelected());
        b.select();
        assertSame(b, group2.getSelected());
        // group1's selection should remain unchanged (a) because b is no longer
        // tracked by group1's select() path.
        assertSame(a, group1.getSelected());
    }

    @Test
    void selectSameButtonTwiceDoesNotFireListenerSecondTime() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        group.add(a);
        var fired = new AtomicInteger(0);
        group.addSelectionListener(fired::incrementAndGet);
        int baseline = fired.get();
        a.select(); // already selected -> no-op
        assertEquals(baseline, fired.get());
    }

    @Test
    void removingButtonThenReaddingReSelectsIt() {
        var group = new RadioGroup();
        var a = new RadioButton("A");
        group.add(a);
        group.remove(a);
        assertNull(group.getSelected());
        group.add(a);
        assertSame(a, group.getSelected());
        assertTrue(a.isSelected());
        assertSame(group, a.getGroup());
    }

    @Test
    void newRadioGroupHasNoSelectedAndNoButtons() {
        var group = new RadioGroup();
        assertNull(group.getSelected());
        assertTrue(group.getButtons().isEmpty());
    }

    @Test
    void radioButtonPreferredSizeModeAccountsForLabel() {
        var btn = new RadioButton("Hi");
        var size = btn.getPreferredSize();
        assertEquals(2 + 4, size.columns(), "marker plus label length");
        assertEquals(1, size.rows());
    }

    // ── Focus highlight tests ──────────────────────────────────────

    @Test
    void drawUnfocusedUsesNormalColors() {
        var btn = new RadioButton("Test");
        btn.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(12, 1));
        btn.setFocused(false);
        var buf = new ScreenBuffer(new TerminalSize(12, 1));
        btn.draw(new TextGraphics(buf));
        var theme = ThemeManager.active();
        var cell = buf.getCell(0, 0);
        assertEquals(theme.foreground(), cell.fg(), "unfocused radio should use theme foreground");
        assertEquals(theme.background(), cell.bg(), "unfocused radio should use theme background");
    }

    @Test
    void drawFocusedUsesFocusColors() {
        var btn = new RadioButton("Test");
        btn.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(12, 1));
        btn.setFocused(true);
        var buf = new ScreenBuffer(new TerminalSize(12, 1));
        btn.draw(new TextGraphics(buf));
        var theme = ThemeManager.active();
        var cell = buf.getCell(0, 0);
        assertEquals(theme.focusFg(), cell.fg(), "focused radio should use theme focusFg");
        assertEquals(theme.focusBg(), cell.bg(), "focused radio should use theme focusBg");
    }

    @Test
    void drawFocusedSelectedStillUsesFocusColors() {
        var group = new RadioGroup();
        var btn = new RadioButton("Test");
        group.add(btn);
        btn.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(12, 1));
        btn.setFocused(true);
        var buf = new ScreenBuffer(new TerminalSize(12, 1));
        btn.draw(new TextGraphics(buf));
        var theme = ThemeManager.active();
        var cell = buf.getCell(0, 0);
        assertEquals(theme.focusFg(), cell.fg(), "focused+selected radio should use theme focusFg");
        assertEquals(theme.focusBg(), cell.bg(), "focused+selected radio should use theme focusBg");
        // Content should still be the selected marker
        assertTrue(buf.getCell(0, 0).is('('));
        assertEquals("●", buf.getCell(1, 0).character());
    }

    @Test
    void drawFocusedLabelCellsUseFocusColors() {
        var btn = new RadioButton("MyLabel");
        btn.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        btn.setFocused(true);
        var buf = new ScreenBuffer(new TerminalSize(20, 1));
        btn.draw(new TextGraphics(buf));
        var theme = ThemeManager.active();
        // Label starts at col 4: "(○) MyLabel"
        var cell = buf.getCell(4, 0);
        assertTrue(cell.is('M'));
        assertEquals(theme.focusFg(), cell.fg(), "focused radio label should use theme focusFg");
        assertEquals(theme.focusBg(), cell.bg(), "focused radio label should use theme focusBg");
    }
}