package io.jterm.style;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Holds the active {@link Theme} and notifies listeners when it changes.
 *
 * <p>Widgets query {@code ThemeManager.active()} to get semantic colors
 * instead of hardcoding {@link AnsiColor} values. This allows switching
 * the entire look-and-feel at runtime.
 *
 * <pre>{@code
 * // In a widget's drawComponent:
 * var theme = ThemeManager.active();
 * var style = new TextCell(' ', theme.foreground(), theme.background());
 * graphics.fillRectangle(0, 0, w, h, style);
 * }</pre>
 */
public final class ThemeManager {
    private static volatile Theme active = Theme.DARK;
    private static final CopyOnWriteArrayList<Consumer<Theme>> listeners = new CopyOnWriteArrayList<>();

    private ThemeManager() {} // utility class

    /** Returns the currently active theme. */
    public static Theme active() {
        return active;
    }

    /** Sets the active theme and notifies all listeners. */
    public static void setActive(Theme theme) {
        active = theme;
        for (var listener : listeners) {
            listener.accept(theme);
        }
    }

    /** Cycles to the next built-in theme. Returns the new active theme. */
    public static Theme cycle() {
        Theme[] built = Theme.BUILT_IN;
        for (int i = 0; i < built.length; i++) {
            if (active == built[i]) {
                Theme next = built[(i + 1) % built.length];
                setActive(next);
                return next;
            }
        }
        // If current is custom, go to first built-in
        setActive(built[0]);
        return built[0];
    }

    /** Register a callback to be notified when the theme changes. */
    public static void addListener(Consumer<Theme> listener) {
        listeners.add(listener);
    }

    /** Remove a previously registered listener. */
    public static void removeListener(Consumer<Theme> listener) {
        listeners.remove(listener);
    }
}