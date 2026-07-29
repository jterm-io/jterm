package io.jterm.widget.model;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GridModel backed by a JDBC ResultSet. Lazily fetches rows in batches
 * and caches them. Column names are extracted from ResultSetMetaData.
 *
 * <p>The caller must pass a ResultSet that is already positioned before
 * the first row. The model reads rows forward-only. Call {@link #close()}
 * when done to release the ResultSet and its Statement.
 */
public class ResultSetGridModel implements GridModel<ResultSetRow> {

    private static final int FETCH_BATCH = 50;
    private static final int MAX_CELL_WIDTH = 50;

    private final ResultSet resultSet;
    private final Statement statement;
    private final List<String> columnNames;
    private final int columnCount;
    private final List<ResultSetRow> rows = new CopyOnWriteArrayList<>();
    private final List<GridListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean exhausted = false;
    private volatile boolean closed = false;

    /**
     * Creates a model from an already-executed ResultSet.
     *
     * @param resultSet the ResultSet (positioned before first row)
     * @param statement the Statement that produced the ResultSet (for closing); may be null
     */
    public ResultSetGridModel(ResultSet resultSet, Statement statement) throws SQLException {
        this.resultSet = resultSet;
        this.statement = statement;
        ResultSetMetaData meta = resultSet.getMetaData();
        this.columnCount = meta.getColumnCount();
        this.columnNames = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(meta.getColumnLabel(i));
        }
    }

    /** Returns the column names from the ResultSet metadata. */
    public List<String> getColumnNames() {
        return Collections.unmodifiableList(columnNames);
    }

    /** Returns the number of columns. */
    public int getColumnCount() {
        return columnCount;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public ResultSetRow getRow(int index) {
        ensureFetched(index);
        return rows.get(index);
    }

    /** Ensures rows up to (and including) the given index have been fetched. */
    private void ensureFetched(int index) {
        while (!exhausted && rows.size() <= index) {
            fetchBatch();
        }
    }

    /** Fetches the next batch of rows from the ResultSet. */
    private void fetchBatch() {
        try {
            int fetched = 0;
            while (fetched < FETCH_BATCH && resultSet.next()) {
                List<String> values = new ArrayList<>(columnCount);
                for (int c = 1; c <= columnCount; c++) {
                    String val = resultSet.getString(c);
                    if (val == null) val = "";
                    if (val.length() > MAX_CELL_WIDTH) val = val.substring(0, MAX_CELL_WIDTH - 3) + "...";
                    values.add(val);
                }
                rows.add(new ResultSetRow(values));
                fetched++;
            }
            if (fetched < FETCH_BATCH) {
                exhausted = true;
                closeResources();
            }
        } catch (SQLException e) {
            exhausted = true;
            closeResources();
        }
    }

    /** Fetches all remaining rows. Use with caution on large result sets. */
    public void fetchAll() {
        while (!exhausted) {
            fetchBatch();
        }
    }

    /** Closes the ResultSet and Statement. Safe to call multiple times. */
    public void close() {
        closeResources();
    }

    private synchronized void closeResources() {
        if (closed) return;
        closed = true;
        try { resultSet.close(); } catch (SQLException ignored) {}
        if (statement != null) {
            try { statement.close(); } catch (SQLException ignored) {}
        }
    }

    @Override
    public void addGridListener(GridListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeGridListener(GridListener listener) {
        listeners.remove(listener);
    }
}