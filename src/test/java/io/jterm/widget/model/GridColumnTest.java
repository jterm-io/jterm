package io.jterm.widget.model;

import io.jterm.style.CellStyle;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class GridColumnTest {

    // Simple row type for testing
    record TestRow(String name, int age, double score) {}

    @Test
    void textFactoryProducesLeftAlignmentAndNullFormat() {
        GridColumn<TestRow> col = GridColumn.text("Name", TestRow::name);
        assertEquals("Name", col.header());
        assertEquals(GridColumn.Alignment.LEFT, col.alignment());
        assertNull(col.format());
        assertEquals(0, col.minWidth());
        assertEquals(0, col.maxWidth());
        assertNull(col.styler());
    }

    @Test
    void textFactoryAccessorReturnsStringValue() {
        GridColumn<TestRow> col = GridColumn.text("Name", TestRow::name);
        TestRow row = new TestRow("Alice", 30, 95.5);
        assertEquals("Alice", col.accessor().apply(row));
    }

    @Test
    void intColFactoryProducesRightAlignmentAndPercentDFormat() {
        GridColumn<TestRow> col = GridColumn.intCol("Age", TestRow::age);
        assertEquals("Age", col.header());
        assertEquals(GridColumn.Alignment.RIGHT, col.alignment());
        assertEquals("%d", col.format());
    }

    @Test
    void intColFactoryAccessorReturnsIntegerValue() {
        GridColumn<TestRow> col = GridColumn.intCol("Age", TestRow::age);
        TestRow row = new TestRow("Alice", 30, 95.5);
        assertEquals(30, col.accessor().apply(row));
    }

    @Test
    void doubleColFactoryProducesRightAlignmentAndGivenFormat() {
        GridColumn<TestRow> col = GridColumn.doubleCol("Score", "%.2f", TestRow::score);
        assertEquals("Score", col.header());
        assertEquals(GridColumn.Alignment.RIGHT, col.alignment());
        assertEquals("%.2f", col.format());
    }

    @Test
    void doubleColFactoryAccessorReturnsDoubleValue() {
        GridColumn<TestRow> col = GridColumn.doubleCol("Score", "%.2f", TestRow::score);
        TestRow row = new TestRow("Alice", 30, 95.5);
        assertEquals(95.5, col.accessor().apply(row));
    }

    @Test
    void columnFactoryProducesLeftAlignmentAndNullFormat() {
        GridColumn<TestRow> col = GridColumn.column("Data", r -> r.name() + ":" + r.age());
        assertEquals("Data", col.header());
        assertEquals(GridColumn.Alignment.LEFT, col.alignment());
        assertNull(col.format());
    }

    @Test
    void columnFactoryAccessorReturnsObjectValue() {
        GridColumn<TestRow> col = GridColumn.column("Data", r -> r.name() + ":" + r.age());
        TestRow row = new TestRow("Alice", 30, 95.5);
        assertEquals("Alice:30", col.accessor().apply(row));
    }

    @Test
    void withAlignmentReturnsNewInstanceWithModifiedAlignment() {
        GridColumn<TestRow> col = GridColumn.text("Name", TestRow::name);
        GridColumn<TestRow> modified = col.withAlignment(GridColumn.Alignment.CENTER);
        assertNotSame(col, modified);
        assertEquals(GridColumn.Alignment.LEFT, col.alignment());
        assertEquals(GridColumn.Alignment.CENTER, modified.alignment());
        // Other fields unchanged
        assertEquals(col.header(), modified.header());
        assertEquals(col.format(), modified.format());
    }

    @Test
    void withMinWidthReturnsNewInstanceWithModifiedMinWidth() {
        GridColumn<TestRow> col = GridColumn.text("Name", TestRow::name);
        GridColumn<TestRow> modified = col.withMinWidth(10);
        assertNotSame(col, modified);
        assertEquals(0, col.minWidth());
        assertEquals(10, modified.minWidth());
    }

    @Test
    void withMaxWidthReturnsNewInstanceWithModifiedMaxWidth() {
        GridColumn<TestRow> col = GridColumn.text("Name", TestRow::name);
        GridColumn<TestRow> modified = col.withMaxWidth(50);
        assertNotSame(col, modified);
        assertEquals(0, col.maxWidth());
        assertEquals(50, modified.maxWidth());
    }

    @Test
    void withStylerSetsTheStylerFunction() {
        GridColumn<TestRow> col = GridColumn.doubleCol("Score", "%.2f", TestRow::score);
        assertNull(col.styler());
        Function<Object, CellStyle> styler = v -> ((Double) v) > 90 ? CellStyle.GREEN : CellStyle.DEFAULT;
        GridColumn<TestRow> modified = col.withStyler(styler);
        assertNotSame(col, modified);
        assertSame(styler, modified.styler());
        // Original unchanged
        assertNull(col.styler());
    }

    @Test
    void withStylerStylerIsInvokedCorrectly() {
        GridColumn<TestRow> col = GridColumn.doubleCol("Score", "%.2f", TestRow::score)
            .withStyler(v -> ((Double) v) > 90 ? CellStyle.GREEN : CellStyle.RED);
        assertSame(CellStyle.GREEN, col.styler().apply(95.0));
        assertSame(CellStyle.RED, col.styler().apply(50.0));
    }

    @Test
    void copyMethodsDoNotMutateOriginal() {
        GridColumn<TestRow> col = GridColumn.text("Name", TestRow::name);
        GridColumn<TestRow> withAlign = col.withAlignment(GridColumn.Alignment.RIGHT);
        GridColumn<TestRow> withMin = col.withMinWidth(5);
        GridColumn<TestRow> withMax = col.withMaxWidth(20);
        GridColumn<TestRow> withStyle = col.withStyler(v -> CellStyle.BOLD);

        // Original is untouched
        assertEquals(GridColumn.Alignment.LEFT, col.alignment());
        assertEquals(0, col.minWidth());
        assertEquals(0, col.maxWidth());
        assertNull(col.styler());
    }

    @Test
    void resolveAlignmentReturnsLeftForAutoAsFallback() {
        GridColumn<TestRow> col = GridColumn.column("Data", r -> r.name());
        // column() factory sets LEFT, not AUTO — but if someone constructs with AUTO directly,
        // resolveAlignment() should return LEFT as a fallback
        GridColumn<TestRow> autoCol = new GridColumn<>(
            "X", r -> r.name(), GridColumn.Alignment.AUTO, null, 0, 0, null);
        assertEquals(GridColumn.Alignment.LEFT, autoCol.resolveAlignment());
    }

    @Test
    void resolveAlignmentReturnsExplicitAlignment() {
        GridColumn<TestRow> col = GridColumn.text("Name", TestRow::name)
            .withAlignment(GridColumn.Alignment.CENTER);
        assertEquals(GridColumn.Alignment.CENTER, col.resolveAlignment());

        GridColumn<TestRow> rightCol = GridColumn.intCol("Age", TestRow::age);
        assertEquals(GridColumn.Alignment.RIGHT, rightCol.resolveAlignment());

        GridColumn<TestRow> leftCol = GridColumn.text("Name", TestRow::name);
        assertEquals(GridColumn.Alignment.LEFT, leftCol.resolveAlignment());
    }

    @Test
    void alignmentEnumHasAllFourValues() {
        assertEquals(4, GridColumn.Alignment.values().length);
        assertNotNull(GridColumn.Alignment.valueOf("LEFT"));
        assertNotNull(GridColumn.Alignment.valueOf("CENTER"));
        assertNotNull(GridColumn.Alignment.valueOf("RIGHT"));
        assertNotNull(GridColumn.Alignment.valueOf("AUTO"));
    }
}