package io.jterm.completion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link GhostTextSupport} shared helper used by both
 * {@link io.jterm.widget.TextBox} and {@link io.jterm.widget.TextArea}.
 */
class GhostTextSupportTest {

    @Test
    void noGhostTextByDefault() {
        var support = new GhostTextSupport();
        assertNull(support.getGhostText());
        assertFalse(support.hasGhostText());
    }

    @Test
    void refreshSetsGhostTextFromProvider() {
        var support = new GhostTextSupport((text, pos) -> "llo");
        support.refresh("he", 2);
        assertEquals("llo", support.getGhostText());
        assertTrue(support.hasGhostText());
    }

    @Test
    void refreshClearsWhenProviderReturnsNull() {
        var support = new GhostTextSupport((text, pos) ->
                text.equals("he") ? "llo" : null);
        support.refresh("he", 2);
        assertTrue(support.hasGhostText());

        support.refresh("xyz", 3);
        assertNull(support.getGhostText());
        assertFalse(support.hasGhostText());
    }

    @Test
    void refreshClearsWhenProviderReturnsEmpty() {
        var support = new GhostTextSupport((text, pos) -> "");
        support.refresh("he", 2);
        assertFalse(support.hasGhostText());
    }

    @Test
    void refreshDoesNothingWhenNoProvider() {
        var support = new GhostTextSupport();
        support.refresh("he", 2);
        assertNull(support.getGhostText());
    }

    @Test
    void clearResetsGhostText() {
        var support = new GhostTextSupport((text, pos) -> "llo");
        support.refresh("he", 2);
        assertTrue(support.hasGhostText());
        support.clear();
        assertNull(support.getGhostText());
    }

    @Test
    void setProviderReplacesProvider() {
        var support = new GhostTextSupport((text, pos) -> "abc");
        support.refresh("x", 1);
        assertEquals("abc", support.getGhostText());

        support.setProvider((text, pos) -> "xyz");
        assertNull(support.getGhostText(), "setProvider should clear ghost text");
        support.refresh("x", 1);
        assertEquals("xyz", support.getGhostText());
    }

    @Test
    void setProviderToNullDisablesCompletions() {
        var support = new GhostTextSupport((text, pos) -> "abc");
        support.setProvider(null);
        assertNull(support.getProvider());
        support.refresh("x", 1);
        assertNull(support.getGhostText());
    }

    @Test
    void tryAcceptSpaceReturnsSuffix() {
        var support = new GhostTextSupport((text, pos) -> "llo");
        support.refresh("he", 2);
        String accepted = support.tryAccept(' ');
        assertEquals("llo", accepted);
        assertFalse(support.hasGhostText(), "ghost text should be cleared after accept");
    }

    @Test
    void tryAcceptOtherCharReturnsNullAndClears() {
        var support = new GhostTextSupport((text, pos) -> "llo");
        support.refresh("he", 2);
        String accepted = support.tryAccept('x');
        assertNull(accepted, "non-space char should not accept");
        assertFalse(support.hasGhostText(), "ghost text should be cleared on other char");
    }

    @Test
    void tryAcceptSpaceWithoutGhostTextReturnsNull() {
        var support = new GhostTextSupport();
        String accepted = support.tryAccept(' ');
        assertNull(accepted);
    }

    @Test
    void tryAcceptTabReturnsSuffix() {
        var support = new GhostTextSupport((text, pos) -> "llo");
        support.refresh("he", 2);
        String accepted = support.tryAcceptTab();
        assertEquals("llo", accepted);
        assertFalse(support.hasGhostText());
    }

    @Test
    void tryAcceptTabWithoutGhostTextReturnsNull() {
        var support = new GhostTextSupport();
        String accepted = support.tryAcceptTab();
        assertNull(accepted);
    }
}