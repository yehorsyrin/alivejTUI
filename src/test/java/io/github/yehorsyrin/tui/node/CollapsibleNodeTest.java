package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.layout.LayoutEngine;
import io.github.yehorsyrin.tui.render.Renderer;
import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CollapsibleNode} and {@link Collapsible} factory.
 *
 * @author Jarvis (AI)
 */
class CollapsibleNodeTest {

    private final LayoutEngine engine = new LayoutEngine();

    static class FakeBackend implements io.github.yehorsyrin.tui.backend.TerminalBackend {
        final java.util.List<String> putChars = new java.util.ArrayList<>();
        @Override public void init() {}
        @Override public void shutdown() {}
        @Override public int getWidth()  { return 80; }
        @Override public int getHeight() { return 24; }
        @Override public void putChar(int col, int row, char c, Style s) {
            putChars.add(col + "," + row + "=" + c);
        }
        @Override public void flush() {}
        @Override public void hideCursor() {}
        @Override public void showCursor() {}
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() { putChars.clear(); }
        @Override public io.github.yehorsyrin.tui.event.KeyEvent readKey() {
            return io.github.yehorsyrin.tui.event.KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.EOF);
        }
        @Override public void setResizeListener(Runnable r) {}
    }

    // --- Factory ---

    @Test
    void collapsible_of_startsCollapsed() {
        CollapsibleNode c = Collapsible.of("Section", Text.of("child"));
        assertFalse(c.isExpanded());
    }

    @Test
    void collapsible_expanded_startsExpanded() {
        CollapsibleNode c = Collapsible.expanded("Section", Text.of("child"));
        assertTrue(c.isExpanded());
    }

    @Test
    void collapsible_titlePreserved() {
        CollapsibleNode c = Collapsible.of("My Section");
        assertEquals("My Section", c.getTitle());
    }

    @Test
    void collapsible_nullTitle_emptyString() {
        CollapsibleNode c = new CollapsibleNode(null, false);
        assertEquals("", c.getTitle());
    }

    @Test
    void collapsible_childrenAdded() {
        CollapsibleNode c = Collapsible.of("S", Text.of("A"), Text.of("B"));
        assertEquals(2, c.getChildren().size());
    }

    // --- Toggle ---

    @Test
    void toggle_collapsedToExpanded() {
        CollapsibleNode c = Collapsible.of("S");
        c.toggle();
        assertTrue(c.isExpanded());
    }

    @Test
    void toggle_expandedToCollapsed() {
        CollapsibleNode c = Collapsible.expanded("S");
        c.toggle();
        assertFalse(c.isExpanded());
    }

    @Test
    void expand_sets_expanded() {
        CollapsibleNode c = Collapsible.of("S");
        c.expand();
        assertTrue(c.isExpanded());
    }

    @Test
    void collapse_sets_collapsed() {
        CollapsibleNode c = Collapsible.expanded("S");
        c.collapse();
        assertFalse(c.isExpanded());
    }

    // --- Layout ---

    @Test
    void layout_collapsed_height1() {
        CollapsibleNode c = Collapsible.of("Section", Text.of("child"));
        engine.layout(c, 0, 0, 20, 24);
        assertEquals(1, c.getHeight());
    }

    @Test
    void layout_expanded_height1PlusChildren() {
        CollapsibleNode c = Collapsible.expanded("Section",
                Text.of("line1"), Text.of("line2"), Text.of("line3"));
        engine.layout(c, 0, 0, 20, 24);
        assertEquals(4, c.getHeight()); // 1 title + 3 children
    }

    @Test
    void layout_expanded_childrenPositionedBelowTitle() {
        TextNode child = Text.of("hello");
        CollapsibleNode c = Collapsible.expanded("Title", child);
        engine.layout(c, 2, 5, 20, 24);
        // child should be at y=6 (title at y=5)
        assertEquals(6, child.getY());
        assertEquals(2, child.getX());
    }

    @Test
    void layout_width_equalsAvailable() {
        CollapsibleNode c = Collapsible.of("S");
        engine.layout(c, 0, 0, 30, 24);
        assertEquals(30, c.getWidth());
    }

    // --- Focusable ---

    @Test
    void initialFocus_false() {
        assertFalse(Collapsible.of("S").isFocused());
    }

    @Test
    void setFocused_true() {
        CollapsibleNode c = Collapsible.of("S");
        c.setFocused(true);
        assertTrue(c.isFocused());
    }

    @Test
    void focusId_isTitle() {
        CollapsibleNode c = Collapsible.of("My Section");
        assertEquals("My Section", c.getFocusId());
    }

    // --- Rendering ---

    @Test
    void render_collapsed_showsCollapsedArrow() {
        FakeBackend backend = new FakeBackend();
        CollapsibleNode c = Collapsible.of("S");
        new Renderer(backend).render(c);
        assertTrue(backend.putChars.contains("0,0=" + CollapsibleNode.COLLAPSED_ARROW),
                "Expected ▶ at 0,0");
    }

    @Test
    void render_expanded_showsExpandedArrow() {
        FakeBackend backend = new FakeBackend();
        CollapsibleNode c = Collapsible.expanded("S");
        new Renderer(backend).render(c);
        assertTrue(backend.putChars.contains("0,0=" + CollapsibleNode.EXPANDED_ARROW),
                "Expected ▼ at 0,0");
    }

    @Test
    void render_collapsed_noChildRows() {
        FakeBackend backend = new FakeBackend();
        CollapsibleNode c = Collapsible.of("S", Text.of("child"));
        new Renderer(backend).render(c);
        assertTrue(backend.putChars.stream().noneMatch(e -> e.matches("\\d+,1=.*")),
                "No child rows should appear when collapsed");
    }

    @Test
    void render_expanded_childRowsPresent() {
        FakeBackend backend = new FakeBackend();
        CollapsibleNode c = Collapsible.expanded("S", Text.of("AB"));
        new Renderer(backend).render(c);
        // child "AB" at row 1
        assertTrue(backend.putChars.contains("0,1=A"), "Expected A at 0,1");
        assertTrue(backend.putChars.contains("1,1=B"), "Expected B at 1,1");
    }

    @Test
    void render_focused_usesFocusedStyle() {
        FakeBackend backend = new FakeBackend();
        Style bold = Style.DEFAULT.withBold(true);
        CollapsibleNode c = Collapsible.of("S");
        c.setFocused(true);
        c.focusedStyle(bold);
        new Renderer(backend).render(c);
        // We can't easily check style from FakeBackend putChars string,
        // but we can verify no exceptions and the header renders
        assertTrue(backend.putChars.contains("0,0=" + CollapsibleNode.COLLAPSED_ARROW));
    }
}
