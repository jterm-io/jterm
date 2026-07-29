package io.jterm.sprite;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Renders sprites and animations to a {@link TextGraphics} buffer with
 * timer-driven frame updates. Multiple animations may run concurrently on
 * dedicated virtual threads; a {@link AnimationHandle} is returned for each
 * running animation so it can be stopped individually or via
 * {@link #stopAll()}.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * SpriteRenderer renderer = new SpriteRenderer(graphics, terminalSize);
 * // One-shot animation
 * renderer.playOnce(sprite, 10, 5, () -> System.out.println("done"));
 * // Loop animation; stop later
 * AnimationHandle handle = renderer.playLoop(sprite, 30, 0);
 * Thread.sleep(2000);
 * handle.stop();
 * }</pre>
 *
 * <p>This renderer is thread-safe: the internal list of running handles is a
 * {@link CopyOnWriteArrayList} and each animation runs on its own virtual
 * thread. The renderer clears the previous frame's region before drawing the
 * next frame to avoid stale artifacts.
 */
public class SpriteRenderer {

    private final TextGraphics graphics;
    private final TerminalSize size;
    private final CopyOnWriteArrayList<AnimationHandle> activeHandles = new CopyOnWriteArrayList<>();

    /**
     * Construct a renderer bound to a graphics buffer and terminal size.
     *
     * @param graphics the graphics buffer to draw into
     * @param size     the terminal size (used for clipping)
     */
    public SpriteRenderer(TextGraphics graphics, TerminalSize size) {
        this.graphics = graphics;
        this.size = size;
    }

    /**
     * Render the sprite's current frame at position (col, row). Clears any
     * previously drawn region tracked by this renderer for the same sprite
     * position when the position changes.
     *
     * @param sprite the sprite to render
     * @param col    column offset
     * @param row    row offset
     */
    public void render(Sprite sprite, int col, int row) {
        clearRegionIfNeeded(sprite, col, row);
        sprite.render(graphics, col, row);
        rememberRegion(sprite, col, row);
    }

    private int lastCol = Integer.MIN_VALUE;
    private int lastRow = Integer.MIN_VALUE;
    private int lastWidth = 0;
    private int lastHeight = 0;
    private Sprite lastSprite = null;

    private void clearRegionIfNeeded(Sprite sprite, int col, int row) {
        // If the same sprite is being rendered at a new position, clear the
        // old region first.
        if (lastSprite == sprite && (col != lastCol || row != lastRow)) {
            clearRegion(lastCol, lastRow, lastWidth, lastHeight);
        }
    }

    private void rememberRegion(Sprite sprite, int col, int row) {
        lastSprite = sprite;
        lastCol = col;
        lastRow = row;
        lastWidth = sprite.getWidth();
        lastHeight = sprite.getHeight();
    }

    private void clearRegion(int col, int row, int w, int h) {
        if (w <= 0 || h <= 0) return;
        var blank = new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT);
        for (int r = row; r < row + h && r < size.rows() && r >= 0; r++) {
            for (int c = col; c < col + w && c < size.columns() && c >= 0; c++) {
                graphics.setCell(c, r, blank);
            }
        }
    }

    /**
     * Play a sprite animation once at position (col, row) and fire
     * {@code callback} when the animation finishes (the sprite reaches its
     * last frame in {@link Sprite.LoopMode#ONCE}).
     *
     * @param sprite   sprite to animate (must have {@link Sprite.LoopMode#ONCE})
     * @param col      column offset
     * @param row      row offset
     * @param callback optional callback fired after animation; may be null
     * @return a handle that can stop the animation early
     */
    public AnimationHandle playOnce(Sprite sprite, int col, int row, Runnable callback) {
        sprite.setLoopMode(Sprite.LoopMode.ONCE);
        sprite.reset();
        var handle = new AnimationHandle(sprite, col, row, callback, this);
        activeHandles.add(handle);
        handle.start();
        return handle;
    }

    /**
     * Play a sprite animation in a loop at position (col, row).
     *
     * @param sprite sprite to animate
     * @param col    column offset
     * @param row    row offset
     * @return a handle that can stop the loop
     */
    public AnimationHandle playLoop(Sprite sprite, int col, int row) {
        if (sprite.getLoopMode() != Sprite.LoopMode.LOOP
                && sprite.getLoopMode() != Sprite.LoopMode.PING_PONG) {
            sprite.setLoopMode(Sprite.LoopMode.LOOP);
        }
        sprite.reset();
        var handle = new AnimationHandle(sprite, col, row, null, this);
        activeHandles.add(handle);
        handle.start();
        return handle;
    }

    /**
     * Stop all running animations managed by this renderer.
     */
    public void stopAll() {
        for (var h : activeHandles) {
            h.stop();
        }
        activeHandles.clear();
    }

    void removeHandle(AnimationHandle handle) {
        activeHandles.remove(handle);
    }

    /**
     * Get the graphics buffer.
     *
     * @return graphics buffer
     */
    public TextGraphics getGraphics() {
        return graphics;
    }

    /**
     * Get the terminal size.
     *
     * @return terminal size
     */
    public TerminalSize getSize() {
        return size;
    }

    /**
     * Handle for a running animation. Use {@link #stop()} to stop the animation
     * and release its resources. {@link #isRunning()} reports the current state.
     */
    public static class AnimationHandle {
        private final Sprite sprite;
        private final int col;
        private final int row;
        private final Runnable callback;
        private final SpriteRenderer renderer;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private Thread thread;

        AnimationHandle(Sprite sprite, int col, int row, Runnable callback, SpriteRenderer renderer) {
            this.sprite = sprite;
            this.col = col;
            this.row = row;
            this.callback = callback;
            this.renderer = renderer;
        }

        /**
         * Start the animation thread. Called internally.
         */
        void start() {
            if (!running.compareAndSet(false, true)) return;
            thread = Thread.ofVirtual().name("sprite-anim").start(() -> {
                long lastNanos = System.nanoTime();
                while (running.get()) {
                    long now = System.nanoTime();
                    long deltaMs = (now - lastNanos) / 1_000_000L;
                    lastNanos = now;
                    sprite.advance(deltaMs);
                    synchronized (renderer.graphics) {
                        sprite.render(renderer.graphics, col, row);
                    }
                    if (sprite.isFinished()) {
                        running.set(false);
                        break;
                    }
                    try {
                        Thread.sleep(Math.max(16, sprite.getFrameMs() / 2));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                if (callback != null) {
                    try {
                        callback.run();
                    } catch (RuntimeException ignored) {
                        // Callback failures should not propagate.
                    }
                }
                renderer.removeHandle(this);
            });
        }

        /**
         * Stop the animation.
         */
        public void stop() {
            running.set(false);
            if (thread != null) {
                thread.interrupt();
            }
        }

        /**
         * @return true if the animation is still running
         */
        public boolean isRunning() {
            return running.get();
        }
    }
}