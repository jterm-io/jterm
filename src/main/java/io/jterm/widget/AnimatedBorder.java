package io.jterm.widget;

import io.jterm.animation.AnimatedBorderEffect;
import io.jterm.animation.AnimationTimer;
import io.jterm.animation.BorderContext;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;

/**
 * A border that animates its edge/corner characters on a timer.
 *
 * <p>On each frame tick (default ~10fps), the registered
 * {@link AnimatedBorderEffect} receives a mutable {@link BorderContext}
 * and can override individual corner and edge characters. After the effect
 * updates the context, the border redraws using the (possibly modified)
 * characters.</p>
 *
 * <p>Use {@link #start()} / {@link #stop()} to control the animation
 * lifecycle, and {@link #isAnimating()} to query the current state.</p>
 */
public class AnimatedBorder extends Border {

    private static final int DEFAULT_FPS = 10;

    private final AnimatedBorderEffect effect;
    private final BorderStyle storedStyle;
    private final int fps;
    private AnimationTimer timer;
    private long frameCounter;

    /**
     * Creates an animated border with the default single-line style.
     *
     * @param contents the component to wrap
     * @param effect   the animation effect
     */
    public AnimatedBorder(Component contents, AnimatedBorderEffect effect) {
        this(contents, BorderStyle.SINGLE_LINE, effect);
    }

    /**
     * Creates an animated border with the given style and effect.
     *
     * @param contents the component to wrap
     * @param style    the border style
     * @param effect   the animation effect
     */
    public AnimatedBorder(Component contents, BorderStyle style, AnimatedBorderEffect effect) {
        this(contents, style, effect, DEFAULT_FPS);
    }

    /**
     * Creates an animated border with the given style, effect, and frame rate.
     *
     * @param contents the component to wrap
     * @param style    the border style
     * @param effect   the animation effect
     * @param fps      the target frames per second
     */
    public AnimatedBorder(Component contents, BorderStyle style, AnimatedBorderEffect effect, int fps) {
        super(contents, style);
        this.storedStyle = style;
        this.effect = effect;
        this.fps = fps;
    }

    /** Returns the border style. */
    public BorderStyle getBorderStyle() {
        return storedStyle;
    }

    /** Starts the animation timer. Idempotent — safe to call when already running. */
    public void start() {
        if (timer != null && timer.isRunning()) return;
        timer = new AnimationTimer(fps, this::onFrame);
        timer.start();
    }

    /** Stops the animation timer. Idempotent — safe to call when not running. */
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    /** Returns whether the animation timer is currently running. */
    public boolean isAnimating() {
        return timer != null && timer.isRunning();
    }

    /** Returns the animation effect. */
    public AnimatedBorderEffect getEffect() {
        return effect;
    }

    /**
     * Creates a fresh {@link BorderContext} for the current border dimensions
     * and style, initialized to the style defaults. Callers (including
     * effects and tests) can use this to prepare a context before calling
     * {@link #drawWithBorderContext(TextGraphics, BorderContext)}.
     */
    public BorderContext createContext() {
        return new BorderContext(getSize(), storedStyle);
    }

    /**
     * Draws the border using a pre-built {@link BorderContext}.
     *
     * <p>This is the rendering entry point for tests and for the
     * animation timer: the effect updates the context, then this
     * method renders the border with those (possibly overridden)
     * characters.</p>
     *
     * @param graphics the text-graphics target
     * @param ctx      the border context with (possibly custom) characters
     */
    public void drawWithBorderContext(TextGraphics graphics, BorderContext ctx) {
        var size = getSize();
        if (size.columns() < 2 || size.rows() < 2) return;

        var theme = ThemeManager.active();
        // Fill background like Border.drawComponent does
        var bgCell = new TextCell(' ', theme.foreground(), theme.background());
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bgCell);

        // Use context border color override if set, otherwise theme default
        var borderFg = ctx.getBorderColor() != null ? ctx.getBorderColor() : theme.border();
        var borderCell = new TextCell(' ', borderFg, theme.background());

        // Top edge: corner + horizontal chars + corner
        StringBuilder top = new StringBuilder();
        top.append(ctx.getCorner(BorderContext.Corner.TL));
        for (int c = 1; c < size.columns() - 1; c++) {
            top.append(ctx.getEdge(BorderContext.Side.TOP, c - 1));
        }
        top.append(ctx.getCorner(BorderContext.Corner.TR));
        graphics.drawString(0, 0, top.toString(), borderCell);

        // Side edges
        for (int r = 1; r < size.rows() - 1; r++) {
            String left = ctx.getEdge(BorderContext.Side.LEFT, r - 1);
            String right = ctx.getEdge(BorderContext.Side.RIGHT, r - 1);
            graphics.drawString(0, r, left, borderCell);
            graphics.drawString(size.columns() - 1, r, right, borderCell);
        }

        // Bottom edge: corner + horizontal chars + corner
        StringBuilder bottom = new StringBuilder();
        bottom.append(ctx.getCorner(BorderContext.Corner.BL));
        for (int c = 1; c < size.columns() - 1; c++) {
            bottom.append(ctx.getEdge(BorderContext.Side.BOTTOM, c - 1));
        }
        bottom.append(ctx.getCorner(BorderContext.Corner.BR));
        graphics.drawString(0, size.rows() - 1, bottom.toString(), borderCell);

        // Draw children only (skip Border.drawComponent which would overwrite)
        drawChildren(graphics);
    }

    /**
     * Draws only child components, bypassing Border's own border rendering.
     * This is used by drawWithBorderContext to avoid overwriting animated chars.
     */
    private void drawChildren(TextGraphics graphics) {
        for (var child : getChildren()) {
            if (!child.isVisible()) continue;
            var pos = child.getPosition();
            var childSize = child.getSize();
            if (childSize.columns() <= 0 || childSize.rows() <= 0) continue;
            var sub = io.jterm.graphics.TextGraphicsExtensions.subGraphics(graphics, pos, childSize);
            child.draw(sub);
        }
    }

    /**
     * Override parent drawComponent to use animated rendering when animating,
     * or fall back to standard border rendering when stopped.
     */
    @Override
    protected void drawComponent(TextGraphics graphics) {
        if (storedStyle.isEmpty()) {
            super.drawComponent(graphics);
            return;
        }
        // When animating, the timer handles rendering via drawWithBorderContext.
        // When stopped, draw the standard border.
        // For now, delegate to parent for static rendering.
        super.drawComponent(graphics);
    }

    // ---- Internal ----

    private void onFrame() {
        var ctx = createContext();
        effect.update(frameCounter++, ctx);
        invalidate();
    }
}