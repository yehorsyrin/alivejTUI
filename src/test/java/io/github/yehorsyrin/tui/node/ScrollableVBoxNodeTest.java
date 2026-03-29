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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ScrollableVBoxNode} — layout, scroll, and rendering.
 *
 * @author Jarvis (AI)
 */
class ScrollableVBoxNodeTest {

    static class FakeBackend implements TerminalBackend {
        final List<String> putChars = new ArrayList<>();
        @Override public void init() {}
        @Override public void shutdown() {}
        @Override public int getWidth()  { return 80; }
        @Override public int getHeight() { return 24; }
        @Override public void putChar(int col, int row, char c, Style style) { putChars.add(col + "," + row + "=" + c); }
        @Override public void flush() {}
        @Override public void hideCursor() {}
        @Override public void showCursor() {}
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() { putChars.clear(); }
        @Override public KeyEvent readKey() { return KeyEvent.of(KeyType.EOF); }
        @Override public void setResizeListener(Runnable r) {}
    }

    // --- Construction ---

    @Test
    void defaultScrollOffset_isZero() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(5);
        assertEquals(0, box.getScrollOffset());
    }

    @Test
    void maxHeight_stored() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(10);
        assertEquals(10, box.getMaxHeight());
    }

    @Test
    void maxHeightZero_clampedToOne() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(0);
        assertEquals(1, box.getMaxHeight());
    }

    // --- Scroll ---

    @Test
    void scrollDown_incrementsOffset() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(3, Text.of("a"), Text.of("b"), Text.of("c"), Text.of("d"), Text.of("e"));
        LayoutEngine le = new LayoutEngine();
        le.layout(box, 0, 0, 80, 24);
        box.scrollDown(1);
        assertEquals(1, box.getScrollOffset());
    }

    @Test
    void scrollUp_decrementsOffset() {
        // 5 children, maxHeight=3 → maxOffset=2; scrollDown(2)→2, scrollUp(1)→1
        ScrollableVBoxNode box = new ScrollableVBoxNode(3,
            Text.of("a"), Text.of("b"), Text.of("c"), Text.of("d"), Text.of("e"));
        LayoutEngine le = new LayoutEngine();
        le.layout(box, 0, 0, 80, 24);
        box.scrollDown(2);
        box.scrollUp(1);
        assertEquals(1, box.getScrollOffset());
    }

    @Test
    void scrollUp_clampedToZero() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(3);
        box.scrollUp(5);
        assertEquals(0, box.getScrollOffset());
    }

    @Test
    void scrollDown_clampedToContentMinusMaxHeight() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(3,
            Text.of("a"), Text.of("b"), Text.of("c"), Text.of("d"), Text.of("e"));
        LayoutEngine le = new LayoutEngine();
        le.layout(box, 0, 0, 80, 24);
        // 5 rows content, maxHeight=3 → max scrollOffset = 2
        box.scrollDown(100);
        assertEquals(2, box.getScrollOffset());
    }

    @Test
    void setScrollOffset_appliedDirectly() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(5);
        box.setScrollOffset(3);
        assertEquals(3, box.getScrollOffset());
    }

    // --- Layout ---

    @Test
    void layout_heightClampedToMaxHeight() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(3,
            Text.of("row0"), Text.of("row1"), Text.of("row2"), Text.of("row3"), Text.of("row4"));
        LayoutEngine le = new LayoutEngine();
        le.layout(box, 0, 0, 80, 24);
        assertEquals(3, box.getHeight());
    }

    @Test
    void layout_heightEqualsContentWhenFewerRowsThanMax() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(10, Text.of("a"), Text.of("b"));
        LayoutEngine le = new LayoutEngine();
        le.layout(box, 0, 0, 80, 24);
        assertEquals(2, box.getHeight());
    }

    @Test
    void layout_contentHeightRecorded() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(3,
            Text.of("a"), Text.of("b"), Text.of("c"), Text.of("d"));
        LayoutEngine le = new LayoutEngine();
        le.layout(box, 0, 0, 80, 24);
        assertEquals(4, box.getContentHeight());
    }

    // --- Rendering (via Renderer) ---

    @Test
    void render_showsFirstRows_whenNotScrolled() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(3,
            Text.of("A"), Text.of("B"), Text.of("C"), Text.of("D"), Text.of("E"));
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        renderer.render(box);

        // Should show rows 0,1,2 → A at y=0, B at y=1, C at y=2
        assertTrue(backend.putChars.contains("0,0=A"));
        assertTrue(backend.putChars.contains("0,1=B"));
        assertTrue(backend.putChars.contains("0,2=C"));
        // D and E should NOT appear
        assertFalse(backend.putChars.stream().anyMatch(s -> s.endsWith("=D")));
        assertFalse(backend.putChars.stream().anyMatch(s -> s.endsWith("=E")));
    }

    @Test
    void render_showsScrolledRows_afterScrollDown() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(3,
            Text.of("A"), Text.of("B"), Text.of("C"), Text.of("D"), Text.of("E"));
        LayoutEngine le = new LayoutEngine();
        le.layout(box, 0, 0, 80, 24);
        box.scrollDown(2);  // skip A and B

        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        renderer.render(box);

        // Should show rows C(0), D(1), E(2) — remapped to y=0,1,2
        assertTrue(backend.putChars.contains("0,0=C"));
        assertTrue(backend.putChars.contains("0,1=D"));
        assertTrue(backend.putChars.contains("0,2=E"));
        assertFalse(backend.putChars.stream().anyMatch(s -> s.endsWith("=A")));
        assertFalse(backend.putChars.stream().anyMatch(s -> s.endsWith("=B")));
    }

    @Test
    void render_emptyContainer_noException() {
        ScrollableVBoxNode box = new ScrollableVBoxNode(5);
        FakeBackend backend = new FakeBackend();
        assertDoesNotThrow(() -> new Renderer(backend).render(box));
    }

    @Test
    void factory_scrollableVbox_createsNode() {
        ScrollableVBoxNode box = ScrollableVBox.of(5, Text.of("x"));
        assertEquals(5, box.getMaxHeight());
        assertEquals(1, box.getChildren().size());
    }
}
