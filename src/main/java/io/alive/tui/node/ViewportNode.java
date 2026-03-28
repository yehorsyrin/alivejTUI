package io.alive.tui.node;

import io.alive.tui.core.Node;

/**
 * A generic scroll container that clips any content node to a fixed visible height
 * and renders an optional vertical scroll bar.
 *
 * <pre>{@code
 * Node content = VBox.of(Text.of("Line 1"), Text.of("Line 2"), ...);
 * ViewportNode vp = Viewport.of(content, 5);   // shows 5 rows at a time
 * vp.scrollDown();
 * }</pre>
 *
 * <p>The scroll bar occupies the rightmost column of the viewport's width.
 * It is only rendered when the content is taller than the visible area.
 * The thumb character is {@code ▓}; the track character is {@code │}.
 *
 * @author Jarvis (AI)
 */
public class ViewportNode extends Node {

    /** Scroll-bar thumb character (current position indicator). */
    public static final char THUMB_CHAR = '▓';
    /** Scroll-bar track character (background of scroll bar). */
    public static final char TRACK_CHAR = '│';

    private final int maxHeight;
    private int scrollOffset  = 0;
    private int contentHeight = 0;
    private boolean showScrollbar = true;

    /**
     * Creates a viewport.
     *
     * @param content   the node to clip; may be {@code null}
     * @param maxHeight maximum number of visible rows
     */
    public ViewportNode(Node content, int maxHeight) {
        this.maxHeight = Math.max(1, maxHeight);
        if (content != null) {
            this.children.add(content);
        }
    }

    /** Returns the maximum number of visible rows. */
    public int getMaxHeight() { return maxHeight; }

    /** Returns the current scroll offset (number of rows scrolled past the top). */
    public int getScrollOffset() { return scrollOffset; }

    /** Sets the scroll offset, clamped to {@code [0, scrollableRows()]}. */
    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(offset, scrollableRows()));
    }

    /** Returns the total height of the content node (set by the layout engine). */
    public int getContentHeight() { return contentHeight; }

    /** Called by the layout engine to record the content's natural height. */
    public void setContentHeight(int h) { this.contentHeight = Math.max(0, h); }

    /** Returns {@code true} if the scroll bar column should be rendered. */
    public boolean isShowScrollbar() { return showScrollbar; }

    /** Enables or disables the scroll bar. Returns {@code this} for chaining. */
    public ViewportNode showScrollbar(boolean show) { this.showScrollbar = show; return this; }

    /** Returns the number of rows that can be scrolled ({@code max(0, contentHeight - maxHeight)}). */
    public int scrollableRows() { return Math.max(0, contentHeight - maxHeight); }

    /** Returns {@code true} if the content is taller than the visible area. */
    public boolean isScrollable() { return scrollableRows() > 0; }

    // --- Scroll helpers ---

    /** Scrolls down by one row. */
    public void scrollDown() { setScrollOffset(scrollOffset + 1); }

    /** Scrolls up by one row. */
    public void scrollUp() { setScrollOffset(scrollOffset - 1); }

    /** Scrolls down by one page (visible height rows). */
    public void pageDown() { setScrollOffset(scrollOffset + maxHeight); }

    /** Scrolls up by one page (visible height rows). */
    public void pageUp() { setScrollOffset(scrollOffset - maxHeight); }

    /** Scrolls to the very top. */
    public void scrollToTop() { setScrollOffset(0); }

    /** Scrolls to the very bottom. */
    public void scrollToBottom() { setScrollOffset(scrollableRows()); }
}
