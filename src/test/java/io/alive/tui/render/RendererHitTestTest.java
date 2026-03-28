package io.alive.tui.render;

import io.alive.tui.backend.TerminalBackend;
import io.alive.tui.core.Node;
import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.node.TextNode;
import io.alive.tui.node.VBoxNode;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Renderer#hitTest(int, int)}.
 *
 * <p>After {@code render()}, the layout engine computes positions. All assertions
 * use positions derived from the actual layout output.
 *
 * @author Jarvis (AI)
 */
class RendererHitTestTest {

    static class FakeBackend implements TerminalBackend {
        @Override public void init() {}
        @Override public void shutdown() {}
        @Override public int getWidth()  { return 80; }
        @Override public int getHeight() { return 24; }
        @Override public void putChar(int col, int row, char c, Style s) {}
        @Override public void flush() {}
        @Override public void hideCursor() {}
        @Override public void showCursor() {}
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() {}
        @Override public KeyEvent readKey() { return KeyEvent.of(KeyType.EOF); }
        @Override public void setResizeListener(Runnable r) {}
    }

    private Renderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new Renderer(new FakeBackend());
    }

    @Test
    void hitTest_noRender_returnsNull() {
        assertNull(renderer.hitTest(5, 5));
    }

    @Test
    void hitTest_rootNode_returnsRoot() {
        // Single TextNode "x": layout sets x=0, y=0, w=1, h=1
        TextNode node = new TextNode("x", Style.DEFAULT);
        renderer.render(node);
        // (0,0) is inside the node
        assertSame(node, renderer.hitTest(0, 0));
    }

    @Test
    void hitTest_outsideRootNode_returnsNull() {
        // TextNode "x" → w=1, h=1 at (0,0). Col 1 is outside.
        TextNode node = new TextNode("x", Style.DEFAULT);
        renderer.render(node);
        assertNull(renderer.hitTest(1, 0));
    }

    @Test
    void hitTest_vBox_hitsCorrectChild() {
        // VBox with two text nodes stacked:
        // child0: y=0, child1: y=1 (each h=1, w=80)
        TextNode child0 = new TextNode("AAAA", Style.DEFAULT);
        TextNode child1 = new TextNode("BBBB", Style.DEFAULT);
        VBoxNode vbox = new VBoxNode(0);
        vbox.addChild(child0);
        vbox.addChild(child1);

        renderer.render(vbox);

        // Row 0 → child0
        assertSame(child0, renderer.hitTest(0, 0));
        // Row 1 → child1
        assertSame(child1, renderer.hitTest(0, 1));
    }

    @Test
    void hitTest_vBox_outsideAll_returnsNull() {
        TextNode child = new TextNode("X", Style.DEFAULT);
        VBoxNode vbox  = new VBoxNode(0);
        vbox.addChild(child);

        renderer.render(vbox);

        // VBox h=1, so row 2 is outside
        assertNull(renderer.hitTest(0, 2));
    }

    @Test
    void hitTest_deepestChildReturned() {
        // VBox → child VBox → leaf text
        // Outer vbox: row 0..1, inner vbox: row 0..0, leaf: row 0
        TextNode leaf  = new TextNode("L", Style.DEFAULT);
        VBoxNode inner = new VBoxNode(0);
        inner.addChild(leaf);
        VBoxNode outer = new VBoxNode(0);
        outer.addChild(inner);

        renderer.render(outer);

        // (0,0) is inside outer, inner, and leaf — leaf is deepest
        assertSame(leaf, renderer.hitTest(0, 0));
    }

    @Test
    void hitTest_overlayCheckedBeforeBase() {
        TextNode base    = new TextNode("BASE", Style.DEFAULT);
        TextNode overlay = new TextNode("OVER", Style.DEFAULT);

        renderer.render(base);
        renderer.pushOverlay(overlay);
        renderer.render(base);  // second render lays out the overlay
        // overlay: (0,0,4,1); base: (0,0,4,1) — overlay wins because checked first
        Node hit = renderer.hitTest(0, 0);
        assertSame(overlay, hit);
    }

    @Test
    void hitTest_overlayCleared_fallsBackToBase() {
        TextNode base    = new TextNode("BASE", Style.DEFAULT);
        TextNode overlay = new TextNode("OVER", Style.DEFAULT);

        renderer.render(base);
        renderer.pushOverlay(overlay);
        renderer.render(base);   // re-render to flush overlay into previousOverlayTree
        renderer.clearOverlay();

        assertSame(base, renderer.hitTest(0, 0));
    }
}
