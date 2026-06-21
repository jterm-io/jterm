package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.widget.AbstractComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Types ASCII/ANSI art onto the screen character-by-character, mimicking a
 * classic BBS banner "typing in" on connection. Once complete it can fire a
 * callback and optionally keep a blinking cursor at the final position.
 *
 * <p>Implements both {@link AnimatedBackground} and
 * {@link io.jterm.widget.Component} so it can be used as a fullscreen
 * background layer or as a panel component.
 */
public class TypewriterEffect extends AbstractComponent implements AnimatedBackground {
    private static final int MIN_CHARS_PER_FRAME = 1;
    private static final int MAX_CHARS_PER_FRAME = 256;
    private static final int MIN_LINE_DELAY = 0;
    private static final int MAX_LINE_DELAY = 60;
    private static final int MIN_TARGET_FPS = 1;
    private static final int MAX_TARGET_FPS = 60;

    private final List<String> lines;
    private final List<Runnable> completeCallbacks = new CopyOnWriteArrayList<>();
    private volatile Color color;
    private volatile Color backgroundColor;
    private volatile int charsPerFrame = 2;
    private volatile int lineDelay = 0;
    private volatile int targetFps = 15;
    private volatile boolean showCursor = true;
    private volatile boolean cursorVisible = true;
    private volatile long lastCursorToggleNs;
    private volatile boolean running = false;

    private int currentLine = 0;
    private int currentCol = 0;
    private int lineDelayCounter = 0;
    private boolean complete = false;
    private boolean callbackFired = false;

    private TerminalSize preferredSize;

    public TypewriterEffect(List<String> lines, Color color, Color backgroundColor) {
        this.lines = List.copyOf(lines != null ? lines : List.of());
        this.color = color != null ? color : AnsiColor.DEFAULT;
        this.backgroundColor = backgroundColor != null ? backgroundColor : AnsiColor.DEFAULT;
        this.preferredSize = computePreferredSize();
    }

    public TypewriterEffect(String text, Color color, Color backgroundColor) {
        this(splitLines(text), color, backgroundColor);
    }

    public TypewriterEffect(List<String> lines) {
        this(lines, ThemeManager.active().foreground(), ThemeManager.active().background());
    }

    public TypewriterEffect(String text) {
        this(text, ThemeManager.active().foreground(), ThemeManager.active().background());
    }

