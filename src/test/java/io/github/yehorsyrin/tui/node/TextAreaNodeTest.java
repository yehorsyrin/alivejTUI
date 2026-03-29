package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.layout.LayoutEngine;
import io.github.yehorsyrin.tui.render.Renderer;
import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TextAreaNode} and {@link TextArea} factory.
 *
 * @author Jarvis (AI)
 */
class TextAreaNodeTest {

    private final LayoutEngine engine = new LayoutEngine();

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    @Test
    void factory_of_maxHeight_emptyText() {
        TextAreaNode ta = TextArea.of(5);
        assertEquals("", ta.getText());
        assertEquals(1, ta.getLines().size());
        assertEquals(5, ta.getMaxHeight());
    }

    @Test
    void factory_of_initialText_maxHeight() {
        TextAreaNode ta = TextArea.of("hello", 3);
        assertEquals("hello", ta.getText());
        assertEquals(1, ta.getLines().size());
        assertEquals(3, ta.getMaxHeight());
    }

    @Test
    void factory_of_multilineInitialText() {
        TextAreaNode ta = TextArea.of("line1\nline2\nline3", 10);
        assertEquals(3, ta.getLines().size());
        assertEquals("line1", ta.getLines().get(0));
        assertEquals("line2", ta.getLines().get(1));
        assertEquals("line3", ta.getLines().get(2));
    }

    @Test
    void factory_of_withOnChange() {
        AtomicInteger count = new AtomicInteger();
        TextAreaNode ta = TextArea.of("hi", 5, count::incrementAndGet);
        ta.insertChar('!');
        assertEquals(1, count.get());
    }

    @Test
    void factory_of_nullInitialText_treatedAsEmpty() {
        TextAreaNode ta = TextArea.of(null, 5);
        assertEquals("", ta.getText());
        assertEquals(1, ta.getLines().size());
    }

    // -----------------------------------------------------------------------
    // getText / setText round-trip
    // -----------------------------------------------------------------------

    @Test
    void getText_roundTrip_singleLine() {
        TextAreaNode ta = TextArea.of("hello world", 5);
        assertEquals("hello world", ta.getText());
    }

    @Test
    void getText_roundTrip_multiLine() {
        TextAreaNode ta = TextArea.of("a\nb\nc", 5);
        assertEquals("a\nb\nc", ta.getText());
    }

    @Test
    void setText_replacesContent() {
        TextAreaNode ta = TextArea.of("old", 5);
        ta.setText("new\nlines");
        assertEquals("new\nlines", ta.getText());
        assertEquals(2, ta.getLines().size());
    }

    @Test
    void setText_resetsCursor() {
        TextAreaNode ta = TextArea.of("abc\ndef", 5);
        ta.moveDown();
        ta.end();
        ta.setText("x");
        assertEquals(0, ta.getCursorRow());
        assertEquals(0, ta.getCursorCol());
        assertEquals(0, ta.getScrollRow());
    }

    @Test
    void setText_null_setsEmpty() {
        TextAreaNode ta = TextArea.of("some text", 5);
        ta.setText(null);
        assertEquals("", ta.getText());
        assertEquals(1, ta.getLines().size());
    }

    @Test
    void setText_firesOnChange() {
        AtomicInteger count = new AtomicInteger();
        TextAreaNode ta = TextArea.of("hi", 5, count::incrementAndGet);
        ta.setText("bye");
        assertEquals(1, count.get());
    }

    // -----------------------------------------------------------------------
    // insertChar
    // -----------------------------------------------------------------------

    @Test
    void insertChar_appendsToEmptyLine() {
        TextAreaNode ta = TextArea.of("", 5);
        ta.insertChar('h');
        ta.insertChar('i');
        assertEquals("hi", ta.getText());
        assertEquals(2, ta.getCursorCol());
    }

    @Test
    void insertChar_insertsAtMiddle() {
        TextAreaNode ta = TextArea.of("ac", 5);
        ta.setCursorForTest(0, 1);
        ta.insertChar('b');
        assertEquals("abc", ta.getText());
        assertEquals(2, ta.getCursorCol());
    }

    @Test
    void insertChar_firesOnChange() {
        AtomicInteger count = new AtomicInteger();
        TextAreaNode ta = TextArea.of("", 5, count::incrementAndGet);
        ta.insertChar('x');
        assertEquals(1, count.get());
    }

