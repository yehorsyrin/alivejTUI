package io.github.yehorsyrin.tui;

import io.github.yehorsyrin.tui.backend.TerminalBackend;
import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.diff.CellChange;
import io.github.yehorsyrin.tui.diff.Differ;
import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.layout.LayoutEngine;
import io.github.yehorsyrin.tui.node.Text;
import io.github.yehorsyrin.tui.node.VBox;
import io.github.yehorsyrin.tui.render.Renderer;
import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end pipeline tests: layout → diff → render.
 *
 * @author Jarvis (AI)
 */
class IntegrationPipelineTest {

    static class FakeBackend implements TerminalBackend {
        final List<String> putChars = new ArrayList<>();
        int flushCount = 0;

        @Override public void init() {}
        @Override public void shutdown() {}
        @Override public int getWidth()  { return 80; }
        @Override public int getHeight() { return 24; }
        @Override public void putChar(int col, int row, char c, Style style) { putChars.add(col + "," + row + "=" + c); }
        @Override public void flush() { flushCount++; }
        @Override public void hideCursor() {}
        @Override public void showCursor() {}
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() { putChars.clear(); }
        @Override public KeyEvent readKey() throws InterruptedException { return KeyEvent.of(KeyType.EOF); }
        @Override public void setResizeListener(Runnable r) {}
    }

    @Test
    void simpleText_fullPipeline() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        renderer.render(Text.of("Hi"));
        assertTrue(backend.putChars.contains("0,0=H"));
        assertTrue(backend.putChars.contains("1,0=i"));
        assertEquals(1, backend.flushCount);
    }

    @Test
    void vboxWithTwoRows_fullPipeline() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        renderer.render(VBox.of(Text.of("A"), Text.of("B")));
        assertTrue(backend.putChars.contains("0,0=A"));
        assertTrue(backend.putChars.contains("0,1=B"));
    }

    @Test
    void incrementalUpdate_onlyDiffFlushed() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        renderer.render(Text.of("ABC"));
        backend.putChars.clear();
        backend.flushCount = 0;
        renderer.render(Text.of("AXC"));  // only B→X changed
        assertEquals(1, backend.putChars.size());
        assertTrue(backend.putChars.contains("1,0=X"));
        assertEquals(1, backend.flushCount);
    }

    @Test
    void differ_noDiff_noFlush() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        renderer.render(Text.of("Same"));
        int before = backend.flushCount;
        renderer.render(Text.of("Same"));
        assertEquals(before, backend.flushCount);
    }

    @Test
    void layoutEngine_positionsCorrectly() {
        Node tree = VBox.of(Text.of("Row0"), Text.of("Row1"), Text.of("Row2"));
        LayoutEngine le = new LayoutEngine();
        le.layout(tree, 0, 0, 80, 24);
        // Row1 should be at y=1, Row2 at y=2
        Node row1 = tree.getChildren().get(1);
        Node row2 = tree.getChildren().get(2);
        assertEquals(1, row1.getY());
        assertEquals(2, row2.getY());
    }
}
