package io.jterm.widget;

import io.jterm.widget.model.DefaultTableModel;
import io.jterm.widget.model.TableModelEvent;
import io.jterm.widget.model.TableModelEventType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableExtraTest {
    @Test
    void setModelSwitchesBackingModel() {
        var t = new Table("A", "B");
        var newModel = new DefaultTableModel("X", "Y", "Z");
        t.setModel(newModel);
        assertSame(newModel, t.getModel());
    }

    @Test
    void tableChangedClampsSelection() {
        var model = new DefaultTableModel("C1");
        model.addRow("r1");
        var t = new Table(model);
        t.setBounds(new io.jterm.core.TerminalPosition(0, 0), new io.jterm.core.TerminalSize(10, 5));
        t.setSelectedRow(0);
        model.clear();
        t.tableChanged(new TableModelEvent(TableModelEventType.STRUCTURE_CHANGED, 0, 0, -1));
        assertEquals(0, t.getSelectedRow()); // clamps to max(0, rowCount-1) when empty
    }

    @Test
    void addRowWorksWithDefaultModel() {
        var t = new Table("A");
        t.addRow("x");
        assertEquals(1, t.getModel().getRowCount());
    }

    @Test
    void addRowThrowsWhenCustomModel() {
        class CustomModel implements io.jterm.widget.model.TableModel {
            private final List<String> data = new ArrayList<>();
            public int getRowCount() { return data.size(); }
            public int getColumnCount() { return 1; }
            public String getColumnName(int column) { return "A"; }
            public String getValueAt(int row, int column) { return data.get(row); }
            public void addTableModelListener(io.jterm.widget.model.TableModelListener l) {}
            public void removeTableModelListener(io.jterm.widget.model.TableModelListener l) {}
        }
        var t = new Table(new CustomModel());
        assertThrows(IllegalStateException.class, () -> t.addRow("x"));
    }

    @Test
    void preferredSizeWithEmptyModelIsOneColumnWide() {
        var t = new Table("A");
        var ps = t.getPreferredSize();
        assertTrue(ps.columns() >= 1);
        assertTrue(ps.rows() >= 3);
    }
}
