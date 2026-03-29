package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.layout.LayoutEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ViewportNode} and {@link Viewport} factory (public API).
 *
 * @author Jarvis (AI)
 */
class ViewportNodeTest {

    private final LayoutEngine engine = new LayoutEngine();

    // --- Factory ---

    @Test
    void viewport_of_createsViewportWithMaxHeight() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 5);
        assertEquals(5, vp.getMaxHeight());
    }

    @Test
    void viewport_plain_scrollbarDisabled() {
        ViewportNode vp = Viewport.plain(Text.of("hi"), 5);
        assertFalse(vp.isShowScrollbar());
    }

    @Test
    void viewport_of_scrollbarEnabledByDefault() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 5);
        assertTrue(vp.isShowScrollbar());
    }

    @Test
    void viewport_showScrollbar_chainable() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 5);
        assertSame(vp, vp.showScrollbar(false));
        assertFalse(vp.isShowScrollbar());
    }

    // --- Scroll state ---

    @Test
    void initialScrollOffset_isZero() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 5);
        assertEquals(0, vp.getScrollOffset());
    }

    @Test
    void setScrollOffset_clampedToZero() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 5);
        vp.setScrollOffset(-5);
        assertEquals(0, vp.getScrollOffset());
    }

    @Test
    void setScrollOffset_clampedToScrollableRows() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 3);
        vp.setContentHeight(10);
        // scrollableRows = 10 - 3 = 7
        vp.setScrollOffset(100);
        assertEquals(7, vp.getScrollOffset());
    }

    @Test
    void scrollableRows_whenContentFits_zero() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 10);
        vp.setContentHeight(5);
        assertEquals(0, vp.scrollableRows());
        assertFalse(vp.isScrollable());
    }

    @Test
    void scrollableRows_whenContentExceeds() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 5);
        vp.setContentHeight(10);
        assertEquals(5, vp.scrollableRows());
        assertTrue(vp.isScrollable());
    }

    @Test
    void scrollDown_incrementsOffset() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 3);
        vp.setContentHeight(10);
        vp.scrollDown();
        assertEquals(1, vp.getScrollOffset());
    }

    @Test
    void scrollUp_decrementsOffset() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 3);
        vp.setContentHeight(10);
        vp.setScrollOffset(3);
        vp.scrollUp();
        assertEquals(2, vp.getScrollOffset());
    }

    @Test
    void scrollUp_atTopClampedToZero() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 3);
        vp.setContentHeight(10);
        vp.scrollUp();
        assertEquals(0, vp.getScrollOffset());
    }

    @Test
    void pageDown_incrementsByMaxHeight() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 3);
        vp.setContentHeight(20);
        vp.pageDown();
        assertEquals(3, vp.getScrollOffset());
    }

    @Test
    void pageUp_decrementsbyMaxHeight() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 3);
        vp.setContentHeight(20);
        vp.setScrollOffset(6);
        vp.pageUp();
        assertEquals(3, vp.getScrollOffset());
    }

    @Test
    void scrollToTop_setsOffsetToZero() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 3);
        vp.setContentHeight(20);
        vp.setScrollOffset(10);
        vp.scrollToTop();
        assertEquals(0, vp.getScrollOffset());
    }

    @Test
    void scrollToBottom_setsOffsetToScrollableRows() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 3);
        vp.setContentHeight(10);
        vp.scrollToBottom();
        assertEquals(7, vp.getScrollOffset()); // 10 - 3 = 7
    }

    // --- Layout ---

    @Test
    void layout_contentFits_heightEqualsContent() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 10);
        engine.layout(vp, 0, 0, 20, 24);
        // TextNode "hi" = height 1, which is < maxHeight 10
        assertEquals(1, vp.getHeight());
        assertEquals(1, vp.getContentHeight());
    }

    @Test
    void layout_contentExceedsMax_heightEqualsMax() {
        // Create a VBox with many lines
        io.github.yehorsyrin.tui.node.VBoxNode vbox = io.github.yehorsyrin.tui.node.VBox.of(
                Text.of("L1"), Text.of("L2"), Text.of("L3"),
                Text.of("L4"), Text.of("L5"), Text.of("L6")
        );
        ViewportNode vp = Viewport.of(vbox, 3);
        engine.layout(vp, 0, 0, 20, 24);
        assertEquals(3, vp.getHeight());
        assertEquals(6, vp.getContentHeight());
    }

    @Test
    void layout_widthReservedForScrollbar() {
        ViewportNode vp = Viewport.of(Text.of("hi"), 3);
        engine.layout(vp, 0, 0, 20, 24);
        // With scrollbar enabled, content gets width 19, vp gets 20
        assertEquals(20, vp.getWidth());
    }

    @Test
    void layout_noScrollbar_viewportWidthEqualsAvailable() {
        ViewportNode vp = Viewport.plain(Text.of("hi"), 3);
        engine.layout(vp, 0, 0, 20, 24);
        // Without scrollbar the viewport itself still gets the full available width
        assertEquals(20, vp.getWidth());
    }
}
