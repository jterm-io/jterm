package io.jterm.widget.model;

import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResultSetGridModelTest {

    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        conn = DriverManager.getConnection("jdbc:h2:mem:testgrid;DB_CLOSE_DELAY=-1");
        try (Statement s = conn.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS test (id INT, name VARCHAR(100), description VARCHAR(500))");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute("DROP TABLE IF EXISTS test");
        }
        conn.close();
    }

    @Test
    void testGetColumnNames() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO test VALUES (1, 'Alice', 'desc')");
            ResultSet rs = stmt.executeQuery("SELECT id, name, description FROM test");
            ResultSetGridModel model = new ResultSetGridModel(rs, stmt);
            assertEquals(List.of("ID", "NAME", "DESCRIPTION"), model.getColumnNames());
            model.close();
        }
    }

    @Test
    void testGetRowCount_afterFetch() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            for (int i = 0; i < 5; i++) {
                stmt.execute("INSERT INTO test VALUES (" + i + ", 'name" + i + "', 'desc" + i + "')");
            }
            ResultSet rs = stmt.executeQuery("SELECT * FROM test");
            ResultSetGridModel model = new ResultSetGridModel(rs, stmt);
            model.getRow(0); // trigger fetch
            assertTrue(model.getRowCount() >= 1);
            model.close();
        }
    }

    @Test
    void testGetRow_returnsCorrectValues() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO test VALUES (42, 'Bob', 'builder')");
            ResultSet rs = stmt.executeQuery("SELECT * FROM test");
            ResultSetGridModel model = new ResultSetGridModel(rs, stmt);
            ResultSetRow row = model.getRow(0);
            assertEquals("42", row.value(0));
            assertEquals("Bob", row.value(1));
            assertEquals("builder", row.value(2));
            model.close();
        }
    }

    @Test
    void testLazyFetching() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            for (int i = 0; i < 100; i++) {
                stmt.execute("INSERT INTO test VALUES (" + i + ", 'name" + i + "', 'desc" + i + "')");
            }
            ResultSet rs = stmt.executeQuery("SELECT * FROM test");
            ResultSetGridModel model = new ResultSetGridModel(rs, stmt);
            assertEquals(0, model.getRowCount()); // no rows fetched yet
            model.getRow(99); // fetch all
            assertEquals(100, model.getRowCount());
            model.close();
        }
    }

    @Test
    void testNullValuesBecomeEmptyString() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO test VALUES (1, NULL, 'ok')");
            ResultSet rs = stmt.executeQuery("SELECT * FROM test");
            ResultSetGridModel model = new ResultSetGridModel(rs, stmt);
            ResultSetRow row = model.getRow(0);
            assertEquals("", row.value(1));
            model.close();
        }
    }

    @Test
    void testLongValuesAreTruncated() throws SQLException {
        String longVal = "x".repeat(100);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO test VALUES (1, 'name', '" + longVal + "')");
            ResultSet rs = stmt.executeQuery("SELECT * FROM test");
            ResultSetGridModel model = new ResultSetGridModel(rs, stmt);
            ResultSetRow row = model.getRow(0);
            String desc = row.value(2);
            assertTrue(desc.length() <= 50);
            assertTrue(desc.endsWith("..."));
            model.close();
        }
    }

    @Test
    void testCloseIsIdempotent() throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("INSERT INTO test VALUES (1, 'test', 'test')");
        ResultSet rs = stmt.executeQuery("SELECT * FROM test");
        ResultSetGridModel model = new ResultSetGridModel(rs, stmt);
        model.close();
        assertDoesNotThrow(() -> model.close()); // second close is a no-op
    }
}