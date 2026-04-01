package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MarkdownParser} and markdown rendering via {@link Text#ofMarkdown}.
 *
 * @author Jarvis (AI)
 */
class MarkdownParserTest {

    // --- MarkdownParser.parse ---

    @Test
    void plainText_returnsOneSegment() {
        List<StyledSegment> segs = MarkdownParser.parse("hello");
        assertEquals(1, segs.size());
        assertEquals("hello", segs.get(0).text());
        assertEquals(Style.DEFAULT, segs.get(0).style());
    }

    @Test
    void boldSyntax_returnsBoldSegment() {
        List<StyledSegment> segs = MarkdownParser.parse("**bold**");
        assertEquals(1, segs.size());
        assertEquals("bold", segs.get(0).text());
        assertTrue(segs.get(0).style().isBold());
    }

    @Test
    void italicSyntax_returnsItalicSegment() {
        List<StyledSegment> segs = MarkdownParser.parse("*italic*");
        assertEquals(1, segs.size());
        assertEquals("italic", segs.get(0).text());
        assertTrue(segs.get(0).style().isItalic());
    }

    @Test
    void codeSyntax_returnsBoldUnderlineSegment() {
        List<StyledSegment> segs = MarkdownParser.parse("`code`");
        assertEquals(1, segs.size());
        assertEquals("code", segs.get(0).text());
        assertTrue(segs.get(0).style().isBold());
        assertTrue(segs.get(0).style().isUnderline());
    }

    @Test
    void strikethroughSyntax_returnsStrikethroughSegment() {
        List<StyledSegment> segs = MarkdownParser.parse("~~del~~");
        assertEquals(1, segs.size());
        assertEquals("del", segs.get(0).text());
        assertTrue(segs.get(0).style().isStrikethrough());
    }

    @Test
    void mixedMarkup_multipleSegments() {
        List<StyledSegment> segs = MarkdownParser.parse("**bold** and *italic*");
        assertEquals(3, segs.size());
        assertEquals("bold",    segs.get(0).text());
        assertTrue(segs.get(0).style().isBold());
        assertEquals(" and ",   segs.get(1).text());
        assertEquals(Style.DEFAULT, segs.get(1).style());
        assertEquals("italic",  segs.get(2).text());
        assertTrue(segs.get(2).style().isItalic());
    }

    @Test
    void unclosedMarker_treatedAsPlain() {
        List<StyledSegment> segs = MarkdownParser.parse("**unclosed");
        assertEquals(1, segs.size());
        assertEquals("**unclosed", segs.get(0).text());
        assertEquals(Style.DEFAULT, segs.get(0).style());
    }

    @Test
    void emptyString_returnsEmptyList() {
        assertTrue(MarkdownParser.parse("").isEmpty());
    }

    @Test
    void nullInput_returnsEmptyList() {
        assertTrue(MarkdownParser.parse(null).isEmpty());
    }

    @Test
    void baseStyleMerged_boldAddsToBase() {
        Style base = Style.DEFAULT.withItalic(true);
        List<StyledSegment> segs = MarkdownParser.parse("**b**", base);
        assertEquals(1, segs.size());
        assertTrue(segs.get(0).style().isBold());
        assertTrue(segs.get(0).style().isItalic()); // inherited from base
    }

    // --- Text.ofMarkdown ---

    @Test
    void textOfMarkdown_createsMarkdownNode() {
        TextNode node = Text.ofMarkdown("**hi**");
        assertTrue(node.hasMarkdown());
        assertNotNull(node.getSegments());
        assertEquals(1, node.getSegments().size());
    }

    @Test
    void textOfMarkdown_getText_returnsCombinedPlainText() {
        TextNode node = Text.ofMarkdown("**bold** and *italic*");
        assertEquals("bold and italic", node.getText());
    }

    @Test
    void textOf_plainNode_noMarkdown() {
        TextNode node = Text.of("plain");
        assertFalse(node.hasMarkdown());
        assertNull(node.getSegments());
    }

    // --- Rendering via FakeBackend ---

    static class FakeBackend implements io.github.yehorsyrin.tui.backend.TerminalBackend {
        final java.util.List<String> cells = new java.util.ArrayList<>();
        @Override public void init() {}
        @Override public void shutdown() {}
        @Override public int getWidth()  { return 80; }
        @Override public int getHeight() { return 24; }
        @Override public void putChar(int col, int row, char c, Style s) {
            cells.add(col + "," + row + "=" + c);
        }
        @Override public void flush() {}
        @Override public void hideCursor() {}
        @Override public void showCursor() {}
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() { cells.clear(); }
        @Override public io.github.yehorsyrin.tui.event.KeyEvent readKey() {
            return io.github.yehorsyrin.tui.event.KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.EOF);
        }
        @Override public void setResizeListener(Runnable r) {}
    }

    @Test
    void render_markdownNode_correctCharsAtPositions() {
        TextNode node = Text.ofMarkdown("**AB**");
        FakeBackend backend = new FakeBackend();
        new io.github.yehorsyrin.tui.render.Renderer(backend).render(node);

        // "AB" at col 0,1 row 0
        assertTrue(backend.cells.contains("0,0=A"), "Expected A at 0,0: " + backend.cells);
        assertTrue(backend.cells.contains("1,0=B"), "Expected B at 1,0: " + backend.cells);
    }

    @Test
    void render_mixedMarkdown_correctSegmentRendering() {
        // "Hi " plain, then "!" bold
        TextNode node = Text.ofMarkdown("Hi **!**");
        FakeBackend backend = new FakeBackend();
        new io.github.yehorsyrin.tui.render.Renderer(backend).render(node);

        assertTrue(backend.cells.contains("0,0=H"));
        assertTrue(backend.cells.contains("1,0=i"));
        assertTrue(backend.cells.contains("2,0= "));
        assertTrue(backend.cells.contains("3,0=!"));
    }
}
