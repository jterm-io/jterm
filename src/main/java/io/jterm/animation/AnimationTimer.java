package io.jterm.animation;

/**
 * Simple fixed-rate timer for animation loops. Runs the supplied callback on
 * a virtual thread and sleeps to honour the requested frame rate.
 */
public class AnimationTimer {
    private final int targetFps;
    private final Runnable frameCallback;
    private volatile boolean running;
    private Thread animationThread;

    public AnimationTimer(int targetFps, Runnable frameCallback) {
        if (targetFps <= 0) {
            throw new IllegalArgumentException("targetFps must be positive");
        }
        if (frameCallback == null) {
            throw new IllegalArgumentException("frameCallback must not be null");
        }
        this.targetFps = targetFps;
        this.frameCallback = frameCallback;
    }

    public void start() {
        if (running) return;
        running = true;
        animationThread = Thread.ofVirtual().name("animation").start(() -> {
            long frameNanos = 1_000_000_000L / targetFps;
            while (running) {
                long start = System.nanoTime();
                frameCallback.run();
                long elapsed = System.nanoTime() - start;
                long sleep = frameNanos - elapsed;
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep / 1_000_000, (int)(sleep % 1_000_000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
    }

    public void stop() {
        running = false;
        if (animationThread != null) {
            animationThread.interrupt();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getTargetFps() {
        return targetFps;
    }
}