    // -----------------------------------------------------------------------
    // deleteBackward
    // -----------------------------------------------------------------------

    @Test
    void deleteBackward_removesCharBeforeCursor() {
        TextAreaNode ta = TextArea.of("abc", 5);
        ta.end();
        ta.deleteBackward();
        assertEquals("ab", ta.getText());
        assertEquals(2, ta.getCursorCol());
    }

    @Test
    void deleteBackward_atStartOfRow_joinsWithPrevious() {
        TextAreaNode ta = TextArea.of("foo\nbar", 5);
        ta.setCursorForTest(1, 0);
        ta.deleteBackward();
        assertEquals("foobar", ta.getText());
        assertEquals(1, ta.getLines().size());
        assertEquals(0, ta.getCursorRow());
        assertEquals(3, ta.getCursorCol());
    }

    @Test
    void deleteBackward_atStartOfFirstRow_noOp() {
        TextAreaNode ta = TextArea.of("abc", 5);
        // cursor already at (0,0)
        ta.deleteBackward();
        assertEquals("abc", ta.getText());
        assertEquals(0, ta.getCursorRow());
        assertEquals(0, ta.getCursorCol());
    }

    @Test
    void deleteBackward_firesOnChange() {
        AtomicInteger count = new AtomicInteger();
        TextAreaNode ta = TextArea.of("x", 5, count::incrementAndGet);
        ta.end();
        ta.deleteBackward();
        assertEquals(1, count.get());
    }

    // -----------------------------------------------------------------------
    // deleteForward
    // -----------------------------------------------------------------------

    @Test
    void deleteForward_removesCharAtCursor() {
        TextAreaNode ta = TextArea.of("abc", 5);
        // cursor at (0,0)
        ta.deleteForward();
        assertEquals("bc", ta.getText());
        assertEquals(0, ta.getCursorCol());
    }

    @Test
    void deleteForward_atEndOfRow_joinsNextLine() {
        TextAreaNode ta = TextArea.of("foo\nbar", 5);
        ta.setCursorForTest(0, 3); // end of "foo"
        ta.deleteForward();
        assertEquals("foobar", ta.getText());
        assertEquals(1, ta.getLines().size());
        assertEquals(0, ta.getCursorRow());
        assertEquals(3, ta.getCursorCol());
    }

    @Test
    void deleteForward_atEndOfLastRow_noOp() {
        TextAreaNode ta = TextArea.of("abc", 5);
        ta.end();
        ta.deleteForward();
        assertEquals("abc", ta.getText());
    }

    @Test
    void deleteForward_firesOnChange() {
        AtomicInteger count = new AtomicInteger();
        TextAreaNode ta = TextArea.of("x", 5, count::incrementAndGet);
        ta.deleteForward();
        assertEquals(1, count.get());
    }

    // -----------------------------------------------------------------------
    // newline
    // -----------------------------------------------------------------------

    @Test
    void newline_splitsLineAtCursor() {
        TextAreaNode ta = TextArea.of("abcdef", 5);
        ta.setCursorForTest(0, 3);
        ta.newline();
        assertEquals(2, ta.getLines().size());
        assertEquals("abc", ta.getLines().get(0));
        assertEquals("def", ta.getLines().get(1));
    }

    @Test
    void newline_cursorAdvancesToNewRow() {
        TextAreaNode ta = TextArea.of("hello", 5);
        ta.setCursorForTest(0, 2);
        ta.newline();
        assertEquals(1, ta.getCursorRow());
        assertEquals(0, ta.getCursorCol());
    }

    @Test
    void newline_atEndOfLine_addsEmptyLine() {
        TextAreaNode ta = TextArea.of("hello", 5);
        ta.end();
        ta.newline();
        assertEquals(2, ta.getLines().size());
        assertEquals("hello", ta.getLines().get(0));
        assertEquals("", ta.getLines().get(1));
    }

    @Test
    void newline_atStartOfLine_addsEmptyLineBefore() {
        TextAreaNode ta = TextArea.of("hello", 5);
        // cursor at (0,0)
        ta.newline();
        assertEquals(2, ta.getLines().size());
        assertEquals("", ta.getLines().get(0));
        assertEquals("hello", ta.getLines().get(1));
        assertEquals(1, ta.getCursorRow());
        assertEquals(0, ta.getCursorCol());
    }

