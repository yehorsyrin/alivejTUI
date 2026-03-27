package io.alive.tui.render;

import io.alive.tui.backend.TerminalBackend;
import io.alive.tui.event.KeyEvent;
import io.alive.tui.node.Text;
import io.alive.tui.node.TextNode;
import io.alive.tui.style.Style;
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
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() { putChars.clear(); }
        @Override public KeyEvent readKey() { return KeyEvent.of(io.alive.tui.event.KeyType.EOF); }
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
}
