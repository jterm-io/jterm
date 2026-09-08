package io.jterm.sprite;

import io.jterm.graphics.TextGraphics;
import io.jterm.screen.AnsiArtRenderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents an animated sprite composed of one or more ANSI-art frames.
 * Each frame is a raw ANSI string (text with embedded SGR escape sequences).
 * <p>
 * Frame advancement is driven externally via {@link #advance(long)}, which
 * accumulates elapsed milliseconds and steps the frame index according to the
 * configured {@link LoopMode} and per-frame timing.
 *
 * <h2>Loop modes</h2>
 * <ul>
 *   <li>{@link LoopMode#ONCE} — play through all frames then stop at the last frame.</li>
 *   <li>{@link LoopMode#LOOP} — play through all frames then wrap back to frame 0.</li>
 *   <li>{@link LoopMode#PING_PONG} — play forward to the end then reverse back to the start, repeating.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Sprite sprite = new Sprite();
 * sprite.addFrame(AnsiArtRenderer.fromFile(Path.of("frame1.ans")));
 * sprite.addFrame(AnsiArtRenderer.fromFile(Path.of("frame2.ans")));
 * sprite.setFrameMs(100);
 * sprite.setLoopMode(Sprite.LoopMode.LOOP);
 * }</pre>
 */
public class Sprite {

    /** Default frame duration in milliseconds. */
    public static final int DEFAULT_FRAME_MS = 100;

    /** Loop behaviour for sprite animation. */
    public enum LoopMode {
        /** Play all frames once, then stop at the last frame. */
        ONCE,
        /** Play all frames, then wrap to the first frame. */
        LOOP,
        /** Play forward to the end, then backward to the start, repeating. */
        PING_PONG
    }

    private final List<String> frames = new CopyOnWriteArrayList<>();
    private volatile int frameMs = DEFAULT_FRAME_MS;
    private volatile LoopMode loopMode = LoopMode.LOOP;

    private volatile int currentIndex = 0;
    private volatile int direction = 1; // 1 = forward, -1 = backward (ping-pong)
    private volatile long accumulatedMs = 0;
    private volatile boolean finished = false;

    private volatile int width = 0;
    private volatile int height = 0;

    /**
     * Create an empty sprite with {@link LoopMode#LOOP} and default frame timing.
     */
    public Sprite() {}

    /**
     * Create a sprite pre-populated with the given frame strings.
     *
     * @param frames one or more raw ANSI strings, one per frame
     */
    public Sprite(List<String> frames) {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("frames must not be null or empty");
        }
        for (String f : frames) {
            addFrame(f);
        }
    }

    /**
     * Append a frame (raw ANSI string) to the sprite.
     *
     * @param ansiContent raw ANSI art string; must not be null
     */
    public void addFrame(String ansiContent) {
        if (ansiContent == null) {
            throw new NullPointerException("ansiContent must not be null");
        }
        frames.add(ansiContent);
        updateSize(ansiContent);
    }

    private void updateSize(String ansiContent) {
        // Compute visible width and height of an ANSI frame.
        int w = 0;
        int h = 0;
        int col = 0;
        for (int i = 0; i < ansiContent.length(); i++) {
            char ch = ansiContent.charAt(i);
            if (ch == 0x1B && i + 1 < ansiContent.length() && ansiContent.charAt(i + 1) == '[') {
                int j = i + 2;
                while (j < ansiContent.length()) {
                    char cj = ansiContent.charAt(j);
                    if (cj >= 0x40 && cj <= 0x7E) break;
                    j++;
                }
                i = j; // skip escape (loop increment will move past final byte)
                continue;
            }
            if (ch == '\n') {
                w = Math.max(w, col);
                col = 0;
                h++;
                continue;
            }
            if (ch == '\r') {
                col = 0;
                continue;
            }
            col++;
        }
        // Account for last partial line
        if (col > 0) {
            w = Math.max(w, col);
            h++;
        }
        if (w > this.width) this.width = w;
        if (h > this.height) this.height = h;
    }

    /**
     * Load a sprite from multiple ANSI art files (one per frame).
     *
     * @param paths list of paths to .ans files, in frame order
     * @return a new sprite loaded with those frames
     * @throws IOException if any file cannot be read
     */
    public static Sprite fromFiles(List<Path> paths) throws IOException {
        if (paths == null || paths.isEmpty()) {
            throw new IllegalArgumentException("paths must not be null or empty");
        }
        var sprite = new Sprite();
        for (Path p : paths) {
            sprite.addFrame(Files.readString(p));
        }
        return sprite;
    }

    /**
     * Advance the sprite animation by {@code elapsedMs} milliseconds.
     * Steps through frames based on {@link #getFrameMs()}.
     *
     * @param elapsedMs milliseconds elapsed since the last call
     */
    public void advance(long elapsedMs) {
        if (finished || frames.isEmpty()) return;
        accumulatedMs += elapsedMs;
        while (accumulatedMs >= frameMs) {
            accumulatedMs -= frameMs;
            step();
            if (finished) {
                accumulatedMs = 0;
                return;
            }
        }
    }

    private void step() {
        if (frames.isEmpty()) return;
        int next = currentIndex + direction;
        switch (loopMode) {
            case ONCE -> {
                if (next >= frames.size()) {
                    currentIndex = frames.size() - 1;
                    finished = true;
                } else if (next < 0) {
                    currentIndex = 0;
                    finished = true;
                } else {
                    currentIndex = next;
                }
            }
            case LOOP -> {
                if (next >= frames.size()) {
                    currentIndex = 0;
                } else if (next < 0) {
                    currentIndex = frames.size() - 1;
                } else {
                    currentIndex = next;
                }
            }
            case PING_PONG -> {
                if (next >= frames.size()) {
                    direction = -1;
                    currentIndex = frames.size() - 2;
                    if (currentIndex < 0) currentIndex = 0;
                } else if (next < 0) {
                    direction = 1;
                    currentIndex = Math.min(1, frames.size() - 1);
                } else {
                    currentIndex = next;
                }
            }
        }
    }

    /**
     * Reset the sprite to its first frame and clear the finished flag.
     */
    public void reset() {
        currentIndex = 0;
        direction = 1;
        accumulatedMs = 0;
        finished = false;
    }

    /**
     * Returns the index of the frame currently displayed.
     *
     * @return current frame index
     */
    public int getCurrentFrameIndex() {
        return currentIndex;
    }

    /**
     * Returns the raw ANSI text of the frame currently displayed.
     *
     * @return raw ANSI string for the current frame
     */
    public String getCurrentFrame() {
        if (frames.isEmpty()) return "";
        return frames.get(currentIndex);
    }

    /**
     * Returns the total number of frames in this sprite.
     *
     * @return number of frames
     */
    public int getFrameCount() {
        return frames.size();
    }

    /**
     * Returns the configured per-frame duration.
     *
     * @return configured per-frame duration in ms
     */
    public int getFrameMs() {
        return frameMs;
    }

    /**
     * Set the per-frame duration in ms.
     *
     * @param frameMs duration; clamped to >= 1
     */
    public void setFrameMs(int frameMs) {
        this.frameMs = Math.max(1, frameMs);
    }

    /**
     * Returns the current loop behavior.
     *
     * @return the current loop mode
     */
    public LoopMode getLoopMode() {
        return loopMode;
    }

    /**
     * Set the loop mode. Resetting to a new mode does not reset the frame index;
     * call {@link #reset()} to restart from the first frame.
     *
     * @param loopMode loop mode to use; in {@link LoopMode#ONCE} mode the sprite
     *                 stops advancing after the final frame
     */
    public void setLoopMode(LoopMode loopMode) {
        this.loopMode = loopMode;
        // If switching to a mode where finished state is invalid, clear it
        if (loopMode != LoopMode.ONCE) {
            finished = false;
        }
    }

    /**
     * Returns the sprite width in terminal cells.
     *
     * @return sprite width in terminal cells (max across all frames)
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the sprite height in terminal cells.
     *
     * @return sprite height in terminal cells (max across all frames)
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns whether a {@link LoopMode#ONCE} sprite has played through.
     *
     * @return true when {@link LoopMode#ONCE} has reached the final frame
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Render the current frame into the supplied graphics buffer at the
     * specified offset. The frame's raw ANSI string is parsed by
     * {@link AnsiArtRenderer}.
     *
     * @param g        graphics buffer
     * @param startCol  column offset
     * @param startRow  row offset
     */
    public void render(TextGraphics g, int startCol, int startRow) {
        AnsiArtRenderer.render(g, getCurrentFrame(), startCol, startRow);
    }
}