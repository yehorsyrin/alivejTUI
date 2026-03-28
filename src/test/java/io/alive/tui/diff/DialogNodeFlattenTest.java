package io.alive.tui.diff;

import io.alive.tui.layout.LayoutEngine;
import io.alive.tui.node.Dialog;
import io.alive.tui.node.DialogNode;
import io.alive.tui.node.Text;
import io.alive.tui.node.TextNode;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TreeFlattener} rendering of {@link DialogNode}.
 *
 * @author Jarvis (AI)
 */
class DialogNodeFlattenTest {

    private final LayoutEngine engine   = new LayoutEngine();
    private final TreeFlattener flattener = new TreeFlattener();

    private Map<String, CellState> lay(DialogNode d, int x, int y, int w, int h) {
        engine.layout(d, x, y, w, h);
        return flattener.flatten(d);
    }

    @Test
    void corners_correct() {
        DialogNode d = Dialog.of(Text.of("hi"));
        Map<String, CellState> cells = lay(d, 0, 0, 10, 5);

        assertEquals('╭', cells.get("0,0").character());
        assertEquals('╮', cells.get("9,0").character());
        assertEquals('╰', cells.get("0,2").character());
        assertEquals('╯', cells.get("9,2").character());
    }

    @Test
    void topAndBottomBorders_hLine() {
        DialogNode d = Dialog.of(Text.of("hi"));
        Map<String, CellState> cells = lay(d, 0, 0, 6, 5);
        // width=6, top row: cols 1-4 are H_LINE
        for (int col = 1; col <= 4; col++) {
            assertEquals('─', cells.get(col + ",0").character(),
                    "Expected H_LINE at top col " + col);
            assertEquals('─', cells.get(col + ",2").character(),
                    "Expected H_LINE at bottom col " + col);
        }
    }

    @Test
    void leftAndRightEdges_vLine() {
        DialogNode d = Dialog.of(Text.of("hi"));
        Map<String, CellState> cells = lay(d, 0, 0, 6, 5);
        // height=3, only row 1 is between borders
        assertEquals('│', cells.get("0,1").character());
        assertEquals('│', cells.get("5,1").character());
    }

    @Test
    void withTitle_titleAppearsInTopBorder() {
        DialogNode d = Dialog.of("OK", Text.of("hi"));
        Map<String, CellState> cells = lay(d, 0, 0, 20, 5);

        // "─ OK " should appear starting at col 1
        assertEquals('─', cells.get("1,0").character());
        assertEquals(' ', cells.get("2,0").character());
        assertEquals('O', cells.get("3,0").character());
        assertEquals('K', cells.get("4,0").character());
        assertEquals(' ', cells.get("5,0").character());
    }

    @Test
    void withTitle_remainingTopBorder_hLine() {
        DialogNode d = Dialog.of("A", Text.of("hi"));
        Map<String, CellState> cells = lay(d, 0, 0, 10, 5);

        // "─ A " = 4 chars occupying cols 1-4, rest (5-8) should be H_LINE
        for (int col = 5; col <= 8; col++) {
            assertEquals('─', cells.get(col + ",0").character(),
                    "Expected H_LINE at col " + col + " after title");
        }
    }

    @Test
    void withoutTitle_topBorder_allHLine() {
        DialogNode d = Dialog.of(Text.of("hi"));
        Map<String, CellState> cells = lay(d, 0, 0, 6, 5);
        for (int col = 1; col <= 4; col++) {
            assertEquals('─', cells.get(col + ",0").character());
        }
    }

    @Test
    void content_appearsInsideBorder() {
        TextNode content = Text.of("AB");
        DialogNode d = Dialog.of(content);
        Map<String, CellState> cells = lay(d, 0, 0, 10, 5);

        assertEquals('A', cells.get("1,1").character());
        assertEquals('B', cells.get("2,1").character());
    }

    @Test
    void offsetDialog_correctPositions() {
        TextNode content = Text.of("X");
        DialogNode d = Dialog.of(content);
        Map<String, CellState> cells = lay(d, 3, 2, 10, 5);

        assertEquals('╭', cells.get("3,2").character());
        assertEquals('X', cells.get("4,3").character());
    }

    @Test
    void borderStyle_appliedToCorners() {
        Style bold = Style.DEFAULT.withBold(true);
        DialogNode d = Dialog.of("T", Text.of("hi"), bold);
        Map<String, CellState> cells = lay(d, 0, 0, 10, 5);

        assertTrue(cells.get("0,0").style().isBold());
        assertTrue(cells.get("9,0").style().isBold());
    }
}
