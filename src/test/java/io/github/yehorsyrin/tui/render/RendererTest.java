package io.github.yehorsyrin.tui.render;

import io.github.yehorsyrin.tui.backend.TerminalBackend;
import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.node.Input;
import io.github.yehorsyrin.tui.node.InputNode;
import io.github.yehorsyrin.tui.node.Text;
import io.github.yehorsyrin.tui.node.TextNode;
import io.github.yehorsyrin.tui.node.VBox;
import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RendererTest {

    /** In-memory TerminalBackend — no real terminal needed. */
    static class FakeBackend implements TerminalBackend {
        final List<String> putChars = new ArrayList<>(); // "col,row=char"
        int flushCount = 0;
        boolean cursorHidden = false;
        int cursorCol = -1, cursorRow = -1;
        int width = 80, height = 24;
        Runnable resizeListener;

        @Override public void init() {}
        @Override public void shutdown() {}
        @Override public int getWidth()  { return width; }
        @Override public int getHeight() { return height; }

        @Override
        public void putChar(int col, int row, char c, Style style) {
            putChars.add(col + "," + row + "=" + c);
        }

        @Override public void flush()       { flushCount++; }
        @Override public void hideCursor()  { cursorHidden = true; }
        @Override public void showCursor()  { cursorHidden = false; }
        @Override public void setCursor(int col, int row) { cursorCol = col; cursorRow = row; }
        @Override public void clear() { putChars.clear(); }
        @Override public KeyEvent readKey() { return KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.EOF); }
        @Override public void setResizeListener(Runnable r) { this.resizeListener = r; }
    }

    private FakeBackend backend;
    private Renderer renderer;

    @BeforeEach
    void setUp() {
        backend  = new FakeBackend();
        renderer = new Renderer(backend);
    }

    @Test
    void firstRender_allCharsWritten() {
        TextNode text = Text.of("Hi");
        renderer.render(text);
        assertTrue(backend.putChars.contains("0,0=H"));
        assertTrue(backend.putChars.contains("1,0=i"));
    }

    @Test
    void firstRender_flushCalled() {
        renderer.render(Text.of("Hi"));
        assertEquals(1, backend.flushCount);
    }

    @Test
    void identicalRender_noFlush() {
        renderer.render(Text.of("Hi"));
        int before = backend.flushCount;
        renderer.render(Text.of("Hi"));
        assertEquals(before, backend.flushCount); // no change → no flush
    }

    @Test
    void changedChar_onlyDiffWritten() {
        renderer.render(Text.of("Hello"));
        backend.putChars.clear();
        renderer.render(Text.of("Hellp"));
        assertEquals(1, backend.putChars.size());
        assertEquals("4,0=p", backend.putChars.get(0));
    }

    @Test
    void forceFullRender_redrawsEverything() {
        renderer.render(Text.of("Hi"));
        backend.putChars.clear();
        backend.flushCount = 0;
        renderer.forceFullRender(Text.of("Hi"));
        assertEquals(2, backend.putChars.size()); // H and i redrawn
        assertEquals(1, backend.flushCount);
    }

    @Test
    void onResize_redrawsLastTree() {
        renderer.render(Text.of("Hi"));
        backend.putChars.clear();
        backend.flushCount = 0;
        renderer.onResize();
        assertEquals(2, backend.putChars.size());
    }

    @Test
    void nullTree_erasesOldCells() {
        renderer.render(Text.of("Hi"));
        backend.putChars.clear();
        renderer.render(null);
        // "Hi" was 2 cells — both should be erased with spaces
        assertEquals(2, backend.putChars.size());
        assertTrue(backend.putChars.stream().allMatch(s -> s.endsWith("= ")));
    }

    @Test
    void nullBackend_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new Renderer(null));
    }

    @Test
    void previousTreeTracked() {
        assertNull(renderer.getPreviousTree());
        TextNode t = Text.of("A");
        renderer.render(t);
        assertSame(t, renderer.getPreviousTree());
    }

    // --- Cursor positioning (TASK-13) ---

    @Test
    void focusedInputNode_cursorPositionedAtCursor() {
        InputNode input = Input.of("abc", null);
        input.setFocused(true);
        input.setCursorPos(2);   // cursor after 'b'

        renderer.render(input);

        // x=0 (node at col 0), cursorPos=2 → cursor at col 2, row 0
        assertEquals(2, backend.cursorCol);
        assertEquals(0, backend.cursorRow);
    }

    @Test
    void focusedInputNode_atNonZeroPosition_cursorOffset() {
        // Place input inside a VBox so it gets a non-zero y
        InputNode input = Input.of("hi", null);
        input.setFocused(true);
        input.setCursorPos(1);

        var tree = VBox.of(Text.of("header"), input);
        renderer.render(tree);

        // input appears on row 1 (below "header"), cursorPos=1 → col=0+1=1
        assertEquals(1, backend.cursorCol);
        assertEquals(1, backend.cursorRow);
    }

    @Test
    void noFocusedInput_cursorNotMoved() {
        InputNode input = Input.of("abc", null);
        input.setFocused(false);  // not focused

        renderer.render(input);

        // setCursor never called — stays at -1
        assertEquals(-1, backend.cursorCol);
        assertEquals(-1, backend.cursorRow);
    }

    @Test
    void focusedInputInNestedTree_cursorFound() {
        InputNode input = Input.of("x", null);
        input.setFocused(true);
        input.setCursorPos(0);

        var tree = VBox.of(Text.of("row0"), VBox.of(Text.of("row1"), input));
        renderer.render(tree);

        assertEquals(2, backend.cursorRow);  // input is on row 2
        assertEquals(0, backend.cursorCol);
    }

    // --- Overlay (TASK-18) ---

    @Test
    void pushOverlay_cellsAppearedOnTopOfBase() {
        // Base: 'A' at (0,0). Overlay: 'X' at (0,0) — should win.
        renderer.render(Text.of("A"));
        backend.putChars.clear();

        renderer.pushOverlay(Text.of("X"));
        renderer.render(Text.of("A"));  // re-render with overlay

        // Overlay 'X' overwrites 'A' at position 0,0
        assertTrue(backend.putChars.contains("0,0=X"),
            "Overlay cell should be rendered: " + backend.putChars);
    }

    @Test
    void pushOverlay_baseAndOverlayBothRendered() {
        // Base row 1: "B" (row 0 is a space), Overlay row 0: "O" — non-overlapping rows
        renderer.pushOverlay(Text.of("O"));
        var base = VBox.of(Text.of(" "), Text.of("B"));
        renderer.render(base);

        // Overlay 'O' at row 0, base 'B' at row 1 — both should appear
        assertTrue(backend.putChars.contains("0,0=O"), "Overlay cell missing: " + backend.putChars);
        assertTrue(backend.putChars.contains("0,1=B"), "Base cell missing: " + backend.putChars);
    }

    @Test
    void clearOverlay_restoresBaseOnly() {
        // Render with overlay, then clear and re-render
        renderer.pushOverlay(Text.of("X"));
        renderer.render(Text.of("A"));

        backend.putChars.clear();
        backend.flushCount = 0;

        renderer.clearOverlay();
        renderer.render(Text.of("A"));

        // 'X' was replaced by 'A' again
        assertTrue(backend.putChars.contains("0,0=A"), "Base cell should reappear after clear");
        // Ensure 'X' no longer rendered as a change (it was overwritten by 'A' which is a change)
    }

    @Test
    void getOverlay_returnsCurrentOverlay() {
        assertNull(renderer.getOverlay());
        TextNode overlay = Text.of("popup");
        renderer.pushOverlay(overlay);
        assertSame(overlay, renderer.getOverlay());
        renderer.clearOverlay();
        assertNull(renderer.getOverlay());
    }

    @Test
    void forceFullRender_withOverlay_redrawsBoth() {
        // Base 'B' at row 1, Overlay 'O' at row 0 — non-overlapping
        renderer.pushOverlay(Text.of("O"));
        var base = VBox.of(Text.of(" "), Text.of("B"));
        renderer.render(base);
        backend.putChars.clear();
        backend.flushCount = 0;

        renderer.forceFullRender(base);

        // Both base and overlay cells are redrawn
        assertTrue(backend.putChars.stream().anyMatch(s -> s.endsWith("=B")),
            "Base 'B' should be redrawn: " + backend.putChars);
        assertTrue(backend.putChars.stream().anyMatch(s -> s.endsWith("=O")),
            "Overlay 'O' should be redrawn: " + backend.putChars);
        assertEquals(1, backend.flushCount);
    }

    @Test
    void overlay_noExceptionOnEmptyBase() {
        renderer.pushOverlay(Text.of("popup"));
        assertDoesNotThrow(() -> renderer.render(null));
    }
}
