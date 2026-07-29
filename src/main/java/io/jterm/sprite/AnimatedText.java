package io.jterm.sprite;

import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.TextCell;

import java.util.List;

/**
 * Scrolling/morphing text animations for the terminal. Each instance supports
 * one of the following modes:
 *
 * <ul>
 *   <li>{@link Mode#TYPEWRITER} — reveal one character at a time.</li>
 *   <li>{@link Mode#SCROLL_LEFT} — scroll text right-to-left across a viewport.</li>
 *   <li>{@link Mode#SCROLL_RIGHT} — scroll text left-to-right across a viewport.</li>
 *   <li>{@link Mode#BLINK} — cycle the foreground color of fixed text through a list of color variants.</li>
 * </ul>
 *
 * <p>Animations are driven by calling {@link #tick(long)} with elapsed
 * milliseconds. State can be queried via {@link #getText()} (visible text
 * within the viewport) and rendered via {@link #render(TextGraphics, int, int)}.
 */
public class AnimatedText {

    /** Animation mode. */
    public enum Mode {
        TYPEWRITER, SCROLL_LEFT, SCROLL_RIGHT, BLINK
    }

    private final Mode mode;
    private final String text;
    private final int viewportWidth;
    private final List<Color> colors; // for BLINK
    private final int stepMs;          // ms per step

    // typewriter
    private int revealed = 0;

    // scroll
    private int scrollOffset = 0;

    // blink
    private int colorIndex = 0;

    private boolean complete = false;

    private AnimatedText(Mode mode, String text, int viewportWidth, List<Color> colors, int stepMs) {
        this.mode = mode;
        this.text = text == null ? "" : text;
        this.viewportWidth = Math.max(1, viewportWidth);
        this.colors = colors;
        this.stepMs = Math.max(1, stepMs);
        init();
    }

    private void init() {
        switch (mode) {
            case TYPEWRITER -> { revealed = 0; complete = text.isEmpty(); }
            case SCROLL_LEFT -> { scrollOffset = 0; complete = false; }
            case SCROLL_RIGHT -> { scrollOffset = -viewportWidth; complete = false; }
            case BLINK -> { colorIndex = 0; complete = false; }
        }
    }

    /**
     * Create a typewriter effect that reveals {@code text} one char at a time.
     *
     * @param text   the text to reveal
     * @param stepMs ms per character reveal
     */
    public static AnimatedText typewriter(String text, int stepMs) {
        return new AnimatedText(Mode.TYPEWRITER, text, Math.max(1, text == null ? 1 : text.length()),
                null, stepMs);
    }

    /**
     * Create a left-scrolling (right-to-left) text animation.
     *
     * @param text         text to scroll
     * @param viewportWidth width of the visible window in terminal cells
     * @param stepMs       ms per step (column shift)
     */
    public static AnimatedText scrollLeft(String text, int viewportWidth, int stepMs) {
        return new AnimatedText(Mode.SCROLL_LEFT, text, viewportWidth, null, stepMs);
    }

    /**
     * Create a right-scrolling (left-to-right) text animation.
     *
     * @param text         text to scroll
     * @param viewportWidth width of the visible window in terminal cells
     * @param stepMs       ms per step (column shift)
     */
    public static AnimatedText scrollRight(String text, int viewportWidth, int stepMs) {
        return new AnimatedText(Mode.SCROLL_RIGHT, text, viewportWidth, null, stepMs);
    }

    /**
     * Create a blinking text animation that cycles foreground colors.
     *
     * @param text    text to display
     * @param colors  list of colors to cycle through
     * @param stepMs  ms per color change
     */
    public static AnimatedText blink(String text, List<Color> colors, int stepMs) {
        return new AnimatedText(Mode.BLINK, text, Math.max(1, text == null ? 1 : text.length()),
                colors, stepMs);
    }

    /**
     * Advance the animation by {@code elapsedMs} milliseconds.
     */
    public void tick(long elapsedMs) {
        if (complete) return;
        long accumulated = elapsedMs;
        while (accumulated >= stepMs) {
            accumulated -= stepMs;
            step();
            if (complete) return;
        }
    }

    private void step() {
        switch (mode) {
            case TYPEWRITER -> {
                revealed++;
                if (revealed >= text.length()) {
                    revealed = text.length();
                    complete = true;
                }
            }
            case SCROLL_LEFT -> {
                scrollOffset++;
                // when fully scrolled past, wrap around
                if (scrollOffset >= text.length()) {
                    scrollOffset = -viewportWidth;
                }
            }
            case SCROLL_RIGHT -> {
                scrollOffset++;
                if (scrollOffset > text.length()) {
                    scrollOffset = -viewportWidth;
                }
            }
            case BLINK -> {
                if (colors != null && !colors.isEmpty()) {
                    colorIndex = (colorIndex + 1) % colors.size();
                }
            }
        }
    }

    /**
     * @return true when the animation has finished (only {@link Mode#TYPEWRITER} completes)
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Reset the animation to its starting state.
     */
    public void reset() {
        complete = false;
        init();
    }

    /**
     * Get the visible text within the viewport. For typewriter this is the
     * revealed substring; for scroll modes it is a viewport-width substring
     * of the full text at the current scroll offset; for blink it is the
     * full text.
     *
     * @return the visible text string (may include padding/truncation)
     */
    public String getText() {
        return switch (mode) {
            case TYPEWRITER -> text.substring(0, Math.min(revealed, text.length()));
            case SCROLL_LEFT, SCROLL_RIGHT -> scrollSlice();
            case BLINK -> text;
        };
    }

    private String scrollSlice() {
        // Build a viewport-width string by sampling text characters starting
        // at scrollOffset. Characters outside [0, text.length) are spaces.
        StringBuilder sb = new StringBuilder(viewportWidth);
        for (int i = 0; i < viewportWidth; i++) {
            int idx = scrollOffset + i;
            if (idx >= 0 && idx < text.length()) {
                sb.append(text.charAt(idx));
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    /**
     * Render the animation into the graphics buffer at position (col, row).
     * For blink mode, the current color variant is applied to every character.
     *
     * @param g    graphics buffer
     * @param col  column offset
     * @param row  row offset
     */
    public void render(TextGraphics g, int col, int row) {
        String visible = getText();
        Color fg = (mode == Mode.BLINK && colors != null && !colors.isEmpty())
                ? colors.get(colorIndex)
                : AnsiColor.DEFAULT;
        for (int i = 0; i < visible.length(); i++) {
            char ch = visible.charAt(i);
            if (ch == ' ') continue;
            g.setCell(col + i, row, new TextCell(ch, fg, AnsiColor.DEFAULT));
        }
    }

    /**
     * @return the active animation mode
     */
    public Mode getMode() {
        return mode;
    }
}