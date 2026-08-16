package io.jterm.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TextStyleResolver} — a functional interface for per-character styling.
 */
class TextStyleResolverTest {

    @Test
    void lambdaReturningOverrideStyleWorks() {
        TextStyleResolver resolver = (charIndex, c, defaultStyle) ->
                defaultStyle.withForeground(AnsiColor.RED);
        var defaults = new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLACK);
        var result = resolver.resolveStyle(0, 'H', defaults);
        assertNotNull(result);
        assertEquals(AnsiColor.RED, result.fg());
    }

    @Test
    void lambdaReturningNullMeansUseDefault() {
        TextStyleResolver resolver = (charIndex, c, defaultStyle) -> null;
        var defaults = new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLACK);
        var result = resolver.resolveStyle(5, 'x', defaults);
        assertNull(result);
    }

    @Test
    void lambdaCanConditionallyOverride() {
        TextStyleResolver resolver = (charIndex, c, defaultStyle) ->
                charIndex < 3 ? defaultStyle.withForeground(AnsiColor.RED) : null;
        var defaults = new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLACK);
        assertNotNull(resolver.resolveStyle(0, 'a', defaults));
        assertNotNull(resolver.resolveStyle(2, 'c', defaults));
        assertNull(resolver.resolveStyle(3, 'd', defaults));
        assertNull(resolver.resolveStyle(10, 'z', defaults));
    }

    @Test
    void isFunctionalInterface() {
        // Verify annotation is present
        assertTrue(TextStyleResolver.class.isAnnotationPresent(FunctionalInterface.class));
    }
}