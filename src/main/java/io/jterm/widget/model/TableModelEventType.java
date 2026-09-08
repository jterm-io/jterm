package io.jterm.widget.model;

/**
 * Types of changes that can occur in a {@link TableModel}.
 */
public enum TableModelEventType {
    /** One or more rows were inserted into the model. */
    ROWS_ADDED,

    /** One or more rows were removed from the model. */
    ROWS_REMOVED,

    /** Row contents changed (e.g. a row was replaced or reordered). */
    ROWS_CHANGED,

    /** Cell values changed without altering the row structure. */
    CELLS_CHANGED,

    /** The model changed in a way not covered by the other types; listeners must reload all data. */
    STRUCTURE_CHANGED
}