package io.alive.tui.node;

import io.alive.tui.layout.LayoutEngine;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Paragraph} factory.
 *
 * @author Jarvis (AI)
 */
class ParagraphTest {

    private final LayoutEngine engine = new LayoutEngine();

    @Test
    void of_createsWrappedTextNode() {
        TextNode node = Paragraph.of("hello");
        assertTrue(node.isWrap());
    }

    @Test
    void of_textPreserved() {
        TextNode node = Paragraph.of("hello world");
        assertEquals("hello world", node.getText());
    }

    @Test
    void of_withStyle_styleApplied() {
        Style bold = Style.DEFAULT.withBold(true);
        TextNode node = Paragraph.of("hi", bold);
        assertTrue(node.getStyle().isBold());
        assertTrue(node.isWrap());
    }

    @Test
    void of_layout_wrapsAtWidth() {
        TextNode node = Paragraph.of("hello world");
        engine.layout(node, 0, 0, 7, 24);
        assertNotNull(node.getWrappedLines());
        assertEquals(2, node.getWrappedLines().size());
        assertEquals("hello", node.getWrappedLines().get(0));
        assertEquals("world", node.getWrappedLines().get(1));
    }

    @Test
    void of_layout_heightEqualsLineCount() {
        TextNode node = Paragraph.of("a b c d");
        engine.layout(node, 0, 0, 4, 24);
        assertTrue(node.getHeight() > 1);
        assertEquals(node.getWrappedLines().size(), node.getHeight());
    }

    @Test
    void of_explicitNewline_wrapsAutomatically() {
        TextNode node = Paragraph.of("line1\nline2");
        engine.layout(node, 0, 0, 80, 24);
        assertNotNull(node.getWrappedLines());
        assertEquals(2, node.getWrappedLines().size());
        assertEquals("line1", node.getWrappedLines().get(0));
        assertEquals("line2", node.getWrappedLines().get(1));
    }

    @Test
    void ofMarkdown_createsMarkdownNode() {
        TextNode node = Paragraph.ofMarkdown("**bold**");
        assertTrue(node.hasMarkdown());
        assertTrue(node.isWrap());
    }

    @Test
    void ofMarkdown_combinedTextUsedForWrap() {
        TextNode node = Paragraph.ofMarkdown("**hello** world");
        assertEquals("hello world", node.getText());
    }

    @Test
    void ofMarkdown_withBaseStyle_styleInherited() {
        Style italic = Style.DEFAULT.withItalic(true);
        TextNode node = Paragraph.ofMarkdown("plain", italic);
        assertTrue(node.isWrap());
        assertNotNull(node.getSegments());
        // Plain text inherits base style (italic)
        assertTrue(node.getSegments().get(0).style().isItalic());
    }

    @Test
    void of_nullText_noException() {
        TextNode node = Paragraph.of(null);
        assertNotNull(node);
        assertTrue(node.isWrap());
        assertEquals("", node.getText());
    }
}