    @Test
    void newline_firesOnChange() {
        AtomicInteger count = new AtomicInteger();
        TextAreaNode ta = TextArea.of("x", 5, count::incrementAndGet);
        ta.newline();
        assertEquals(1, count.get());
    }

    // -----------------------------------------------------------------------
    // moveLeft / moveRight
    // -----------------------------------------------------------------------

    @Test
    void moveLeft_decrementsCursorCol() {
        TextAreaNode ta = TextArea.of("abc", 5);
        ta.setCursorForTest(0, 2);
        ta.moveLeft();
        assertEquals(1, ta.getCursorCol());
    }

    @Test
    void moveLeft_atStartOfNonFirstRow_wrapsToEndOfPreviousRow() {
        TextAreaNode ta = TextArea.of("hello\nworld", 5);
        ta.setCursorForTest(1, 0);
        ta.moveLeft();
        assertEquals(0, ta.getCursorRow());
        assertEquals(5, ta.getCursorCol()); // length of "hello"
    }

    @Test
    void moveLeft_atStartOfFirstRow_noOp() {
        TextAreaNode ta = TextArea.of("abc", 5);
        ta.moveLeft();
        assertEquals(0, ta.getCursorRow());
        assertEquals(0, ta.getCursorCol());
    }

    @Test
    void moveRight_incrementsCursorCol() {
        TextAreaNode ta = TextArea.of("abc", 5);
        ta.moveRight();
        assertEquals(1, ta.getCursorCol());
    }

    @Test
    void moveRight_atEndOfNonLastRow_wrapsToStartOfNextRow() {
        TextAreaNode ta = TextArea.of("hello\nworld", 5);
        ta.setCursorForTest(0, 5); // end of "hello"
        ta.moveRight();
        assertEquals(1, ta.getCursorRow());
        assertEquals(0, ta.getCursorCol());
    }

    @Test
    void moveRight_atEndOfLastRow_noOp() {
        TextAreaNode ta = TextArea.of("abc", 5);
        ta.end();
        ta.moveRight();
        assertEquals(0, ta.getCursorRow());
        assertEquals(3, ta.getCursorCol());
    }

    // -----------------------------------------------------------------------
    // moveUp / moveDown
    // -----------------------------------------------------------------------

    @Test
    void moveUp_decrementsCursorRow() {
        TextAreaNode ta = TextArea.of("abc\ndef", 5);
        ta.setCursorForTest(1, 2);
        ta.moveUp();
        assertEquals(0, ta.getCursorRow());
        assertEquals(2, ta.getCursorCol());
    }

    @Test
    void moveUp_clampsCursorColToLineLength() {
        TextAreaNode ta = TextArea.of("hi\nlongerline", 5);
        ta.setCursorForTest(1, 10);
        ta.moveUp();
        assertEquals(0, ta.getCursorRow());
        assertEquals(2, ta.getCursorCol()); // "hi".length() == 2
    }

    @Test
    void moveUp_atFirstRow_noOp() {
        TextAreaNode ta = TextArea.of("abc", 5);
        ta.moveUp();
        assertEquals(0, ta.getCursorRow());
    }

    @Test
    void moveDown_incrementsCursorRow() {
        TextAreaNode ta = TextArea.of("abc\ndef", 5);
        ta.moveDown();
        assertEquals(1, ta.getCursorRow());
    }

    @Test
    void moveDown_clampsCursorColToLineLength() {
        TextAreaNode ta = TextArea.of("longerline\nhi", 5);
        ta.setCursorForTest(0, 10);
        ta.moveDown();
        assertEquals(1, ta.getCursorRow());
        assertEquals(2, ta.getCursorCol()); // "hi".length() == 2
    }

    @Test
    void moveDown_atLastRow_noOp() {
        TextAreaNode ta = TextArea.of("abc", 5);
        ta.moveDown();
        assertEquals(0, ta.getCursorRow());
    }

    // -----------------------------------------------------------------------
    // home / end
    // -----------------------------------------------------------------------

    @Test
    void home_setsCursorColToZero() {
        TextAreaNode ta = TextArea.of("hello", 5);
        ta.end();
        ta.home();
        assertEquals(0, ta.getCursorCol());
    }

    @Test
    void end_setsCursorColToLineLength() {
        TextAreaNode ta = TextArea.of("hello", 5);
        ta.end();
        assertEquals(5, ta.getCursorCol());
    }

