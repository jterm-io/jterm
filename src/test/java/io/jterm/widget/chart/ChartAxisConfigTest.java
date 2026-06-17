package io.jterm.widget.chart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChartAxisConfigTest {

    @Test
    void autoConfig() {
        var cfg = ChartAxisConfig.auto();
        assertTrue(cfg.autoScale());
        assertEquals("%.0f", cfg.labelFormat());
    }

    @Test
    void fixedConfig() {
        var cfg = ChartAxisConfig.fixed(0, 200, "$%.0f");
        assertFalse(cfg.autoScale());
        assertEquals(0, cfg.min());
        assertEquals(200, cfg.max());
        assertEquals("$%.0f", cfg.labelFormat());
    }

    @Test
    void fixedConfigDefaultFormat() {
        var cfg = ChartAxisConfig.fixed(10, 20);
        assertFalse(cfg.autoScale());
        assertEquals("%.0f", cfg.labelFormat());
    }

    @Test
    void formatValue() {
        var cfg = ChartAxisConfig.fixed(0, 100, "%.1f");
        assertEquals("50.0", cfg.format(50));
    }

    @Test
    void formatCurrency() {
        var cfg = ChartAxisConfig.fixed(0, 200, "$%.0f");
        assertEquals("$150", cfg.format(150));
    }
}