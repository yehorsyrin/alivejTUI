package io.alive.tui.node;

import io.alive.tui.layout.LayoutEngine;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for multi-line text and word-wrap support in {@link TextNode} via the public API.
 *
 * @author Jarvis (AI)
 */
class TextNodeWordWrapTest {

    private final LayoutEngine engine = new LayoutEngine();

    // --- LayoutEngine integration ---

    @Test
    void layout_wrapEnabled_heightReflectsLines() {
        TextNode t = Text.of("hello world").wrap();
        engine.layout(t, 0, 0, 7, 24);
        // "hello" + "world" = 2 lines
        assertEquals(2, t.getHeight());
        assertEquals(7, t.getWidth());
        assertNotNull(t.getWrappedLines());
        assertEquals(2, t.getWrappedLines().size());
    }

    @Test
    void layout_noWrap_singleLine() {
        TextNode t = Text.of("hello world");
        engine.layout(t, 0, 0, 7, 24);
        assertEquals(1, t.getHeight());
        assertNull(t.getWrappedLines());
    }

    @Test
    void layout_textWithNewline_wrapsAutomatically() {
        TextNode t = Text.of("line1\nline2");
        engine.layout(t, 0, 0, 80, 24);
        assertNotNull(t.getWrappedLines());
        assertEquals(2, t.getWrappedLines().size());
        assertEquals("line1", t.getWrappedLines().get(0));
        assertEquals("line2", t.getWrappedLines().get(1));
    }

    @Test
    void layout_wrapDisabled_noWrappedLines() {
        TextNode t = Text.of("a b c").wrap(false);
        engine.layout(t, 0, 0, 3, 24);
        assertNull(t.getWrappedLines());
        assertEquals(1, t.getHeight());
    }

    @Test
    void layout_wrappedNode_xYPreserved() {
        TextNode t = Text.of("hello world").wrap();
        engine.layout(t, 5, 3, 7, 24);
        assertEquals(5, t.getX());
        assertEquals(3, t.getY());
    }

    @Test
    void layout_wrapEnabled_contentWrappedToLines() {
        TextNode t = Text.of("hello world").wrap();
        engine.layout(t, 0, 0, 7, 24);
        assertEquals("hello", t.getWrappedLines().get(0));
        assertEquals("world", t.getWrappedLines().get(1));
    }

    @Test
    void layout_wrapEnabled_widthAlwaysAvailableWidth() {
        TextNode t = Text.of("hi").wrap();
        engine.layout(t, 0, 0, 20, 24);
        assertEquals(20, t.getWidth());
    }
}