    private static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String raw : text.split("\\R", -1)) {
            result.add(raw);
        }
        return result;
    }

    private TerminalSize computePreferredSize() {
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, line.length());
        }
        return new TerminalSize(Math.max(width, 1), Math.max(lines.size(), 1));
    }

    public List<String> getLines() {
        return lines;
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public int getCurrentCol() {
        return currentCol;
    }

    public int getCharsPerFrame() {
        return charsPerFrame;
    }

    public void setCharsPerFrame(int charsPerFrame) {
        this.charsPerFrame = Math.max(MIN_CHARS_PER_FRAME, Math.min(MAX_CHARS_PER_FRAME, charsPerFrame));
    }

    public int getLineDelay() {
        return lineDelay;
    }

    public void setLineDelay(int lineDelay) {
        this.lineDelay = Math.max(MIN_LINE_DELAY, Math.min(MAX_LINE_DELAY, lineDelay));
    }

    public int getTargetFps() {
        return targetFps;
    }

    public void setTargetFps(int targetFps) {
        this.targetFps = Math.max(MIN_TARGET_FPS, Math.min(MAX_TARGET_FPS, targetFps));
    }

    public boolean isCursorShown() {
        return showCursor;
    }

    public void setShowCursor(boolean showCursor) {
        this.showCursor = showCursor;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color != null ? color : AnsiColor.DEFAULT;
        invalidate();
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor != null ? backgroundColor : AnsiColor.DEFAULT;
        invalidate();
    }

    public boolean isComplete() {
        return complete;
    }

    public void onComplete(Runnable callback) {
        if (callback != null) {
            completeCallbacks.add(callback);
        }
    }

    public void reset() {
        currentLine = 0;
        currentCol = 0;
        lineDelayCounter = 0;
        complete = false;
        callbackFired = false;
        cursorVisible = true;
        lastCursorToggleNs = 0;
    }

    @Override
    protected TerminalSize calculatePreferredSize() {
        return preferredSize;
    }

    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        onResize(size);
    }

    @Override
    public void onResize(TerminalSize newSize) {
        // No internal buffer to resize; rendering derives everything from size and lines.
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        renderFrame(graphics, getSize());
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        if (size.columns() <= 0 || size.rows() <= 0) {
            return;
        }

        Color bg = backgroundColor;
        if (bg == AnsiColor.DEFAULT) {
            bg = ThemeManager.active().background();
        }
        Color fg = color;
        if (fg == AnsiColor.DEFAULT) {
            fg = ThemeManager.active().foreground();
        }

        graphics.fillRectangle(0, 0, size.columns(), size.rows(),
                new TextCell(' ', fg, bg));

        if (lines.isEmpty()) {
            return;
        }

        int contentWidth = 0;
        for (String line : lines) {
            contentWidth = Math.max(contentWidth, line.length());
        }
        int contentHeight = lines.size();
        int startCol = (size.columns() - contentWidth) / 2;
        int startRow = (size.rows() - contentHeight) / 2;

        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            String line = lines.get(lineIdx);
            int row = startRow + lineIdx;
            if (row < 0 || row >= size.rows()) {
                continue;
            }

            int charsToDraw;
            if (lineIdx < currentLine) {
                charsToDraw = line.length();
            } else if (lineIdx == currentLine && !complete) {
                charsToDraw = Math.min(currentCol, line.length());
            } else if (lineIdx == currentLine) {
                charsToDraw = line.length();
            } else {
                continue;
            }

            for (int colIdx = 0; colIdx < charsToDraw; colIdx++) {
                int col = startCol + colIdx;
                if (col < 0 || col >= size.columns()) {
                    continue;
                }
                char c = line.charAt(colIdx);
                graphics.setCell(col, row, new TextCell(c, fg, bg));
            }

            if (showCursor && lineIdx == currentLine && !complete) {
                int cursorCol = startCol + charsToDraw;
                if (cursorVisible && cursorCol >= 0 && cursorCol < size.columns()
                        && row >= 0 && row < size.rows()) {
                    graphics.setCell(cursorCol, row, new TextCell('\u2588', fg, bg));
                }
            }
        }
    }

    /**
     * Advances the typewriter by one animation frame. This is the intended
     * integration point for an animation loop (e.g. {@link AnimationManager}).
     *
     * @param nowNanos monotonically increasing time in nanoseconds
     */
    public void tick(long nowNanos) {
        if (complete) {
            updateCursorBlink(nowNanos);
            return;
        }

        if (lines.isEmpty()) {
            complete = true;
            fireCompleteCallbacks();
            return;
        }

        if (lineDelayCounter > 0) {
            lineDelayCounter--;
            updateCursorBlink(nowNanos);
            return;
        }

        if (currentLine >= lines.size()) {
            complete = true;
            fireCompleteCallbacks();
            updateCursorBlink(nowNanos);
            return;
        }

        String line = lines.get(currentLine);
        int remaining = line.length() - currentCol;
        int advance = Math.min(charsPerFrame, remaining);
        currentCol += advance;
        if (currentCol >= line.length()) {
            currentLine++;
            currentCol = 0;
            lineDelayCounter = lineDelay;
            if (currentLine >= lines.size()) {
                complete = true;
                fireCompleteCallbacks();
            }
        }
        updateCursorBlink(nowNanos);
    }

    private void updateCursorBlink(long nowNanos) {
        if (lastCursorToggleNs == 0) {
            lastCursorToggleNs = nowNanos;
            return;
        }
        long blinkNs = 500_000_000L;
        long elapsed = nowNanos - lastCursorToggleNs;
        if (elapsed >= blinkNs) {
            cursorVisible = !cursorVisible;
            lastCursorToggleNs = nowNanos;
        }
    }

    private void fireCompleteCallbacks() {
        if (callbackFired) {
            return;
        }
        callbackFired = true;
        for (Runnable callback : completeCallbacks) {
            try {
                callback.run();
            } catch (RuntimeException e) {
                // Callback failures should not break the animation loop.
            }
        }
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int targetFps() {
        return targetFps;
    }
}
