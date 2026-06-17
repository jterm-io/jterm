package io.jterm.style;

import java.util.EnumSet;

/**
 * Immutable representation of a single terminal cell.
 * Contains the character (as String for multi-codepoint support), foreground color,
 * background color, and SGR modifiers. Safe to share between buffers.
 */
public record TextCell(String character, Color fg, Color bg, EnumSet<SGR> modifiers) {

    public static final TextCell EMPTY = new TextCell(" ", AnsiColor.DEFAULT, AnsiColor.DEFAULT, EnumSet.noneOf(SGR.class));

    public TextCell {
        if (character == null || character.isEmpty())
            throw new IllegalArgumentException("Character cannot be null or empty");
        // Defensive copy of mutable EnumSet
        modifiers = modifiers == null ? EnumSet.noneOf(SGR.class) : EnumSet.copyOf(modifiers);
    }

    // Convenience constructors
    public TextCell(char c) {
        this(String.valueOf(c), AnsiColor.DEFAULT, AnsiColor.DEFAULT, EnumSet.noneOf(SGR.class));
    }

    public TextCell(char c, Color fg, Color bg, SGR... mods) {
        this(String.valueOf(c), fg, bg, mods.length == 0 ? EnumSet.noneOf(SGR.class) : EnumSet.of(mods[0], mods));
    }

    // Builder-style copy methods (return new instance since immutable)
    public TextCell withCharacter(char c) {
        return new TextCell(String.valueOf(c), fg, bg, modifiers);
    }

    public TextCell withForeground(Color color) {
        return new TextCell(character, color, bg, modifiers);
    }

    public TextCell withBackground(Color color) {
        return new TextCell(character, fg, color, modifiers);
    }

    public TextCell withModifier(SGR mod) {
        var mods = EnumSet.copyOf(modifiers);
        mods.add(mod);
        return new TextCell(character, fg, bg, mods);
    }

    public TextCell withoutModifier(SGR mod) {
        var mods = EnumSet.copyOf(modifiers);
        mods.remove(mod);
        return new TextCell(character, fg, bg, mods);
    }

    public boolean is(char c) {
        return character.length() == 1 && character.charAt(0) == c;
    }

    public boolean isDoubleWidth() {
        if (character.length() > 1) return true;
        char c = character.charAt(0);
        return Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_SYLLABLES
            || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }
}
