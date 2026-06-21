package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.widget.model.AbstractListModel;
import io.jterm.widget.model.DefaultListModel;
import io.jterm.widget.model.ListDataEvent;
import io.jterm.widget.model.ListDataListener;
import io.jterm.widget.model.ListModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ListBoxModelIntegrationTest {

    private static final class CustomListModel<T> extends AbstractListModel<T> {
        private final List<T> items = new CopyOnWriteArrayList<>();

        void add(T item) {
            items.add(item);
            fireIntervalAdded(items.size() - 1, items.size() - 1);
        }

        void remove(int index) {
            items.remove(index);
            fireIntervalRemoved(index, index);
        }

        void set(int index, T value) {
            items.set(index, value);
            fireContentsChanged(index, index);
        }

        @Override
        public int getSize() {
            return items.size();
        }

        @Override
        public T getElementAt(int index) {
            return items.get(index);
        }
    }

    @Test
    void listBoxUsesCustomListModel() {
        CustomListModel<String> model = new CustomListModel<>();
        model.add("a");
        model.add("b");
        ListBox<String> list = new ListBox<>(model);
        assertEquals(model, list.getModel());
        assertEquals(2, list.getItems().size());
        assertEquals("a", list.getItems().get(0));
    }

    @Test
    void intervalAddedInvalidatesAndRendersNewItem() {
        CustomListModel<String> model = new CustomListModel<>();
        ListBox<String> list = new ListBox<>(model);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        list.draw(new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5))));
        model.add("first");
        var buffer = new ScreenBuffer(new TerminalSize(10, 5));
        list.draw(new TextGraphics(buffer));
        assertEquals('f', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    void intervalRemovedClampsSelection() {
        CustomListModel<String> model = new CustomListModel<>();
        model.add("a");
        model.add("b");
        model.add("c");
        ListBox<String> list = new ListBox<>(model);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        list.setSelectedIndex(2);
        model.remove(2);
        assertEquals(1, list.getSelectedIndex());
    }

    @Test
    void contentsChangedTriggersReRender() {
        CustomListModel<String> model = new CustomListModel<>();
        model.add("old");
        ListBox<String> list = new ListBox<>(model);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        var buffer = new ScreenBuffer(new TerminalSize(10, 5));
        list.draw(new TextGraphics(buffer));
        assertEquals('o', buffer.getCell(0, 0).character().charAt(0));
        model.set(0, "new");
        buffer = new ScreenBuffer(new TerminalSize(10, 5));
        list.draw(new TextGraphics(buffer));
        assertEquals('n', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    void setModelSwitchesListenerSubscription() {
        CustomListModel<String> first = new CustomListModel<>();
        first.add("a");
        ListBox<String> list = new ListBox<>(first);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        CustomListModel<String> second = new CustomListModel<>();
        second.add("b");
        list.setModel(second);
        assertEquals(second, list.getModel());
        assertEquals("b", list.getSelectedItem());
        var buffer = new ScreenBuffer(new TerminalSize(10, 5));
        list.draw(new TextGraphics(buffer));
        assertEquals('b', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    void oldModelNoLongerAffectsListBoxAfterSetModel() {
        CustomListModel<String> first = new CustomListModel<>();
        first.add("a");
        CustomListModel<String> second = new CustomListModel<>();
        second.add("b");
        ListBox<String> list = new ListBox<>(first);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        list.setModel(second);
        first.add("x");
        assertEquals(1, list.getModel().getSize());
    }

    @Test
    void autoScrollOnModelAddKeepsLastItemVisible() {
        CustomListModel<String> model = new CustomListModel<>();
        ListBox<String> list = new ListBox<>(model);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setAutoScroll(true);
        for (int i = 0; i < 8; i++) {
            model.add("item-" + i);
        }
        assertTrue(list.isLastItemVisible());
    }

    @Test
    void selectionListenerFiresAfterModelDrivenAdjustment() {
        CustomListModel<String> model = new CustomListModel<>();
        model.add("a");
        model.add("b");
        model.add("c");
        ListBox<String> list = new ListBox<>(model);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        list.setSelectedIndex(2);
        AtomicInteger counter = new AtomicInteger();
        list.addSelectionListener(counter::incrementAndGet);
        model.remove(1); // removes 'b'; selected 2 becomes selected 2 (was 'c')
        assertEquals(1, list.getSelectedIndex());
        assertEquals(1, counter.get(), "selection listener should fire when model removal changes the selected element");
    }

    @Test
    void rendererAppliedToModelElements() {
        CustomListModel<Integer> model = new CustomListModel<>();
        model.add(1);
        model.add(2);
        ListBox<Integer> list = new ListBox<>(model);
        list.setRenderer(i -> "Value: " + i);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        var buffer = new ScreenBuffer(new TerminalSize(10, 5));
        list.draw(new TextGraphics(buffer));
        assertEquals('V', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    void emptyModelRendersWithoutCrash() {
        CustomListModel<String> model = new CustomListModel<>();
        ListBox<String> list = new ListBox<>(model);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        assertDoesNotThrow(() -> list.draw(new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)))));
        assertNull(list.getSelectedItem());
        assertEquals(0, list.getSelectedIndex());
    }

    @Test
    void removeLastItemResetsSelectionGracefully() {
        CustomListModel<String> model = new CustomListModel<>();
        model.add("only");
        ListBox<String> list = new ListBox<>(model);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        list.setSelectedIndex(0);
        AtomicInteger counter = new AtomicInteger();
        list.addSelectionListener(counter::incrementAndGet);
        model.remove(0);
        assertEquals(0, list.getSelectedIndex());
        assertNull(list.getSelectedItem());
        assertEquals(0, counter.get(), "no selection change event when last item removed and selection clamps to 0");
    }

    @Test
    void backwardCompatWithDefaultListModelAddAndClear() {
        ListBox<String> list = new ListBox<>();
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        list.addItem("a");
        list.addItem("b");
        assertEquals(2, list.getItems().size());
        list.clearItems();
        assertEquals(0, list.getItems().size());
    }

    @Test
    void concurrentModelUpdatesKeepListBoxConsistent() throws InterruptedException {
        CustomListModel<Integer> model = new CustomListModel<>();
        ListBox<Integer> list = new ListBox<>(model);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        int threads = 4;
        int iterations = 50;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int t = 0; t < threads; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < iterations; i++) {
                        model.add(threadId * iterations + i);
                    }
                    done.countDown();
                });
            }
            ready.await();
            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS));
        }
        assertEquals(threads * iterations, list.getModel().getSize());
        assertDoesNotThrow(() -> list.draw(new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)))));
    }
}
