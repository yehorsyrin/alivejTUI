package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.layout.LayoutEngine;
import io.github.yehorsyrin.tui.render.Renderer;
import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TableNode} and {@link Table} factory.
 *
 * @author Jarvis (AI)
 */
class TableNodeTest {

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
    void table_of_headersPreserved() {
        TableNode t = Table.of(List.of("A", "B"), List.of(), 5);
        assertEquals(List.of("A", "B"), t.getHeaders());
    }

    @Test
    void table_of_rowsPreserved() {
        List<List<String>> rows = List.of(List.of("1", "2"), List.of("3", "4"));
        TableNode t = Table.of(List.of("A", "B"), rows, 5);
        assertEquals(2, t.rowCount());
    }

    @Test
    void table_emptyHeaders_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> Table.of(List.of(), List.of(), 5));
    }

    @Test
    void table_nullRows_isEmpty() {
        TableNode t = new TableNode(List.of("H"), null, 5);
        assertEquals(0, t.rowCount());
    }

    // --- Column count / row count ---

    @Test
    void columnCount() {
        TableNode t = Table.of(List.of("A", "B", "C"), List.of(), 5);
        assertEquals(3, t.columnCount());
    }

    @Test
    void rowCount_empty() {
        TableNode t = Table.of(List.of("A"), List.of(), 5);
        assertEquals(0, t.rowCount());
    }

    // --- Selection ---

    @Test
    void initialSelectedRow_withRows_isZero() {
        TableNode t = Table.of(List.of("A"), List.of(List.of("x")), 5);
        assertEquals(0, t.getSelectedRow());
    }

    @Test
    void initialSelectedRow_emptyRows_isMinusOne() {
        TableNode t = Table.of(List.of("A"), List.of(), 5);
        assertEquals(-1, t.getSelectedRow());
    }

    @Test
    void selectDown_advancesSelection() {
        TableNode t = Table.of(List.of("A"),
                List.of(List.of("r0"), List.of("r1"), List.of("r2")), 5);
        t.selectDown();
        assertEquals(1, t.getSelectedRow());
    }

    @Test
    void selectDown_clampedAtEnd() {
        TableNode t = Table.of(List.of("A"), List.of(List.of("r0")), 5);
        t.selectDown();
        assertEquals(0, t.getSelectedRow()); // clamp at 0
    }

    @Test
    void selectUp_decrementsSelection() {
        TableNode t = Table.of(List.of("A"),
                List.of(List.of("r0"), List.of("r1")), 5);
        t.selectDown();
        t.selectUp();
        assertEquals(0, t.getSelectedRow());
    }

    @Test
    void selectUp_clampedAtZero() {
        TableNode t = Table.of(List.of("A"), List.of(List.of("r0"), List.of("r1")), 5);
        t.selectUp();
        assertEquals(0, t.getSelectedRow());
    }

    @Test
    void getSelectedRowData_returnsCorrectRow() {
        TableNode t = Table.of(List.of("A", "B"),
                List.of(List.of("r0a", "r0b"), List.of("r1a", "r1b")), 5);
        t.selectDown();
        assertEquals(List.of("r1a", "r1b"), t.getSelectedRowData());
    }

    // --- Scrolling ---

    @Test
    void scrollableRows_whenAllFit_zero() {
        TableNode t = Table.of(List.of("A"),
                List.of(List.of("x"), List.of("y")), 5);
        assertEquals(0, t.scrollableRows());
        assertFalse(t.isScrollable());
    }

    @Test
    void scrollableRows_whenExceeds() {
        TableNode t = Table.of(List.of("A"),
                List.of(List.of("a"), List.of("b"), List.of("c")), 2);
        assertEquals(1, t.scrollableRows());
        assertTrue(t.isScrollable());
    }

    @Test
    void selectDown_scrollsWhenNeeded() {
        TableNode t = Table.of(List.of("A"),
                List.of(List.of("a"), List.of("b"), List.of("c")), 2);
        // maxHeight=2, rows=3 → scroll when going past row 1
        t.selectDown(); // row 1, offset=0
        t.selectDown(); // row 2, offset should become 1
        assertEquals(2, t.getSelectedRow());
        assertEquals(1, t.getScrollOffset());
    }

    // --- Layout ---

    @Test
    void layout_withBorders_heightIs4PlusVisibleRows() {
        TableNode t = Table.of(List.of("A", "B"),
                List.of(List.of("x", "y"), List.of("p", "q")), 5);
        engine.layout(t, 0, 0, 30, 24);
        // 2 data rows, all visible → height = 4 + 2 = 6
        assertEquals(6, t.getHeight());
    }

    @Test
    void layout_withBorders_heightCapAtMaxHeight() {
        TableNode t = Table.of(List.of("A"),
                List.of(List.of("a"), List.of("b"), List.of("c")), 2);
        engine.layout(t, 0, 0, 20, 24);
        // maxHeight=2 data rows → height = 4 + 2 = 6
        assertEquals(6, t.getHeight());
    }

    @Test
    void layout_noBorders_height1PlusVisibleRows() {
        TableNode t = Table.of(List.of("A"),
                List.of(List.of("x"), List.of("y")), 5)
                .showBorders(false);
        engine.layout(t, 0, 0, 20, 24);
        assertEquals(3, t.getHeight()); // 1 header + 2 rows
    }

    @Test
    void layout_colWidthsSet() {
        TableNode t = Table.of(List.of("Col1", "Col2"), List.of(), 5);
        engine.layout(t, 0, 0, 30, 24);
        assertNotNull(t.getColWidths());
        assertEquals(2, t.getColWidths().length);
    }

    @Test
    void layout_widthEqualsAvailable() {
        TableNode t = Table.of(List.of("A"), List.of(), 5);
        engine.layout(t, 0, 0, 40, 24);
        assertEquals(40, t.getWidth());
    }

    // --- Rendering ---

    @Test
    void render_withBorders_topLeftCorner() {
        FakeBackend backend = new FakeBackend();
        TableNode t = Table.of(List.of("A"), List.of(), 5);
        new Renderer(backend).render(t);
        assertTrue(backend.putChars.contains("0,0=" + TableNode.TL),
                "Expected ┌ at 0,0");
    }

    @Test
    void render_withBorders_topRightCorner() {
        FakeBackend backend = new FakeBackend();
        TableNode t = Table.of(List.of("A"), List.of(), 5);
        new Renderer(backend).render(t);
        // Width is whatever the Renderer allocated (80 by default)
        boolean found = backend.putChars.stream().anyMatch(e -> e.endsWith("=" + TableNode.TR));
        assertTrue(found, "Expected ┐ corner: " + backend.putChars);
    }

    @Test
    void render_withBorders_headerSeparator_hasPlus() {
        FakeBackend backend = new FakeBackend();
        TableNode t = Table.of(List.of("A", "B"), List.of(List.of("x", "y")), 5);
        new Renderer(backend).render(t);
        // Row 2 (header sep) should contain at least one ┼ or ├/┤
        boolean found = backend.putChars.stream()
                .filter(e -> e.matches("\\d+,2=.*"))
                .anyMatch(e -> e.endsWith("=" + TableNode.PLUS)
                        || e.endsWith("=" + TableNode.LT)
                        || e.endsWith("=" + TableNode.RT));
        assertTrue(found, "Expected header separator characters on row 2");
    }

    @Test
    void render_headerContainsHeaderText() {
        FakeBackend backend = new FakeBackend();
        TableNode t = Table.of(List.of("Name"), List.of(), 5);
        new Renderer(backend).render(t);
        // Row 1 should contain 'N', 'a', 'm', 'e' of "Name"
        assertTrue(backend.putChars.stream().anyMatch(e -> e.matches("\\d+,1=N")));
    }

    @Test
    void render_dataRowPresent() {
        FakeBackend backend = new FakeBackend();
        TableNode t = Table.of(List.of("A"), List.of(List.of("hello")), 5);
        new Renderer(backend).render(t);
        // Data row at row 3 (top+header+sep = 3 rows before data)
        assertTrue(backend.putChars.stream().anyMatch(e -> e.matches("\\d+,3=h")));
    }
}
