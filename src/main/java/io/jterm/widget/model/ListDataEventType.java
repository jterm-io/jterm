package io.jterm.widget.model;

/**
 * Types of changes that can occur in a {@link ListModel}.
 */
public enum ListDataEventType {
    /** List contents changed in a way not covered by the interval types. */
    CONTENTS_CHANGED,

    /** Items were inserted into the list. */
    INTERVAL_ADDED,

    /** Items were removed from the list. */
    INTERVAL_REMOVED
}