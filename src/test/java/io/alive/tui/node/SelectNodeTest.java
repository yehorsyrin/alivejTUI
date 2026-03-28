package io.alive.tui.node;

import io.alive.tui.layout.LayoutEngine;
import io.alive.tui.render.Renderer;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SelectNode} and {@link Select} factory.
 *
 * @author Jarvis (AI)
 */
class SelectNodeTest {

    private final LayoutEngine engine = new LayoutEngine();

    // --- Factory ---

    @Test
    void select_of_varargs_firstSelected() {
        SelectNode s = Select.of("A", "B", "C");
        assertEquals(0, s.getSelectedIndex());
        assertEquals("A", s.getSelectedOption());
    }

    @Test
    void select_of_list_firstSelected() {
        SelectNode s = Select.of(List.of("X", "Y"));
        assertEquals(0, s.getSelectedIndex());
    }

    @Test
    void select_of_withIndexAndCallback() {
        SelectNode s = Select.of(List.of("A", "B", "C"), 2, null);
        assertEquals(2, s.getSelectedIndex());
        assertEquals("C", s.getSelectedOption());
    }

    @Test
    void select_of_indexClamped() {
        SelectNode s = new SelectNode(List.of("A", "B"), 99, null);
        assertEquals(1, s.getSelectedIndex());
    }

    @Test
    void select_emptyOptions_throws() {
        assertThrows(IllegalArgumentException.class, () -> new SelectNode(List.of(), 0, null));
    }

    // --- Open / close ---

    @Test
    void initialState_closed() {
        assertFalse(Select.of("A").isOpen());
    }

    @Test
    void open_setsOpenTrue() {
        SelectNode s = Select.of("A", "B");
        s.open();
        assertTrue(s.isOpen());
    }

    @Test
    void close_setsOpenFalse() {
        SelectNode s = Select.of("A", "B");
        s.open();
        s.close();
        assertFalse(s.isOpen());
    }

    @Test
    void toggle_opensAndCloses() {
        SelectNode s = Select.of("A", "B");
        s.toggle();
        assertTrue(s.isOpen());
        s.toggle();
        assertFalse(s.isOpen());
    }

    @Test
    void open_resetsHoverToSelectedIndex() {
        SelectNode s = Select.of("A", "B", "C");
        s.open();
        s.moveDown();  // hover = 1
        s.close();
        s.open();      // hover should reset to selectedIndex = 0
        assertEquals(0, s.getHoverIndex());
    }

    // --- Navigation ---

    @Test
    void moveDown_advancesHover() {
        SelectNode s = Select.of("A", "B", "C");
        s.open();
        s.moveDown();
        assertEquals(1, s.getHoverIndex());
    }

    @Test
    void moveDown_wrapsAround() {
        SelectNode s = Select.of("A", "B", "C");
        s.open();
        s.moveDown(); s.moveDown(); s.moveDown(); // 0→1→2→0
        assertEquals(0, s.getHoverIndex());
    }

    @Test
    void moveUp_decrementsHover() {
        SelectNode s = Select.of("A", "B", "C");
        s.open();
        s.moveDown(); // hover=1
        s.moveUp();   // hover=0
        assertEquals(0, s.getHoverIndex());
    }

    @Test
    void moveUp_wrapsAround() {
        SelectNode s = Select.of("A", "B", "C");
        s.open();
        s.moveUp(); // hover wraps to 2
        assertEquals(2, s.getHoverIndex());
    }

    // --- Accept ---

    @Test
    void accept_closesDropdown() {
        SelectNode s = Select.of("A", "B", "C");
        s.open();
        s.accept();
        assertFalse(s.isOpen());
    }

    @Test
    void accept_setsSelectedIndexToHover() {
        SelectNode s = Select.of("A", "B", "C");
        s.open();
        s.moveDown(); // hover=1
        s.accept();
        assertEquals(1, s.getSelectedIndex());
        assertEquals("B", s.getSelectedOption());
    }

    @Test
    void accept_firesCallback_whenSelectionChanges() {
        AtomicInteger count = new AtomicInteger();
        SelectNode s = new SelectNode(List.of("A", "B", "C"), 0, count::incrementAndGet);
        s.open();
        s.moveDown();
        s.accept();
        assertEquals(1, count.get());
    }