    @Test
    void home_onEmptyLine_staysAtZero() {
        TextAreaNode ta = TextArea.of("", 5);
        ta.home();
        assertEquals(0, ta.getCursorCol());
    }

    // -----------------------------------------------------------------------
    // Scrolling
    // -----------------------------------------------------------------------

    @Test
    void scrollRow_adjustsWhenCursorMovesBelow() {
        // maxHeight=3, insert 5 lines and move down
        TextAreaNode ta = TextArea.of("L0\nL1\nL2\nL3\nL4", 3);
        // cursor at row 0; move down 3 times → row 3 which is outside [0, 3)
        ta.moveDown();
        ta.moveDown();
        ta.moveDown();
        assertEquals(3, ta.getCursorRow());
        // scrollRow should have adjusted so row 3 is visible
        assertTrue(ta.getScrollRow() <= 3);
        assertTrue(ta.getCursorRow() < ta.getScrollRow() + ta.getMaxHeight());
    }

    @Test
    void scrollRow_adjustsWhenCursorMovesAbove() {
        TextAreaNode ta = TextArea.of("L0\nL1\nL2\nL3\nL4", 3);
        // Move cursor down to row 4, which forces scrollRow = 2
        ta.moveDown(); ta.moveDown(); ta.moveDown(); ta.moveDown();
        assertTrue(ta.getScrollRow() > 0);
        // Now move back up until row 0
        ta.moveUp(); ta.moveUp(); ta.moveUp(); ta.moveUp();
        assertEquals(0, ta.getScrollRow());
    }

    @Test
    void scrollRow_initiallyZero() {
        TextAreaNode ta = TextArea.of("a\nb\nc", 3);
        assertEquals(0, ta.getScrollRow());
    }

    @Test
    void scrollRow_adjustsOnNewline_pushingDown() {
        // maxHeight=2, add a third line and verify scroll
        TextAreaNode ta = TextArea.of("L0\nL1", 2);
        ta.setCursorForTest(1, 2);
        ta.newline(); // now 3 lines, cursor on row 2
        assertEquals(2, ta.getCursorRow());
        assertTrue(ta.getCursorRow() < ta.getScrollRow() + ta.getMaxHeight());
    }

    // -----------------------------------------------------------------------
    // Layout
    // -----------------------------------------------------------------------

    @Test
    void layout_height_minOfMaxHeightAndLineCount() {
        TextAreaNode ta = TextArea.of("L1\nL2\nL3", 5);
        engine.layout(ta, 0, 0, 20, 24);
        assertEquals(3, ta.getHeight()); // 3 lines < maxHeight 5
    }

    @Test
    void layout_height_cappedAtMaxHeight() {
        TextAreaNode ta = TextArea.of("L1\nL2\nL3\nL4\nL5\nL6", 3);
        engine.layout(ta, 0, 0, 20, 24);
        assertEquals(3, ta.getHeight());
    }

    @Test
    void layout_height_atLeastOne_whenEmpty() {
        TextAreaNode ta = TextArea.of("", 5);
        engine.layout(ta, 0, 0, 20, 24);
        assertEquals(1, ta.getHeight());
    }

    @Test
    void layout_width_equalsAvailableWidth() {
        TextAreaNode ta = TextArea.of("hello", 5);
        engine.layout(ta, 0, 0, 30, 24);
        assertEquals(30, ta.getWidth());
    }

    // -----------------------------------------------------------------------
    // Rendering via Renderer + FakeBackend
    // -----------------------------------------------------------------------

