package io.jterm.window;

import io.jterm.animation.AnimatedBackground;
import io.jterm.animation.AnimationTimer;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;

import java.util.List;

/**
 * A fullscreen background window that delegates rendering to an
 * {@link AnimatedBackground}. Background windows render behind all normal
 * windows, never receive focus, and never handle input.
 */
public class AnimatedBackgroundWindow extends WindowImpl {
    private final AnimatedBackground background;
    private AnimationTimer timer;
    private final TextGUI gui;

    public AnimatedBackgroundWindow(AnimatedBackground background, TextGUI gui) {
        super("animation-bg");
        this.background = background;
        this.gui = gui;
        setHints(List.of(WindowHint.FULLSCREEN, WindowHint.NO_DECORATIONS, WindowHint.BACKGROUND));
        getContents().setLayoutManager(null);
    }

    @Override
    public void draw(TextGraphics graphics) {
        background.renderFrame(graphics, getSize());
    }

    public void start() {
        background.start();
        timer = new AnimationTimer(background.targetFps(), () -> gui.requestRefresh());
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }
        background.stop();
    }

    public AnimatedBackground getBackground() {
        return background;
    }

    public AnimationTimer getTimer() {
        return timer;
    }
}
