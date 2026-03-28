package io.alive.tui.node;

import io.alive.tui.layout.LayoutEngine;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DialogNode} and {@link Dialog} factory (public API).
 *
 * @author Jarvis (AI)
 */
class DialogNodeTest {

    private final LayoutEngine engine = new LayoutEngine();

    // --- Factory ---

    @Test
    void dialog_of_noTitle_emptyTitle() {
        DialogNode d = Dialog.of(Text.of("hi"));
        assertEquals("", d.getTitle());
        assertFalse(d.hasTitle());
    }

    @Test
    void dialog_of_withTitle_titleSet() {
        DialogNode d = Dialog.of("Alert", Text.of("hi"));
        assertEquals("Alert", d.getTitle());
        assertTrue(d.hasTitle());
    }

    @Test
    void dialog_of_withBorderStyle() {
        Style red = Style.DEFAULT.withBold(true);
        DialogNode d = Dialog.of("T", Text.of("hi"), red);
        assertEquals(red, d.getBorderStyle());
    }

    @Test
    void dialog_borderStyle_chainable() {
        DialogNode d = Dialog.of(Text.of("x"));
        Style bold = Style.DEFAULT.withBold(true);
        assertSame(d, d.borderStyle(bold));
        assertEquals(bold, d.getBorderStyle());
    }

    @Test
    void dialog_defaultBorderStyle_isDefault() {
        DialogNode d = Dialog.of(Text.of("x"));
        assertEquals(Style.DEFAULT, d.getBorderStyle());
    }

    @Test
    void dialog_nullContent_noChildren() {
        DialogNode d = new DialogNode(null, null, null);
        assertTrue(d.getChildren().isEmpty());
    }

    @Test
    void dialog_withContent_oneChild() {
        TextNode content = Text.of("hi");
        DialogNode d = Dialog.of(content);
        assertEquals(1, d.getChildren().size());
        assertSame(content, d.getChildren().get(0));
    }

    // --- Layout ---

    @Test
    void layout_withContent_heightIsContentPlusBorder() {
        DialogNode d = Dialog.of("T", Text.of("hi"));
        engine.layout(d, 0, 0, 20, 24);
        // content height = 1 (single text line), + 2 border = 3
        assertEquals(3, d.getHeight());
        assertEquals(20, d.getWidth());
    }

    @Test
    void layout_empty_usesAvailableSize() {
        DialogNode d = new DialogNode(null, null, null);
        engine.layout(d, 0, 0, 10, 5);
        assertEquals(10, d.getWidth());
        assertEquals(5, d.getHeight());
    }

    @Test
    void layout_contentPositionedInsideBorder() {
        TextNode content = Text.of("hi");
        DialogNode d = Dialog.of("T", content);
        engine.layout(d, 2, 3, 20, 24);
        // content should be at (3, 4) = parent (2,3) + (1,1) border offset
        assertEquals(3, content.getX());
        assertEquals(4, content.getY());
    }

    @Test
    void layout_xyPreserved() {
        DialogNode d = Dialog.of(Text.of("test"));
        engine.layout(d, 5, 7, 20, 10);
        assertEquals(5, d.getX());
        assertEquals(7, d.getY());
    }
}