    static class FakeBackend implements io.github.yehorsyrin.tui.backend.TerminalBackend {
        final java.util.List<String> putChars = new java.util.ArrayList<>();
        @Override public void init() {}
        @Override public void shutdown() {}
        @Override public int getWidth()  { return 80; }
        @Override public int getHeight() { return 24; }
        @Override public void putChar(int col, int row, char c, Style s) {
            putChars.add(col + "," + row + "=" + c);
        }
        @Override public void flush() {}
        @Override public void hideCursor() {}
        @Override public void showCursor() {}
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() { putChars.clear(); }
        @Override public io.github.yehorsyrin.tui.event.KeyEvent readKey() {
            return io.github.yehorsyrin.tui.event.KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.EOF);
        }
        @Override public void setResizeListener(Runnable r) {}
    }

    @Test
    void render_singleLine_appearsOnRow0() {
        FakeBackend backend = new FakeBackend();
        TextAreaNode ta = TextArea.of("hi", 5);
        new Renderer(backend).render(ta);
        assertTrue(backend.putChars.stream().anyMatch(e -> e.equals("0,0=h")),
                "Expected 'h' at col 0, row 0; got: " + backend.putChars);
        assertTrue(backend.putChars.stream().anyMatch(e -> e.equals("1,0=i")),
                "Expected 'i' at col 1, row 0; got: " + backend.putChars);
    }

    @Test
    void render_multiLine_appearsOnCorrectRows() {
        FakeBackend backend = new FakeBackend();
        TextAreaNode ta = TextArea.of("abc\ndef", 5);
        new Renderer(backend).render(ta);
        assertTrue(backend.putChars.stream().anyMatch(e -> e.equals("0,0=a")),
                "Expected 'a' at row 0");
        assertTrue(backend.putChars.stream().anyMatch(e -> e.equals("0,1=d")),
                "Expected 'd' at row 1; got: " + backend.putChars);
    }

    @Test
    void render_scrolled_onlyVisibleRowsAppear() {
        FakeBackend backend = new FakeBackend();
        // 4 lines, maxHeight=2, scroll to show rows 2 and 3
        TextAreaNode ta = TextArea.of("R0\nR1\nR2\nR3", 2);
        // Scroll down by moving cursor to row 3
        ta.moveDown(); ta.moveDown(); ta.moveDown();
        new Renderer(backend).render(ta);
        // Row 0 in terminal should show line R2
        assertTrue(backend.putChars.stream().anyMatch(e -> e.equals("0,0=R")),
                "Expected 'R' at col 0, row 0 (for R2); got: " + backend.putChars);
        // Row 2 and 3 from original content should NOT appear at terminal rows 2/3
        // because maxHeight is 2, only 2 terminal rows are used
        assertFalse(backend.putChars.stream().anyMatch(e -> e.matches("\\d+,2=.*")),
                "No cells should appear at terminal row 2 when maxHeight=2");
    }

    @Test
    void render_focused_cursorCellRendered() {
        FakeBackend backend = new FakeBackend();
        TextAreaNode ta = TextArea.of("hello", 5);
        ta.setFocused(true);
        // cursor at (0,0) — 'h' should be rendered with cursorStyle
        new Renderer(backend).render(ta);
        // The cell at (0,0) should still contain 'h' (style varies, but char is correct)
        assertTrue(backend.putChars.stream().anyMatch(e -> e.equals("0,0=h")),
                "Expected 'h' at cursor position 0,0; got: " + backend.putChars);
    }

    @Test
    void render_unfocused_noCursorStyleDifference_charStillPresent() {
        FakeBackend backend = new FakeBackend();
        TextAreaNode ta = TextArea.of("hello", 5);
        ta.setFocused(false);
        new Renderer(backend).render(ta);
        assertTrue(backend.putChars.stream().anyMatch(e -> e.equals("0,0=h")),
                "Expected 'h' at col 0, row 0");
    }

    // -----------------------------------------------------------------------
    // Focusable
    // -----------------------------------------------------------------------

    @Test
    void initialFocus_isFalse() {
        assertFalse(TextArea.of(5).isFocused());
    }

    @Test
    void setFocused_true() {
        TextAreaNode ta = TextArea.of(5);
        ta.setFocused(true);
        assertTrue(ta.isFocused());
    }

    @Test
    void getFocusId_returnsKey() {
        TextAreaNode ta = TextArea.of(5);
        ta.setKey("myKey");
        assertEquals("myKey", ta.getFocusId());
    }

    // -----------------------------------------------------------------------
    // getLines returns unmodifiable view
    // -----------------------------------------------------------------------

    @Test
    void getLines_isUnmodifiable() {
        TextAreaNode ta = TextArea.of("a\nb", 5);
        assertThrows(UnsupportedOperationException.class,
                () -> ta.getLines().add("c"));
    }

    // -----------------------------------------------------------------------
    // Helper: set cursor position directly for testing
    // -----------------------------------------------------------------------

    // We add a package-private helper in TextAreaNode for tests, but since
    // tests are in the same package we can use the public API to navigate.
    // For precise cursor placement we expose a helper only used in tests.
}
