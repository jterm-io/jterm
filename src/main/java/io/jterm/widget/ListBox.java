package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.util.TerminalTextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Scrollable list of items. */
public class ListBox<T> extends AbstractComponent {
    private final List<T> items = new ArrayList<>();
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private Function<T, String> renderer = Object::toString;
    private final List<Runnable> selectionListeners = new ArrayList<>();

    public ListBox() {}

    public void addItem(T item) {
        items.add(item);
        invalidate();
    }

    public void setRenderer(Function<T, String> renderer) {
        this.renderer = renderer;
        invalidate();
    }

    public void addSelectionListener(Runnable listener) {
        selectionListeners.add(listener);
    }

    public T getSelectedItem() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) return null;
        return items.get(selectedIndex);
    }

    public int getSelectedIndex() { return selectedIndex; }

    public void setSelectedIndex(int index) {
        if (index < 0) index = 0;
        else if (items.isEmpty()) index = 0;
        else if (index >= items.size()) index = items.size() - 1;
        if (index == this.selectedIndex) return;
        this.selectedIndex = index;
        ensureVisible();
        for (var l : selectionListeners) l.run();
        invalidate();
    }

    public List<T> getItems() { return new ArrayList<>(items); }

    private void ensureVisible() {
        var rows = getSize().rows();
        if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
        if (selectedIndex >= scrollOffset + rows) scrollOffset = selectedIndex - rows + 1;
        if (scrollOffset < 0) scrollOffset = 0;
    }

    @Override
    protected TerminalSize calculatePreferredSize() {
        int maxLen = 4;
        for (var item : items) {
            maxLen = Math.max(maxLen, TerminalTextUtils.getTrueWidth(renderer.apply(item)));
        }
        return new TerminalSize(maxLen, Math.max(2, Math.min(items.size(), 10)));
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        for (int r = 0; r < size.rows(); r++) {
            int idx = scrollOffset + r;
            if (idx >= items.size()) {
                graphics.fillRectangle(0, r, size.columns(), 1, new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT));
                continue;
            }
            String text = renderer.apply(items.get(idx));
            boolean selected = idx == selectedIndex;
            TextCell style = selected
                ? new TextCell(' ', AnsiColor.BLACK, AnsiColor.WHITE)
                : new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT);
            graphics.fillRectangle(0, r, size.columns(), 1, style);
            graphics.drawString(0, r, TerminalTextUtils.truncate(text, size.columns()), style);
        }
    }

    @Override
    public void handleKeyStroke(KeyStroke keyStroke) {
        switch (keyStroke.type()) {
            case ARROW_UP -> setSelectedIndex(selectedIndex - 1);
            case ARROW_DOWN -> setSelectedIndex(selectedIndex + 1);
            case ENTER -> {
                for (var l : selectionListeners) l.run();
            }
            default -> {}
        }
    }

    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        ensureVisible();
    }
}
