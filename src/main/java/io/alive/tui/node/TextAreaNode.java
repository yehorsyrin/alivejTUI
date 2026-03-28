package io.alive.tui.node;

import io.alive.tui.core.Focusable;
import io.alive.tui.core.Node;
import io.alive.tui.style.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A multi-line text input component with row/column cursor management and vertical scrolling.
 *
 * @author Jarvis (AI)
 */
public class TextAreaNode extends Node implements Focusable {

    private List<String> lines;
    private int cursorRow;
    private int cursorCol;
    private int scrollRow;
    private int maxHeight;
    private Style style;
    private Style focusedStyle;
    private Style cursorStyle;
    private boolean focused;
    private Runnable onChange;

    public TextAreaNode(String initialText, int maxHeight, Runnable onChange) {
        this.maxHeight    = Math.max(1, maxHeight);
        this.onChange     = onChange;
        this.style        = Style.DEFAULT;
        this.focusedStyle = Style.DEFAULT;
        this.cursorStyle  = Style.DEFAULT.withBold(true);
        this.lines        = new ArrayList<>();
        if (initialText == null || initialText.isEmpty()) {
            this.lines.add("");
        } else {
            for (String line : initialText.split("\n", -1)) {
                this.lines.add(line);
            }
        }
        this.cursorRow = 0;
        this.cursorCol = 0;
        this.scrollRow = 0;
    }

    // --- Text access ---

    /** Returns all text as a single newline-joined string. */
    public String getText() {
        return String.join("\n", lines);
    }

    /** Replaces all content, resets cursor to (0, 0). */
    public void setText(String text) {
        lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
        } else {
            for (String line : text.split("\n", -1)) {
                lines.add(line);
            }
        }
        cursorRow = 0;
        cursorCol = 0;
        scrollRow = 0;
        notifyChange();
    }

    /** Returns an unmodifiable view of the lines list. */
    public List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }

    // --- Editing ---

    /** Inserts a character at the current cursor position and advances cursorCol. */
    public void insertChar(char c) {
        String line = lines.get(cursorRow);
        lines.set(cursorRow, line.substring(0, cursorCol) + c + line.substring(cursorCol));
        cursorCol++;
        notifyChange();
    }

    /**
     * Backspace: deletes the character before the cursor.
     * If at the start of a row (col == 0) and not the first row, joins with the previous line.
     */
    public void deleteBackward() {
        if (cursorCol > 0) {
            String line = lines.get(cursorRow);
            lines.set(cursorRow, line.substring(0, cursorCol - 1) + line.substring(cursorCol));
            cursorCol--;
        } else if (cursorRow > 0) {
            String prevLine = lines.get(cursorRow - 1);
            String currLine = lines.get(cursorRow);
            int newCol = prevLine.length();
            lines.set(cursorRow - 1, prevLine + currLine);
            lines.remove(cursorRow);
            cursorRow--;
            cursorCol = newCol;
            adjustScroll();
        }
        notifyChange();
    }

    /**
     * Delete: deletes the character at the cursor.
     * If at the end of a row and not the last row, joins the next line.
     */
    public void deleteForward() {
        String line = lines.get(cursorRow);
        if (cursorCol < line.length()) {
            lines.set(cursorRow, line.substring(0, cursorCol) + line.substring(cursorCol + 1));
        } else if (cursorRow < lines.size() - 1) {
            String nextLine = lines.get(cursorRow + 1);
            lines.set(cursorRow, line + nextLine);
            lines.remove(cursorRow + 1);
        }
        notifyChange();
    }

    /**
     * Enter: splits the current line at the cursor, moves to the start of the new line.
     */
    public void newline() {
        String line = lines.get(cursorRow);
        String before = line.substring(0, cursorCol);
        String after  = line.substring(cursorCol);
        lines.set(cursorRow, before);
        lines.add(cursorRow + 1, after);
        cursorRow++;
        cursorCol = 0;
        adjustScroll();
        notifyChange();
    }

    // --- Cursor movement ---

    /** Move cursor one column to the left; wraps to end of previous row. */
    public void moveLeft() {
        if (cursorCol > 0) {
            cursorCol--;
        } else if (cursorRow > 0) {
            cursorRow--;
            cursorCol = lines.get(cursorRow).length();
            adjustScroll();
        }
    }

    /** Move cursor one column to the right; wraps to start of next row. */
    public void moveRight() {
        String line = lines.get(cursorRow);
        if (cursorCol < line.length()) {
            cursorCol++;
        } else if (cursorRow < lines.size() - 1) {
            cursorRow++;
            cursorCol = 0;
            adjustScroll();
        }
    }

    /** Move cursor up one row, clamping column to line length. */
    public void moveUp() {
        if (cursorRow > 0) {
            cursorRow--;
            cursorCol = Math.min(cursorCol, lines.get(cursorRow).length());
            adjustScroll();
        }
    }

    /** Move cursor down one row, clamping column to line length. */
    public void moveDown() {
        if (cursorRow < lines.size() - 1) {
            cursorRow++;
            cursorCol = Math.min(cursorCol, lines.get(cursorRow).length());
            adjustScroll();
        }
    }

    /** Move cursor to start of current line. */
    public void home() {
        cursorCol = 0;
    }

    /** Move cursor to end of current line. */
    public void end() {
        cursorCol = lines.get(cursorRow).length();
    }

    // --- Scroll adjustment ---

    private void adjustScroll() {
        // Ensure cursorRow is within [scrollRow, scrollRow + maxHeight)
        if (cursorRow < scrollRow) {
            scrollRow = cursorRow;
        } else if (cursorRow >= scrollRow + maxHeight) {
            scrollRow = cursorRow - maxHeight + 1;
        }
    }

    // --- Focusable ---

    @Override
    public void setFocused(boolean focused) { this.focused = focused; }

    @Override
    public boolean isFocused() { return focused; }

    @Override
    public String getFocusId() { return getKey(); }

    // --- Getters & fluent setters ---

    public int getCursorRow()    { return cursorRow; }
    public int getCursorCol()    { return cursorCol; }
    public int getScrollRow()    { return scrollRow; }
    public int getMaxHeight()    { return maxHeight; }

    public Style getStyle()        { return style; }
    public Style getFocusedStyle() { return focusedStyle; }
    public Style getCursorStyle()  { return cursorStyle; }
    public Runnable getOnChange()  { return onChange; }

    public TextAreaNode style(Style style) {
        this.style = style != null ? style : Style.DEFAULT;
        return this;
    }

    public TextAreaNode focusedStyle(Style focusedStyle) {
        this.focusedStyle = focusedStyle != null ? focusedStyle : Style.DEFAULT;
        return this;
    }

    public TextAreaNode cursorStyle(Style cursorStyle) {
        this.cursorStyle = cursorStyle != null ? cursorStyle : Style.DEFAULT.withBold(true);
        return this;
    }

    public TextAreaNode maxHeight(int maxHeight) {
        this.maxHeight = Math.max(1, maxHeight);
        return this;
    }

    /**
     * Sets the cursor position directly. Intended for testing purposes.
     * Values are clamped to valid bounds.
     *
     * @param row target cursor row
     * @param col target cursor column
     */
    public void setCursorForTest(int row, int col) {
        cursorRow = Math.max(0, Math.min(row, lines.size() - 1));
        cursorCol = Math.max(0, Math.min(col, lines.get(cursorRow).length()));
        adjustScroll();
    }

    private void notifyChange() {
        if (onChange != null) onChange.run();
    }
}
