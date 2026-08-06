package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowImpl;
import io.jterm.window.WindowHint;

import io.jterm.widget.animation.WarpStarfield;

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
 * target FPS and requests a screen refresh only when a frame actually advances.</p>
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

    /**
     * Constructs a new AnimationManager instance.
     * @param gui the gui
     */
    public AnimationManager(DefaultTextGUI gui) {
        this(gui, 60);
    }

    /**
     * Constructs a new AnimationManager instance.
     * @param gui the gui
     * @param targetFps the target fps
     */
    public AnimationManager(DefaultTextGUI gui, int targetFps) {
        this.gui = gui;
        this.targetFps = Math.max(MIN_FPS, Math.min(MAX_FPS, targetFps));
    }

    /**
     * Returns the target fps.
     * @return the result
     */
    public int getTargetFps() {
        return targetFps;
    }

    /**
     * Sets the target fps.
     * @param fps the fps
     */
    public void setTargetFps(int fps) {
        this.targetFps = Math.max(MIN_FPS, Math.min(MAX_FPS, fps));
    }

    /**
     * Register.
     * @param background the background
     */
    public void register(AnimatedBackground background) {
        backgrounds.addIfAbsent(background);
    }

    /**
     * Unregister.
     * @param background the background
     */
    public void unregister(AnimatedBackground background) {
        backgrounds.remove(background);
    }

    /**
     * Stops the animation.
     */
    public void stop() {
        running = false;
    }

    /**
     * Returns whether the running flag is set.
     * @return the result
     */
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
            } else if (bg instanceof TypewriterEffect tw) {
                tw.tick(nowNanos);
            } else if (bg instanceof MatrixRain mr) {
                mr.tick(nowNanos);
            } else if (bg instanceof PlasmaWash pw) {
                pw.tick(nowNanos);
            } else if (bg instanceof WarpStarfield wf) {
                wf.tick(nowNanos);
            } else if (bg instanceof CircuitBoard cb) {
                cb.tick(nowNanos);
            } else if (bg instanceof OceanWaves ow) {
                ow.tick(nowNanos);
            } else if (bg instanceof LavaLamp ll) {
                ll.tick(nowNanos);
            } else if (bg instanceof TerrainFlyover tf) {
                tf.tick(nowNanos);
            } else if (bg instanceof Aurora aurora) {
                aurora.tick(nowNanos);
            } else if (bg instanceof RainStorm rs) {
                rs.tick(nowNanos);
            } else if (bg instanceof Fireworks fw) {
                fw.tick(nowNanos);
            } else if (bg instanceof DNAHelix dh) {
                dh.tick(nowNanos);
            } else if (bg instanceof VoronoiCells vc) {
                vc.tick(nowNanos);
            }
        }
    }
}
