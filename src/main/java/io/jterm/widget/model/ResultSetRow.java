package io.jterm.widget.model;

import java.util.List;

/** Row data extracted from a JDBC ResultSet. */
public record ResultSetRow(List<String> values) {
    public String value(int col) {
        if (col < 0 || col >= values.size()) return "";
        return values.get(col);
    }
}