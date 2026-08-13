package io.jterm.window;

import io.jterm.animation.AnimatedBorderEffect;
import io.jterm.animation.AnimationTimer;
import io.jterm.animation.BorderContext;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.widget.AnimatedBorder;
import io.jterm.widget.Border.BorderStyle;
import io.jterm.widget.Panel;

/**
 * A window that wraps its content in an {@link AnimatedBorder}, providing
 * opt-in animated border effects for any window.
 *
 * <p>The animation starts when the window is opened (via {@link #open(DefaultTextGUI)})
 * and stops when the window is closed (via {@link #close()}). The timer increments
 * a frame counter and requests a GUI refresh on each tick; the effect's
 * {@code update()} is called during {@link #draw(TextGraphics)}.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * var window = new AnimatedBorderWindow("Title", AnimationFactory.sparkleCorners());
 * var window = new AnimatedBorderWindow("Title", BorderStyle.DOUBLE_LINE, AnimationFactory.marchingAnts());
 * </pre>
 *
 * <p>Switch effects at runtime with {@link #setEffect(AnimatedBorderEffect)}.</p>
 *
 * <p>The window title, if non-empty, is rendered inside the top border line
 * (similar to a titled border). With {@link WindowHint#NO_DECORATIONS}, the
 * animated border serves as the sole frame around the content.</p>
 */
public class AnimatedBorderWindow extends WindowImpl {

    private final BorderStyle borderStyle;
    private final Panel contentPanel;
    private AnimatedBorderEffect effect;
    private AnimatedBorder animatedBorder;
    private AnimationTimer refreshTimer;
    private DefaultTextGUI gui;
    private volatile long frameCounter;

    /**
     * Creates an animated border window with the given title and effect.
     *
     * @param title  the window title (rendered in the top border line)
     * @param effect the animation effect to apply to the border
     */
    public AnimatedBorderWindow(String title, AnimatedBorderEffect effect) {
        this(title, BorderStyle.SINGLE_LINE, effect);
    }

    /**
     * Creates an animated border window with the given title, border style, and effect.
     *
     * @param title  the window title (rendered in the top border line)
     * @param style  the border style (SINGLE_LINE, DOUBLE_LINE, etc.)
     * @param effect the animation effect to apply to the border
     */
    public AnimatedBorderWindow(String title, BorderStyle style, AnimatedBorderEffect effect) {
        super(title);
        this.borderStyle = style;
        this.contentPanel = new Panel();
        this.effect = effect;
        this.animatedBorder = new AnimatedBorder(contentPanel, style, effect);
        this.frameCounter = 0;
        // Always use NO_DECORATIONS — the animated border IS the frame
        setHints(java.util.List.of(WindowHint.NO_DECORATIONS));
    }

    /**
     * Creates an animated border window with an empty title and the given effect.
     *
     * @param effect the animation effect to apply to the border
     */
    public AnimatedBorderWindow(AnimatedBorderEffect effect) {
        this("", effect);
    }

    /**
     * Returns the animated border component wrapping the content.
     *
     * @return the animated border
     */
    public AnimatedBorder getAnimatedBorder() {
        return animatedBorder;
    }

    /**
     * Returns the inner content panel where child components should be added.
     * This is the panel <em>inside</em> the animated border.
     *
     * @return the content panel inside the animated border
     */
    @Override
    public Panel getContents() {
        return contentPanel;
    }

    /**
     * Switches the animation effect at runtime. Resets the frame counter.
     * The new effect takes effect on the next draw cycle.
     *
     * @param effect the new animation effect
     */
    public void setEffect(AnimatedBorderEffect effect) {
        this.effect = effect;
        this.frameCounter = 0;
        // Re-create the animated border with the new effect
        this.animatedBorder = new AnimatedBorder(contentPanel, borderStyle, effect);
    }

    /**
     * Returns the current animation effect.
     *
     * @return the current effect
     */
    public AnimatedBorderEffect getEffect() {
        return effect;
    }

    /**
     * Starts the animation timer. Called automatically by {@link #open(DefaultTextGUI)}.
     */
    @Override
    public void open(DefaultTextGUI gui) {
        this.gui = gui;
        startAnimation();
    }

    /**
     * Stops the animation timer. Called automatically when the window is removed.
     */
    @Override
    public void close() {
        stopAnimation();
    }

    /**
     * Returns the preferred size: content preferred size + 2 for the border.
     */
    @Override
    public TerminalSize getPreferredSize() {
        var contentPS = contentPanel.getPreferredSize();
        return new TerminalSize(contentPS.columns() + 2, contentPS.rows() + 2);
    }

    /**
     * Draws the window with the animated border effect.
     *
     * <p>Renders the animated border as the window frame, with the title
     * (if non-empty) embedded in the top border line. Each draw call invokes
     * the effect's {@code update()} method with the current frame counter.</p>
     */
    @Override
    public void draw(TextGraphics graphics) {
        var size = getSize();
        if (size.columns() < 2 || size.rows() < 2) return;

        var theme = ThemeManager.active();

        // Fill background
        if (!getHints().contains(WindowHint.TRANSPARENT)) {
            var bgCell = new TextCell(' ', theme.foreground(), theme.background());
            graphics.fillRectangle(0, 0, size.columns(), size.rows(), bgCell);
        }

        // Update the border context with the current frame's effect
        animatedBorder.setBounds(TerminalPosition.TOP_LEFT, size);
        var ctx = animatedBorder.createContext();
        effect.update(frameCounter, ctx);

        // Render the animated border
        animatedBorder.drawWithBorderContext(graphics, ctx);

        // Draw title in the top border line (after the border is rendered)
        var title = getTitle();
        if (title != null && !title.isEmpty() && size.columns() > title.length() + 4) {
            var titleStyle = new TextCell(' ', theme.foreground(), theme.background());
            graphics.drawString(2, 0, " " + title + " ", titleStyle);
        }
    }

    private void startAnimation() {
        if (refreshTimer != null && refreshTimer.isRunning()) return;
        refreshTimer = new AnimationTimer(10, this::onFrame);
        refreshTimer.start();
    }

    private void stopAnimation() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
    }

    private void onFrame() {
        frameCounter++;
        if (gui != null) {
            gui.requestRefresh();
        }
    }
}