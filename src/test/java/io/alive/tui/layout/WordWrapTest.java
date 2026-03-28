package io.alive.tui.layout;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LayoutEngine#wordWrap(String, int)}.
 *
 * @author Jarvis (AI)
 */
class WordWrapTest {

    @Test
    void shortText_returnsOneLine() {
        List<String> lines = LayoutEngine.wordWrap("hello", 10);
        assertEquals(1, lines.size());
        assertEquals("hello", lines.get(0));
    }

    @Test
    void textFitsExactly_oneLine() {
        List<String> lines = LayoutEngine.wordWrap("hello", 5);
        assertEquals(1, lines.size());
        assertEquals("hello", lines.get(0));
    }

    @Test
    void twoWords_wrapsAtBoundary() {
        // "hello world" at width 7 — "hello" + "world"
        List<String> lines = LayoutEngine.wordWrap("hello world", 7);
        assertEquals(2, lines.size());
        assertEquals("hello", lines.get(0));
        assertEquals("world", lines.get(1));
    }

    @Test
    void twoWords_fitOnOneLine() {
        List<String> lines = LayoutEngine.wordWrap("hi ya", 10);
        assertEquals(1, lines.size());
        assertEquals("hi ya", lines.get(0));
    }

    @Test
    void explicitNewline_forcesBreak() {
        List<String> lines = LayoutEngine.wordWrap("line1\nline2", 20);
        assertEquals(2, lines.size());
        assertEquals("line1", lines.get(0));
        assertEquals("line2", lines.get(1));
    }

    @Test
    void multipleNewlines_emptyLines() {
        List<String> lines = LayoutEngine.wordWrap("a\n\nb", 20);
        assertEquals(3, lines.size());
        assertEquals("a",  lines.get(0));
        assertEquals("",   lines.get(1));
        assertEquals("b",  lines.get(2));
    }

    @Test
    void longWord_hardBreak() {
        // "abcdefgh" at width 3 => "abc" + "def" + "gh"
        List<String> lines = LayoutEngine.wordWrap("abcdefgh", 3);
        assertEquals(3, lines.size());
        assertEquals("abc", lines.get(0));
        assertEquals("def", lines.get(1));
        assertEquals("gh",  lines.get(2));
    }

    @Test
    void widthZero_returnsEmpty() {
        List<String> lines = LayoutEngine.wordWrap("hello", 0);
        assertTrue(lines.isEmpty());
    }

    @Test
    void singleCharWidth_eachCharOnOwnLine() {
        List<String> lines = LayoutEngine.wordWrap("abc", 1);
        assertEquals(3, lines.size());
        assertEquals("a", lines.get(0));
        assertEquals("b", lines.get(1));
        assertEquals("c", lines.get(2));
    }

    @Test
    void multilineWithWrap_bothNewlineAndWordWrap() {
        // "hello world\nfoo" at width 7
        // paragraph 1: "hello" + "world"
        // paragraph 2: "foo"
        List<String> lines = LayoutEngine.wordWrap("hello world\nfoo", 7);
        assertEquals(3, lines.size());
        assertEquals("hello", lines.get(0));
        assertEquals("world", lines.get(1));
        assertEquals("foo",   lines.get(2));
    }

    @Test
    void emptyString_returnsSingleEmptyLine() {
        List<String> lines = LayoutEngine.wordWrap("", 10);
        assertEquals(1, lines.size());
        assertEquals("", lines.get(0));
    }

    @Test
    void singleWordFitsPrecisely_oneLine() {
        List<String> lines = LayoutEngine.wordWrap("abc", 3);
        assertEquals(1, lines.size());
        assertEquals("abc", lines.get(0));
    }
}
