package io.jterm.widget.model;

/**
 * Functional interface for listeners that want to be notified of changes
 * to a {@link GridModel}.
 */
@FunctionalInterface
public interface GridListener {

    /**
     * Called when the grid model has changed (rows added, removed, replaced, or cleared).
     */
    void gridChanged();
}