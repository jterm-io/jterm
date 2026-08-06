package io.jterm.window;

import io.jterm.animation.AnimatedBackground;
import io.jterm.animation.AnimationTimer;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

import java.util.List;

/**
 * A fullscreen background window that delegates rendering to an
 * {@link AnimatedBackground}. Background windows render behind all normal
 * windows, never receive focus, and never handle input.
 *
 * <p>The animation frame is advanced and rendered into a private off-screen
 * buffer by the {@link AnimationTimer} callback at the background's target
 * FPS. The {@link #draw} method only blits the cached buffer to the GUI's
 * graphics context — it never advances animation state. This decouples
 * animation pacing from the input event loop, so keystroke-triggered
 * redraws (which happen in {@link DefaultTextGUI#processInput}) do not
 * cause extra animation steps.</p>
 */
public class AnimatedBackgroundWindow extends WindowImpl {
    private final AnimatedBackground background;
    private AnimationTimer timer;
    private final TextGUI gui;
    private final boolean fullscreen;

    /** Off-screen buffer that holds the most recently rendered animation frame. */
    private volatile ScreenBuffer frameBuffer;
    /** Size of the last rendered frame, so draw() can detect stale buffers. */
    private volatile TerminalSize frameSize;

    /**
     * Create a fullscreen background window driven by the given animated background.
     *
     * @param background the animation to render
     * @param gui         the GUI this window belongs to
     */
    public AnimatedBackgroundWindow(AnimatedBackground background, TextGUI gui) {
        this(background, gui, true);
    }

    /**
     * Creates a background animation window. When {@code fullscreen} is false,
     * the caller must set bounds via {@link #setBounds(TerminalPosition, TerminalSize)}
     * (e.g. a thin strip at the bottom of the screen).
     */
    public AnimatedBackgroundWindow(AnimatedBackground background, TextGUI gui, boolean fullscreen) {
        super("animation-bg");
        this.background = background;
        this.gui = gui;
        this.fullscreen = fullscreen;
        if (fullscreen) {
            setHints(List.of(WindowHint.FULLSCREEN, WindowHint.NO_DECORATIONS, WindowHint.BACKGROUND));
        } else {
            setHints(List.of(WindowHint.NO_DECORATIONS, WindowHint.BACKGROUND));
        }
        getContents().setLayoutManager(null);
    }

    /**
     * Blits the cached animation frame to the GUI's graphics context.
     * Does NOT advance animation state — that happens only in the
     * {@link AnimationTimer} callback at the target FPS.
     */
    @Override
    public void draw(TextGraphics graphics) {
        var sz = getSize();
        var buf = frameBuffer;
        var fSize = frameSize;
        if (buf == null || fSize == null) {
            // No frame rendered yet — fill with black and let the first
            // timer tick populate the buffer.
            TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
            graphics.fillRectangle(0, 0, sz.columns(), sz.rows(), bg);
            return;
        }
        // Blit the cached frame, clamping to the intersection of the
        // buffer and graphics sizes.
        int maxRows = Math.min(fSize.rows(), sz.rows());
        int maxCols = Math.min(fSize.columns(), sz.columns());
        for (int r = 0; r < maxRows; r++) {
            for (int c = 0; c < maxCols; c++) {
                graphics.setCell(c, r, buf.getCell(c, r));
            }
        }
    }

    /**
     * Advances the animation state and renders a frame into the off-screen
     * buffer. Called by the {@link AnimationTimer} at the target FPS.
     * After rendering, requests a GUI refresh so {@link #draw} is called
     * to blit the new frame.
     */
    private void renderTick() {
        var sz = getSize();
        if (sz.columns() <= 0 || sz.rows() <= 0) return;
        // (Re)allocate the off-screen buffer if the size changed
        if (frameBuffer == null || frameSize == null
                || frameSize.columns() != sz.columns() || frameSize.rows() != sz.rows()) {
            TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
            frameBuffer = new ScreenBuffer(sz, bg);
            frameSize = sz;
        }
        var buf = frameBuffer;
        var g = new TextGraphics(buf);
        background.tick(System.nanoTime());
        background.renderFrame(g, sz);
        if (gui != null) {
            gui.requestRefresh();
        }
    }

    /**
     * Forwards resize to the background animation only when the size actually
     * changes. The GUI layout manager calls setBounds() on every screen update,
     * so forwarding unconditionally would reset animation state (e.g. VoronoiCells
     * recreating seeds) many times per second. By tracking the last forwarded
     * size, we ensure onResize() is called only on real terminal resizes.
     */
    private TerminalSize lastResizeSize;

    /**
     * Set the window position and size.
     *
     * @param position the terminal position
     * @param size the terminal dimensions
     */
    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        if (lastResizeSize == null
                || lastResizeSize.columns() != size.columns()
                || lastResizeSize.rows() != size.rows()) {
            lastResizeSize = size;
            background.onResize(size);
        }
    }

    /**
     * Start the animation timer and pre-render initial frames.
     */
    public void start() {
        background.start();
        // Pre-render several frames so the first visible frame has content
        // spread across the screen instead of clustered at initial positions.
        // tick() alone only updates timing — renderFrame() is what advances
        // star positions (advanceStar), so we must call renderTick() to
        // actually move stars before the first draw.
        for (int i = 0; i < 5; i++) {
            renderTick();
        }
        timer = new AnimationTimer(background.targetFps(), this::renderTick);
        timer.start();
    }

    /**
     * Stop the animation timer and the underlying background animation.
     */
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
        background.stop();
    }

    /**
     * Return the animated background rendered by this window.
     *
     * @return the animated background
     */
    public AnimatedBackground getBackground() {
        return background;
    }

    /**
     * Return the animation timer driving this window.
     *
     * @return the animation timer, or null if not started
     */
    public AnimationTimer getTimer() {
        return timer;
    }

    /** Visible for tests: returns the off-screen frame buffer (may be null before first tick). */
    ScreenBuffer getFrameBuffer() {
        return frameBuffer;
    }
}
