package io.github.yehorsyrin.tui.diff;

import io.github.yehorsyrin.tui.layout.LayoutEngine;
import io.github.yehorsyrin.tui.node.Text;
import io.github.yehorsyrin.tui.node.TextNode;
import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TreeFlattener} multi-line / word-wrap rendering.
 *
 * @author Jarvis (AI)
 */
class TreeFlattenerWordWrapTest {

    private final LayoutEngine engine = new LayoutEngine();
    private final TreeFlattener flattener = new TreeFlattener();

    private Map<String, CellState> flattenAfterLayout(TextNode t, int x, int y, int w, int h) {
        engine.layout(t, x, y, w, h);
        return flattener.flatten(t);
    }

    @Test
    void wrappedText_cellsOnMultipleRows() {
        TextNode t = Text.of("hello world").wrap();
        Map<String, CellState> cells = flattenAfterLayout(t, 0, 0, 7, 24);

        // Row 0: "hello"
        assertEquals('h', cells.get("0,0").character());
        assertEquals('e', cells.get("1,0").character());
        assertEquals('l', cells.get("2,0").character());
        assertEquals('l', cells.get("3,0").character());
        assertEquals('o', cells.get("4,0").character());

        // Row 1: "world"
        assertEquals('w', cells.get("0,1").character());
        assertEquals('o', cells.get("1,1").character());
        assertEquals('r', cells.get("2,1").character());
        assertEquals('l', cells.get("3,1").character());
        assertEquals('d', cells.get("4,1").character());
    }

    @Test
    void wrappedText_twoWords_separateRows() {
        TextNode t = Text.of("ab cd").wrap();
        Map<String, CellState> cells = flattenAfterLayout(t, 0, 0, 4, 24);

        assertEquals('a', cells.get("0,0").character());
        assertEquals('b', cells.get("1,0").character());
        assertEquals('c', cells.get("0,1").character());
        assertEquals('d', cells.get("1,1").character());
    }

    @Test
    void wrappedText_offsetPosition() {
        TextNode t = Text.of("hi\nthere");
        Map<String, CellState> cells = flattenAfterLayout(t, 2, 5, 80, 24);

        // "hi" at (2,5)
        assertEquals('h', cells.get("2,5").character());
        assertEquals('i', cells.get("3,5").character());

        // "there" at (2,6)
        assertEquals('t', cells.get("2,6").character());
        assertEquals('h', cells.get("3,6").character());
        assertEquals('e', cells.get("4,6").character());
    }

    @Test
    void wrappedText_styleApplied() {
        Style boldStyle = Style.DEFAULT.withBold(true);
        TextNode t = new TextNode("hello world", boldStyle).wrap();
        Map<String, CellState> cells = flattenAfterLayout(t, 0, 0, 7, 24);

        assertTrue(cells.get("0,0").style().isBold());
        assertTrue(cells.get("0,1").style().isBold());
    }

    @Test
    void wrappedText_noExtraCellsBeyondContent() {
        TextNode t = Text.of("hi\nbye").wrap();
        Map<String, CellState> cells = flattenAfterLayout(t, 0, 0, 80, 24);

        // Only "hi" (2 cells on row 0) and "bye" (3 cells on row 1)
        assertEquals(5, cells.size());
    }

    @Test
    void explicitNewline_treatedAsWrap() {
        TextNode t = Text.of("a\nb");
        // No .wrap() call — newline in text should trigger wrapping anyway
        Map<String, CellState> cells = flattenAfterLayout(t, 0, 0, 80, 24);

        assertNotNull(cells.get("0,0"));
        assertEquals('a', cells.get("0,0").character());
        assertNotNull(cells.get("0,1"));
        assertEquals('b', cells.get("0,1").character());
    }
}
