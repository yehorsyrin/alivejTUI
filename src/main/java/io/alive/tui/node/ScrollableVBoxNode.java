package io.alive.tui.node;

import io.alive.tui.core.Node;

/**
 * A vertically scrollable container that limits its visible height to {@code maxHeight} rows.
 *
 * <p>Children are laid out in their natural vertical order; {@link #scrollDown} and
 * {@link #scrollUp} shift the visible viewport. Only the rows in
 * {@code [scrollOffset, scrollOffset + maxHeight)} are rendered.
 *
 * <p>Usage:
 * <pre>{@code
 * ScrollableVBoxNode box = new ScrollableVBoxNode(10, child1, child2, child3);
 * box.scrollDown(1);
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public class ScrollableVBoxNode extends VBoxNode {

    private final int maxHeight;
    private int scrollOffset;
    private int contentHeight;   // set by LayoutEngine

    public ScrollableVBoxNode(int maxHeight, Node... children) {
        super(0, children);
        this.maxHeight    = Math.max(1, maxHeight);
        this.scrollOffset = 0;
    }

    /** The maximum number of visible rows. */
    public int getMaxHeight() { return maxHeight; }

    /** The number of rows scrolled past the top. */
    public int getScrollOffset() { return scrollOffset; }

    /**
     * The total height of all children combined (set by {@link io.alive.tui.layout.LayoutEngine}).
     * Used to cap scroll range.
     */
    public int getContentHeight() { return contentHeight; }

    /** Called by LayoutEngine after laying out children. */
    public void setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
    }

    /** Scroll down by {@code amount} rows (clamped so content stays visible). */
    public void scrollDown(int amount) {
        int maxOffset = Math.max(0, contentHeight - maxHeight);
        scrollOffset = Math.min(scrollOffset + amount, maxOffset);
    }

    /** Scroll up by {@code amount} rows (clamped to 0). */
    public void scrollUp(int amount) {
        scrollOffset = Math.max(0, scrollOffset - amount);
    }

    /** Set the scroll offset directly (clamped). */
    public void setScrollOffset(int offset) {
        scrollOffset = Math.max(0, offset);
    }
}
