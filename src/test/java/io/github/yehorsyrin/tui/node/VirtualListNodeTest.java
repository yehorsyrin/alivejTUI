package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.backend.TerminalBackend;
import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.layout.LayoutEngine;
import io.github.yehorsyrin.tui.render.Renderer;
import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link VirtualListNode} and {@link VirtualList} factory.
 *
 * @author Jarvis (AI)
 */
class VirtualListNodeTest {

    private final LayoutEngine engine = new LayoutEngine();

    static class FakeBackend implements TerminalBackend {
        final java.util.List<String> puts = new java.util.ArrayList<>();
        @Override public void init() {}
        @Override public void shutdown() {}
        @Override public int getWidth()  { return 80; }
        @Override public int getHeight() { return 24; }
        @Override public void putChar(int col, int row, char c, Style s) {
            puts.add(col + "," + row + "=" + c);
        }
        @Override public void flush() {}
        @Override public void hideCursor() {}
        @Override public void showCursor() {}
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() { puts.clear(); }
        @Override public KeyEvent readKey() { return KeyEvent.of(KeyType.EOF); }
        @Override public void setResizeListener(Runnable r) {}
    }

    private static List<String> items(int count) {
        return IntStream.range(0, count).mapToObj(i -> "item" + i).collect(toList());
    }

    // --- Factory ---

    @Test
    void factory_of_createsNode() {
        VirtualListNode n = VirtualList.of(items(5), 3);
        assertEquals(5, n.itemCount());
        assertEquals(3, n.getMaxVisibleRows());
    }

    // --- Constructor ---

    @Test
    void constructor_invalidMaxVisibleRows_throws() {
        assertThrows(IllegalArgumentException.class, () -> new VirtualListNode(items(5), 0));
    }

    @Test
    void constructor_nullItems_treatedAsEmpty() {
        VirtualListNode n = new VirtualListNode(null, 5);
        assertEquals(0, n.itemCount());
        assertEquals(-1, n.getSelectedIndex());
    }

    @Test
    void constructor_emptyItems_selectedIsMinusOne() {
        VirtualListNode n = VirtualList.of(List.of(), 5);
        assertEquals(-1, n.getSelectedIndex());
    }

    @Test
    void constructor_withItems_selectedIsZero() {
        VirtualListNode n = VirtualList.of(items(3), 5);
        assertEquals(0, n.getSelectedIndex());
    }

    // --- Visible items ---

    @Test
    void getVisibleItems_allFit() {
        VirtualListNode n = VirtualList.of(items(3), 5);
        assertEquals(List.of("item0", "item1", "item2"), n.getVisibleItems());
    }

    @Test
    void getVisibleItems_capped() {
        VirtualListNode n = VirtualList.of(items(10), 3);
        assertEquals(List.of("item0", "item1", "item2"), n.getVisibleItems());
    }

    @Test
    void getVisibleItems_afterScroll() {
        VirtualListNode n = VirtualList.of(items(10), 3);
        n.selectDown();
        n.selectDown();
        n.selectDown(); // select=3, scroll offset should now be 1
        assertEquals("item1", n.getVisibleItems().get(0));
    }

    // --- scrollableRows / isScrollable ---

    @Test
    void scrollableRows_allFit_zero() {
        VirtualListNode n = VirtualList.of(items(3), 5);
        assertEquals(0, n.scrollableRows());
        assertFalse(n.isScrollable());
    }

    @Test
    void scrollableRows_whenExceeds() {
        VirtualListNode n = VirtualList.of(items(10), 3);
        assertEquals(7, n.scrollableRows());
        assertTrue(n.isScrollable());
    }

    // --- Navigation ---

    @Test
    void selectDown_advancesIndex() {
        VirtualListNode n = VirtualList.of(items(5), 3);
        n.selectDown();
        assertEquals(1, n.getSelectedIndex());
    }

    @Test
    void selectDown_clampAtEnd() {
        VirtualListNode n = VirtualList.of(items(3), 5);
        n.selectDown(); n.selectDown(); n.selectDown(); // past end
        assertEquals(2, n.getSelectedIndex());
    }

