package io.alive.tui.diff;

import io.alive.tui.layout.LayoutEngine;
import io.alive.tui.node.*;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TreeFlattener} rendering of {@link ViewportNode}.
 *
 * @author Jarvis (AI)
 */
class ViewportNodeFlattenTest {

    private final LayoutEngine engine    = new LayoutEngine();
    private final TreeFlattener flattener = new TreeFlattener();

    private Map<String, CellState> lay(ViewportNode vp, int x, int y, int w, int h) {
        engine.layout(vp, x, y, w, h);
        return flattener.flatten(vp);
    }

    /** Builds a VBox with n single-char lines "0", "1", ..., "n-1". */
    private VBoxNode numberedLines(int n) {
        TextNode[] nodes = new TextNode[n];
        for (int i = 0; i < n; i++) nodes[i] = Text.of(String.valueOf(i % 10));
        return VBox.of(nodes);
    }

    @Test
    void scrollOffset0_showsFirstRows() {
        VBoxNode content = numberedLines(10);
        ViewportNode vp = Viewport.plain(content, 3);
        Map<String, CellState> cells = lay(vp, 0, 0, 5, 24);

        // Only rows 0-2 visible
        assertEquals('0', cells.get("0,0").character());
        assertEquals('1', cells.get("0,1").character());
        assertEquals('2', cells.get("0,2").character());
        assertNull(cells.get("0,3"));
    }

    @Test
    void scrollOffset2_showsRows2to4() {
        VBoxNode content = numberedLines(10);
        ViewportNode vp = Viewport.plain(content, 3);
        engine.layout(vp, 0, 0, 5, 24);
        vp.setScrollOffset(2);
        Map<String, CellState> cells = flattener.flatten(vp);

        assertEquals('2', cells.get("0,0").character());
        assertEquals('3', cells.get("0,1").character());
        assertEquals('4', cells.get("0,2").character());
        assertNull(cells.get("0,3"));
    }

    @Test
    void viewport_clipsToMaxHeight() {
        VBoxNode content = numberedLines(8);
        ViewportNode vp = Viewport.plain(content, 4);
        Map<String, CellState> cells = lay(vp, 0, 0, 5, 24);

        // Only 4 rows visible
        assertNotNull(cells.get("0,3"));
        assertNull(cells.get("0,4"));
    }

    @Test
    void viewport_withOffset_remapsToScreenCoords() {
        ViewportNode vp = Viewport.plain(numberedLines(10), 3);
        engine.layout(vp, 0, 5, 5, 24);  // y offset = 5
        vp.setScrollOffset(0);
        Map<String, CellState> cells = flattener.flatten(vp);

        // Row 0 of content → screen row 5
        assertEquals('0', cells.get("0,5").character());
        assertEquals('1', cells.get("0,6").character());
    }

    @Test
    void scrollbar_shownWhenScrollable() {
        ViewportNode vp = Viewport.of(numberedLines(10), 3);
        Map<String, CellState> cells = lay(vp, 0, 0, 6, 24);

        // Scroll bar at col 5 (width-1), track char or thumb char
        CellState sb = cells.get("5,0");
        assertNotNull(sb, "Scrollbar cell should exist");
        char ch = sb.character();
        assertTrue(ch == ViewportNode.THUMB_CHAR || ch == ViewportNode.TRACK_CHAR,
                "Expected scrollbar char but got: " + ch);
    }

    @Test
    void scrollbar_notShownWhenContentFits() {
        // Content (2 rows) fits within maxHeight (5), no scrollbar needed
        VBoxNode content = VBox.of(Text.of("A"), Text.of("B"));
        ViewportNode vp = Viewport.of(content, 5);
        Map<String, CellState> cells = lay(vp, 0, 0, 6, 24);

        // Col 5 should not have scrollbar chars (content only has 2 rows)
        CellState sb = cells.get("5,0");
        // If present, it's content; if absent, fine too — either way NOT a track/thumb
        if (sb != null) {
            assertFalse(sb.character() == ViewportNode.TRACK_CHAR
                    || sb.character() == ViewportNode.THUMB_CHAR,
                    "Scrollbar should not render when content fits");
        }
    }

    @Test
    void scrollbar_thumbAtTopWhenAtOffset0() {
        ViewportNode vp = Viewport.of(numberedLines(10), 3);
        Map<String, CellState> cells = lay(vp, 0, 0, 6, 24);

        // At offset 0, thumb starts at top
        assertEquals(ViewportNode.THUMB_CHAR, cells.get("5,0").character());
    }

    @Test
    void scrollbar_thumbAtBottomWhenScrolledToEnd() {
        ViewportNode vp = Viewport.of(numberedLines(10), 3);
        engine.layout(vp, 0, 0, 6, 24);
        vp.scrollToBottom();
        Map<String, CellState> cells = flattener.flatten(vp);

        // At max scroll, thumb is at the bottom row of the track
        int trackH = vp.getHeight(); // 3
        assertEquals(ViewportNode.THUMB_CHAR, cells.get("5," + (trackH - 1)).character());
    }

    @Test
    void noScrollbar_contentGetsFullWidth() {
        ViewportNode vp = Viewport.plain(numberedLines(10), 3);
        Map<String, CellState> cells = lay(vp, 0, 0, 6, 24);

        // Content uses col 5 too (no scrollbar reserved)
        // Row 0 of content is "0", it gets styled at cols 0..5 (padded with space)
        // But since numberedLines produces single-char text, col 0 has the digit
        // Col 5 might be absent (no padding) or might have a space — that's ok
        // Key point: no scroll char at col 5
        CellState col5 = cells.get("5,0");
        if (col5 != null) {
            char ch = col5.character();
            assertFalse(ch == ViewportNode.TRACK_CHAR || ch == ViewportNode.THUMB_CHAR);
        }
    }
}
