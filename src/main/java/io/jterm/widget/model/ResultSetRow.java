package io.jterm.widget.model;

import java.util.List;

/**
 * Row data extracted from a JDBC ResultSet.
 *
 * @param values column values indexed from 0
 */
public record ResultSetRow(List<String> values) {

    /**
     * Returns the value at the given column index.
     *
     * @param col zero-based column index
     * @return the value, or an empty string if the index is out of range
     */
    public String value(int col) {
        if (col < 0 || col >= values.size()) return "";
        return values.get(col);
    }
}