    @Test
    void selectUp_decrementsIndex() {
        VirtualListNode n = VirtualList.of(items(5), 3);
        n.selectDown();
        n.selectUp();
        assertEquals(0, n.getSelectedIndex());
    }

    @Test
    void selectUp_clampAtZero() {
        VirtualListNode n = VirtualList.of(items(3), 5);
        n.selectUp();
        assertEquals(0, n.getSelectedIndex());
    }

    @Test
    void selectDown_scrollsWhenNeeded() {
        VirtualListNode n = VirtualList.of(items(5), 2);
        n.selectDown(); // index=1, offset=0
        n.selectDown(); // index=2, offset=1
        assertEquals(2, n.getSelectedIndex());
        assertEquals(1, n.getScrollOffset());
    }

    @Test
    void selectFirst_jumpsToStart() {
        VirtualListNode n = VirtualList.of(items(10), 3);
        n.selectDown(); n.selectDown(); n.selectDown();
        n.selectFirst();
        assertEquals(0, n.getSelectedIndex());
        assertEquals(0, n.getScrollOffset());
    }

    @Test
    void selectLast_jumpsToEnd() {
        VirtualListNode n = VirtualList.of(items(10), 3);
        n.selectLast();
        assertEquals(9, n.getSelectedIndex());
        assertEquals(7, n.getScrollOffset());
    }

    @Test
    void getSelectedItem_returnsCorrect() {
        VirtualListNode n = VirtualList.of(items(5), 3);
        n.selectDown();
        assertEquals("item1", n.getSelectedItem());
    }

    @Test
    void getSelectedItem_empty_null() {
        VirtualListNode n = VirtualList.of(List.of(), 3);
        assertNull(n.getSelectedItem());
    }

    @Test
    void pageDown_advancesByVisibleRows() {
        VirtualListNode n = VirtualList.of(items(20), 5);
        n.pageDown();
        assertTrue(n.getSelectedIndex() >= 5);
    }

    @Test
    void pageUp_decreasesByVisibleRows() {
        VirtualListNode n = VirtualList.of(items(20), 5);
        n.selectLast();
        n.pageUp();
        assertTrue(n.getSelectedIndex() < 19);
    }

    // --- Layout ---

    @Test
    void layout_widthEqualsAvailable() {
        VirtualListNode n = VirtualList.of(items(5), 3);
        engine.layout(n, 0, 0, 40, 24);
        assertEquals(40, n.getWidth());
    }

    @Test
    void layout_heightEqualsVisibleRows() {
        VirtualListNode n = VirtualList.of(items(5), 3);
        engine.layout(n, 0, 0, 40, 24);
        assertEquals(3, n.getHeight());
    }

    @Test
    void layout_heightCappedAtActualItems() {
        VirtualListNode n = VirtualList.of(items(2), 5);
        engine.layout(n, 0, 0, 40, 24);
        assertEquals(2, n.getHeight()); // only 2 items visible
    }

    // --- Rendering ---

    @Test
    void render_visibleItemsPresent() {
        FakeBackend backend = new FakeBackend();
        VirtualListNode n = VirtualList.of(List.of("hello", "world"), 2);
        new Renderer(backend).render(n);
        assertTrue(backend.puts.stream().anyMatch(e -> e.contains("=h")),
                "Expected 'h' of 'hello'");
        assertTrue(backend.puts.stream().anyMatch(e -> e.contains("=w")),
                "Expected 'w' of 'world'");
    }

    // --- Large dataset performance smoke test ---

    @Test
    void largeDataset_noOutOfMemory() {
        // 100k items — should not cause OOM or slow layout
        List<String> big = new ArrayList<>();
        for (int i = 0; i < 100_000; i++) big.add("row " + i);
        VirtualListNode n = VirtualList.of(big, 20);
        engine.layout(n, 0, 0, 80, 24);
        assertEquals(20, n.getHeight());
        // Navigate to the middle
        for (int i = 0; i < 50_000; i++) n.selectDown();
        assertEquals(50_000, n.getSelectedIndex());
    }
}
