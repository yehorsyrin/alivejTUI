package io.alive.tui.node;

import io.alive.tui.core.Node;
import io.alive.tui.style.Style;

import java.util.List;

/**
 * A lazily rendered list optimised for very large datasets.
 *
 * <p>Unlike {@link ListNode}, which stores items as an {@code ArrayList}, this node
 * holds any {@link List} reference directly (no defensive copy). The layout engine
 * and renderer only visit the <em>visible window</em>, so rendering cost is O(visibleRows)
 * regardless of total item count.
 *
 * <pre>{@code
 * List<String> bigData = generate10kRows();
 * VirtualListNode list = VirtualList.of(bigData, 20);  // show 20 rows at a time
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public class VirtualListNode extends Node {

    private final List<String> items;
    private final int          maxVisibleRows;

    private int   selectedIndex = 0;
    private int   scrollOffset  = 0;
    private Style selectedStyle = Style.DEFAULT.withBold(true);
    private Style normalStyle   = Style.DEFAULT;

    /**
     * Creates a virtual list node.
     *
     * @param items          the full item list (not copied — held by reference)
     * @param maxVisibleRows maximum number of rows visible at once; must be ≥ 1
     */
    public VirtualListNode(List<String> items, int maxVisibleRows) {
        if (maxVisibleRows < 1) throw new IllegalArgumentException("maxVisibleRows must be ≥ 1");
        this.items          = items != null ? items : List.of();
        this.maxVisibleRows = maxVisibleRows;
        this.selectedIndex  = this.items.isEmpty() ? -1 : 0;
    }

    // --- Accessors ---

    public List<String> getItems()         { return items; }
    public int          getMaxVisibleRows() { return maxVisibleRows; }
    public int          getSelectedIndex()  { return selectedIndex; }
    public int          getScrollOffset()   { return scrollOffset; }
    public Style        getSelectedStyle()  { return selectedStyle; }
    public Style        getNormalStyle()    { return normalStyle; }

    /** Returns the total number of items. */
    public int itemCount() { return items.size(); }

    /** Returns the number of currently visible rows (≤ {@link #getMaxVisibleRows()}). */
    public int visibleRowCount() {
        return Math.min(maxVisibleRows, items.size() - scrollOffset);
    }

    /**
     * Returns the slice of items currently in the visible window.
     *
     * @return sub-list from {@code scrollOffset} to {@code scrollOffset + visibleRowCount()}
     */
    public List<String> getVisibleItems() {
        if (items.isEmpty()) return List.of();
        int from = scrollOffset;
        int to   = Math.min(from + maxVisibleRows, items.size());
        return items.subList(from, to);
    }

    /** Returns the number of rows that can be scrolled past (0 if all rows fit). */
    public int scrollableRows() {
        return Math.max(0, items.size() - maxVisibleRows);
    }

    /** Returns {@code true} if the list has more items than the visible area. */
    public boolean isScrollable() {
        return scrollableRows() > 0;
    }

    // --- Navigation ---

    /** Moves the selection to the next item (with scroll). */
    public void selectDown() {
        if (items.isEmpty() || selectedIndex >= items.size() - 1) return;
        selectedIndex++;
        if (selectedIndex >= scrollOffset + maxVisibleRows) {
            scrollOffset = selectedIndex - maxVisibleRows + 1;
        }
    }

    /** Moves the selection to the previous item (with scroll). */
    public void selectUp() {
        if (items.isEmpty() || selectedIndex <= 0) return;
        selectedIndex--;
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        }
    }

    /** Scrolls down by one page (maxVisibleRows rows). */
    public void pageDown() {
        if (items.isEmpty()) return;
        int newSel = Math.min(items.size() - 1, selectedIndex + maxVisibleRows);
        selectedIndex = newSel;
        scrollOffset  = Math.min(scrollableRows(), scrollOffset + maxVisibleRows);
        if (selectedIndex < scrollOffset) selectedIndex = scrollOffset;
    }

    /** Scrolls up by one page (maxVisibleRows rows). */
    public void pageUp() {
        if (items.isEmpty()) return;
        int newSel = Math.max(0, selectedIndex - maxVisibleRows);
        selectedIndex = newSel;
        scrollOffset  = Math.max(0, scrollOffset - maxVisibleRows);
        if (selectedIndex >= scrollOffset + maxVisibleRows) {
            selectedIndex = scrollOffset + maxVisibleRows - 1;
        }
    }

    /** Jumps to the first item. */
    public void selectFirst() {
        if (items.isEmpty()) return;
        selectedIndex = 0;
        scrollOffset  = 0;
    }

    /** Jumps to the last item. */
    public void selectLast() {
        if (items.isEmpty()) return;
        selectedIndex = items.size() - 1;
        scrollOffset  = scrollableRows();
    }

    /** Returns the text of the selected item, or {@code null} if nothing is selected. */
    public String getSelectedItem() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) return null;
        return items.get(selectedIndex);
    }

    // --- Configuration (fluent) ---

    public VirtualListNode selectedStyle(Style s) {
        this.selectedStyle = s != null ? s : Style.DEFAULT; return this;
    }

    public VirtualListNode normalStyle(Style s) {
        this.normalStyle = s != null ? s : Style.DEFAULT; return this;
    }
}
