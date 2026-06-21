package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.util.TerminalTextUtils;
import io.jterm.widget.model.DefaultListModel;
import io.jterm.widget.model.ListDataEvent;
import io.jterm.widget.model.ListDataListener;
import io.jterm.widget.model.ListModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Scrollable list of items backed by a {@link ListModel}. */
public class ListBox<T> extends AbstractComponent implements ListDataListener {
    private ListModel<T> model;
    private volatile int selectedIndex = 0;
    private volatile int scrollOffset = 0;
    private volatile Function<T, String> renderer = Object::toString;
    private final List<Runnable> selectionListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile boolean autoScroll = false;
    private volatile boolean manualScroll = false;

    /** Creates a ListBox backed by an empty {@link DefaultListModel}. */
    public ListBox() {
        this(new DefaultListModel<>());
    }

    /** Creates a ListBox that displays the supplied model. */
    public ListBox(ListModel<T> model) {
        setModel(model);
    }

    /**
     * Replaces the model backing this list.
     *
     * <p>The list unregisters itself from the old model (if any), registers with
     * the new model, resets selection/scroll, and invalidates.
     */
    public void setModel(ListModel<T> model) {
        if (this.model != null) {
            this.model.removeListDataListener(this);
        }
        this.model = model;
        model.addListDataListener(this);
        selectedIndex = 0;
        scrollOffset = 0;
        manualScroll = false;
        invalidate();
    }

    /** Returns the model currently backing this list. */
    public ListModel<T> getModel() {
        return model;
    }

    /** Convenience: delegates to the model if it is a {@link DefaultListModel}. */
    public void addItem(T item) {
        if (model instanceof DefaultListModel<T> defaultModel) {
            defaultModel.addElement(item);
        } else {
            throw new UnsupportedOperationException(
                    "addItem requires a DefaultListModel; current model is " + model.getClass().getName());
        }
    }

    /** Convenience: removes all items if the model is a {@link DefaultListModel}. */
    public void clearItems() {
        if (model instanceof DefaultListModel<T> defaultModel) {
            defaultModel.clear();
        } else {
            throw new UnsupportedOperationException(
                    "clearItems requires a DefaultListModel; current model is " + model.getClass().getName());
        }
    }

    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
        if (autoScroll) {
            manualScroll = false;
            scrollToBottom();
        }
        invalidate();
    }

    public boolean isAutoScroll() {
        return autoScroll;
    }

    public void scrollToBottom() {
        int rows = getSize().rows();
        int maxOffset = Math.max(0, model.getSize() - rows);
        scrollOffset = maxOffset;
        manualScroll = false;
        invalidate();
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int offset) {
        int rows = getSize().rows();
        int maxOffset = Math.max(0, model.getSize() - rows);
        this.scrollOffset = Math.max(0, Math.min(offset, maxOffset));
        this.manualScroll = this.scrollOffset < maxOffset;
        invalidate();
    }

    public boolean isLastItemVisible() {
        int rows = getSize().rows();
        int size = model.getSize();
        if (size <= rows) return true;
        return scrollOffset + rows >= size;
    }

    public void setRenderer(Function<T, String> renderer) {
        this.renderer = renderer;
        invalidate();
    }

    public Function<T, String> getRenderer() {
        return renderer;
    }

    public void addSelectionListener(Runnable listener) {
        selectionListeners.add(listener);
    }

    public List<Runnable> getSelectionListeners() {
        return new ArrayList<>(selectionListeners);
    }

    public T getSelectedItem() {
        int size = model.getSize();
        if (size == 0 || selectedIndex < 0 || selectedIndex >= size) return null;
        return model.getElementAt(selectedIndex);
    }

    public int getSelectedIndex() { return selectedIndex; }

    public void setSelectedIndex(int index) {
        int size = model.getSize();
        if (index < 0) index = 0;
        else if (size == 0) index = 0;
        else if (index >= size) index = size - 1;
        if (index == this.selectedIndex) return;
        this.selectedIndex = index;
        if (!manualScroll) {
            ensureVisible();
        }
        fireSelectionChanged();
        invalidate();
    }

    public List<T> getItems() {
        List<T> result = new ArrayList<>(model.getSize());
        for (int i = 0; i < model.getSize(); i++) {
            result.add(model.getElementAt(i));
        }
        return result;
    }

    private void ensureVisible() {
        var rows = getSize().rows();
        if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
        if (selectedIndex >= scrollOffset + rows) scrollOffset = selectedIndex - rows + 1;
        if (scrollOffset < 0) scrollOffset = 0;
        int maxOffset = Math.max(0, model.getSize() - rows);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;
    }

    @Override
    protected TerminalSize calculatePreferredSize() {
        int maxLen = 4;
        for (int i = 0; i < model.getSize(); i++) {
            T item = model.getElementAt(i);
            maxLen = Math.max(maxLen, TerminalTextUtils.getTrueWidth(renderer.apply(item)));
        }
        return new TerminalSize(maxLen, Math.max(2, Math.min(model.getSize(), 10)));
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        var theme = ThemeManager.active();
        int modelSize = model.getSize();
        for (int r = 0; r < size.rows(); r++) {
            int idx = scrollOffset + r;
            if (idx >= modelSize) {
                graphics.fillRectangle(0, r, size.columns(), 1, new TextCell(' ', theme.foreground(), theme.background()));
                continue;
            }
            String text = renderer.apply(model.getElementAt(idx));
            boolean selected = idx == selectedIndex;
            TextCell style = selected
                ? new TextCell(' ', theme.selectionFg(), theme.selectionBg())
                : new TextCell(' ', theme.foreground(), theme.background());
            graphics.fillRectangle(0, r, size.columns(), 1, style);
            graphics.drawString(0, r, TerminalTextUtils.truncate(text, size.columns()), style);
        }
    }

    @Override
    public void handleKeyStroke(KeyStroke keyStroke) {
        switch (keyStroke.type()) {
            case ARROW_UP -> setSelectedIndex(selectedIndex - 1);
            case ARROW_DOWN -> setSelectedIndex(selectedIndex + 1);
            case ENTER -> fireSelectionChanged();
            default -> {}
        }
    }

    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        ensureVisible();
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        if (autoScroll) {
            scrollToBottom();
        }
        if (selectedIndex >= model.getSize()) {
            selectedIndex = Math.max(0, model.getSize() - 1);
        }
        invalidate();
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        int oldSelected = selectedIndex;
        if (selectedIndex >= model.getSize()) {
            selectedIndex = Math.max(0, model.getSize() - 1);
        }
        if (selectedIndex != oldSelected) {
            fireSelectionChanged();
        }
        ensureVisible();
        invalidate();
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        if (selectedIndex >= model.getSize()) {
            selectedIndex = Math.max(0, model.getSize() - 1);
        }
        ensureVisible();
        invalidate();
    }

    private void fireSelectionChanged() {
        for (var l : selectionListeners) l.run();
    }
}
