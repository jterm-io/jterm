package io.jterm.layout;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.widget.Component;

import java.util.List;

/** Five-region layout: NORTH, SOUTH, EAST, WEST, CENTER. */
public class BorderLayout implements LayoutManager {
    /**
     * The five border regions a child can be placed in.
     */
    public enum Region {
        /** Top strip: full width, docked to the top edge. */
        NORTH,
        /** Bottom strip: full width, docked to the bottom edge. */
        SOUTH,
        /** Right column between the corners, docked to the right edge. */
        EAST,
        /** Left column between the corners, docked to the left edge. */
        WEST,
        /** Middle area remaining after the outer regions are measured. */
        CENTER
    }

    /** Per-child layout data assigning a component to a border region. */
    public static class BorderLayoutData implements LayoutData {
        /** The border region the component is placed in. */
        public final Region region;
        /**
         * Associate a child component with a border-layout region.
         *
         * @param region the region to place the component in
         */
        public BorderLayoutData(Region region) { this.region = region; }
    }

    /**
     * Creates a border layout with default (zero) outer-region sizes.
     */
    public BorderLayout() {
    }

    /**
     * Compute the preferred size for the given children.
     *
     * @param children the child components
     *
     * @return the computed preferred size
     */
    @Override
    public TerminalSize getPreferredSize(List<Component> children) {
        int width = 0, height = 0;
        int northHeight = 0, southHeight = 0, eastWidth = 0, westWidth = 0;
        int centerWidth = 0, centerHeight = 0;
        for (var c : children) {
            var ps = c.getPreferredSize();
            var data = (BorderLayoutData) c.getLayoutData();
            if (data == null) continue;
            switch (data.region) {
                case NORTH, SOUTH -> {
                    width = Math.max(width, ps.columns());
                    northHeight = Math.max(northHeight, ps.rows());
                    southHeight = Math.max(southHeight, ps.rows());
                }
                case EAST, WEST -> {
                    height = Math.max(height, ps.rows());
                    eastWidth = Math.max(eastWidth, ps.columns());
                    westWidth = Math.max(westWidth, ps.columns());
                }
                case CENTER -> {
                    centerWidth = ps.columns();
                    centerHeight = ps.rows();
                }
            }
        }
        width = Math.max(width, eastWidth + centerWidth + westWidth);
        height = Math.max(height, northHeight + centerHeight + southHeight);
        return new TerminalSize(Math.max(width, 1), Math.max(height, 1));
    }

    /**
     * Lay out children within the given area.
     *
     * @param area the available area
     * @param children the child components
     */
    @Override
    public void doLayout(TerminalSize area, List<Component> children) {
        int northHeight = 0, southHeight = 0, eastWidth = 0, westWidth = 0;
        Component center = null;
        for (var c : children) {
            var data = (BorderLayoutData) c.getLayoutData();
            if (data == null) continue;
            switch (data.region) {
                case NORTH -> northHeight = Math.max(northHeight, c.getPreferredSize().rows());
                case SOUTH -> southHeight = Math.max(southHeight, c.getPreferredSize().rows());
                case EAST -> eastWidth = Math.max(eastWidth, c.getPreferredSize().columns());
                case WEST -> westWidth = Math.max(westWidth, c.getPreferredSize().columns());
                case CENTER -> center = c;
            }
        }
        int centerHeight = Math.max(0, area.rows() - northHeight - southHeight);
        int centerWidth = Math.max(0, area.columns() - eastWidth - westWidth);
        for (var c : children) {
            var data = (BorderLayoutData) c.getLayoutData();
            if (data == null) continue;
            switch (data.region) {
                case NORTH -> c.setBounds(new TerminalPosition(0, 0), new TerminalSize(area.columns(), northHeight));
                case SOUTH -> c.setBounds(new TerminalPosition(0, area.rows() - southHeight), new TerminalSize(area.columns(), southHeight));
                case EAST -> c.setBounds(new TerminalPosition(area.columns() - eastWidth, northHeight), new TerminalSize(eastWidth, centerHeight));
                case WEST -> c.setBounds(new TerminalPosition(0, northHeight), new TerminalSize(westWidth, centerHeight));
                case CENTER -> c.setBounds(new TerminalPosition(westWidth, northHeight), new TerminalSize(centerWidth, centerHeight));
            }
        }
    }
}
