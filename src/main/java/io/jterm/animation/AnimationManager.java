package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowImpl;
import io.jterm.window.WindowHint;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Drives one or more {@link AnimatedBackground} instances at a configurable
 * frame rate and asks the GUI to refresh when they change.
 *
 * <p>Attach an {@code AnimationManager} to a {@link DefaultTextGUI} and call
 * {@link #tick(long)} from the same render loop that already calls
 * {@link DefaultTextGUI#updateScreen()}. The manager throttles updates to the
 * target FPS and requests a screen refresh only when a frame actually advances.
 */
public class AnimationManager {
    private static final int MIN_FPS = 1;
    private static final int MAX_FPS = 60;
    private static final long NS_PER_MS = 1_000_000L;

    private final DefaultTextGUI gui;
    private final CopyOnWriteArrayList<AnimatedBackground> backgrounds = new CopyOnWriteArrayList<>();
    private volatile int targetFps;
    private volatile boolean running = true;

    private long lastTickNanos = -1;
    private long accumulatedNs;

    public AnimationManager(DefaultTextGUI gui) {
        this(gui, 60);
    }

    public AnimationManager(DefaultTextGUI gui, int targetFps) {
        this.gui = gui;
        this.targetFps = Math.max(MIN_FPS, Math.min(MAX_FPS, targetFps));
    }

    public int getTargetFps() {
        return targetFps;
    }

    public void setTargetFps(int fps) {
        this.targetFps = Math.max(MIN_FPS, Math.min(MAX_FPS, fps));
    }

    public void register(AnimatedBackground background) {
        backgrounds.addIfAbsent(background);
    }

    public void unregister(AnimatedBackground background) {
        backgrounds.remove(background);
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Advances all registered backgrounds when enough time has elapsed.
     * Requests a GUI refresh if any background advanced a frame.
     *
     * @param nowNanos monotonically increasing time in nanoseconds
     * @throws IOException if refreshing the GUI fails
     */
    public void tick(long nowNanos) throws IOException {
        if (!running || backgrounds.isEmpty()) return;

        if (lastTickNanos < 0) {
            lastTickNanos = nowNanos;
            stepAll(nowNanos);
            gui.requestRefresh();
            return;
        }

        long delta = nowNanos - lastTickNanos;
        if (delta < 0) delta = 0;
        lastTickNanos = nowNanos;
        accumulatedNs += delta;

        long frameNs = NS_PER_MS * 1000L / targetFps;
        boolean updated = false;
        while (accumulatedNs >= frameNs) {
            stepAll(nowNanos);
            accumulatedNs -= frameNs;
            updated = true;
        }

        if (updated) {
            gui.requestRefresh();
        }
    }

    private void stepAll(long nowNanos) {
        for (var bg : backgrounds) {
            if (bg instanceof StarfieldBackground sfb) {
                sfb.tick(nowNanos);
            }
        }
    }
}