    @Test
    void accept_noCallback_whenSelectionUnchanged() {
        AtomicInteger count = new AtomicInteger();
        SelectNode s = new SelectNode(List.of("A", "B"), 0, count::incrementAndGet);
        s.open();
        s.accept(); // hover=0 = selectedIndex=0, no change
        assertEquals(0, count.get());
    }

    // --- Focusable ---

    @Test
    void initialFocus_false() {
        assertFalse(Select.of("A").isFocused());
    }

    @Test
    void setFocused_true() {
        SelectNode s = Select.of("A");
        s.setFocused(true);
        assertTrue(s.isFocused());
    }

    @Test
    void focusId_isFirstOption() {
        SelectNode s = Select.of("Alpha", "Beta");
        assertEquals("Alpha", s.getFocusId());
    }

    // --- Layout ---

    @Test
    void layout_closed_height1() {
        SelectNode s = Select.of("A", "B", "C");
        engine.layout(s, 0, 0, 20, 24);
        assertEquals(1, s.getHeight());
    }

    @Test
    void layout_open_height1PlusOptions() {
        SelectNode s = Select.of("A", "B", "C");
        s.open();
        engine.layout(s, 0, 0, 20, 24);
        assertEquals(4, s.getHeight()); // 1 + 3
    }

    @Test
    void layout_width_equalsAvailable() {
        SelectNode s = Select.of("A");
        engine.layout(s, 0, 0, 15, 24);
        assertEquals(15, s.getWidth());
    }

    // --- Rendering via Renderer ---

    static class FakeBackend implements io.alive.tui.backend.TerminalBackend {
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
        @Override public io.alive.tui.event.KeyEvent readKey() {
            return io.alive.tui.event.KeyEvent.of(io.alive.tui.event.KeyType.EOF);
        }
        @Override public void setResizeListener(Runnable r) {}
    }

    @Test
    void render_closed_headerContainsDownArrow() {
        FakeBackend backend = new FakeBackend();
        SelectNode s = Select.of("Opt");
        new Renderer(backend).render(s);
        boolean found = backend.putChars.stream().anyMatch(e -> e.endsWith("=" + SelectNode.ARROW_DOWN));
        assertTrue(found, "ARROW_DOWN should appear in closed header");
    }

    @Test
    void render_open_headerContainsUpArrow() {
        FakeBackend backend = new FakeBackend();
        SelectNode s = Select.of("Opt");
        s.open();
        new Renderer(backend).render(s);
        boolean found = backend.putChars.stream().anyMatch(e -> e.endsWith("=" + SelectNode.ARROW_UP));
        assertTrue(found, "ARROW_UP should appear in open header");
    }

    @Test
    void render_closed_noOptionRows() {
        FakeBackend backend = new FakeBackend();
        SelectNode s = Select.of("A", "B", "C");
        new Renderer(backend).render(s);
        // Only row 0 should have content; row 1+ should not
        assertTrue(backend.putChars.stream().noneMatch(e -> e.matches("\\d+,1=.*")));
    }

    @Test
    void render_open_allOptionRowsPresent() {
        FakeBackend backend = new FakeBackend();
        SelectNode s = Select.of("A", "B", "C");
        s.open();
        new Renderer(backend).render(s);
        // Rows 1, 2, 3 should have content
        assertTrue(backend.putChars.stream().anyMatch(e -> e.matches("\\d+,1=.*")));
        assertTrue(backend.putChars.stream().anyMatch(e -> e.matches("\\d+,2=.*")));
        assertTrue(backend.putChars.stream().anyMatch(e -> e.matches("\\d+,3=.*")));
    }

    @Test
    void render_open_hoverRowHasCursorPrefix() {
        FakeBackend backend = new FakeBackend();
        SelectNode s = Select.of("A", "B", "C");
        s.open(); // hover=0 → row 1 gets cursor
        new Renderer(backend).render(s);
        // Col 0, row 1 should be '›'
        assertTrue(backend.putChars.contains("0,1=" + SelectNode.CURSOR.charAt(0)),
                "Expected › at 0,1; got: " + backend.putChars);
    }
}
