package io.jterm.widget.model;

/**
 * Event describing a change to a {@link ListModel}.
 *
 * @param type the kind of change
 * @param index0 first affected index, inclusive
 * @param index1 last affected index, inclusive
 */
public record ListDataEvent(ListDataEventType type, int index0, int index1) {
}
