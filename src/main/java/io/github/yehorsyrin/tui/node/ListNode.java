package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.style.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * A scrollable list of string items.
 *
 * @author Jarvis (AI)
 */
public class ListNode extends Node {

    private final List<String> items;
    private int selectedIndex;
    private int scrollOffset;
    private Style selectedStyle;
    private Style normalStyle;

    public ListNode(List<String> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.selectedIndex = 0;
        this.scrollOffset = 0;
        this.selectedStyle = Style.DEFAULT.withBold(true);
        this.normalStyle = Style.DEFAULT;
    }

    public List<String> getItems() { return items; }
    public int getSelectedIndex() { return selectedIndex; }
    public int getScrollOffset() { return scrollOffset; }
    public Style getSelectedStyle() { return selectedStyle; }
    public Style getNormalStyle() { return normalStyle; }

    public void setSelectedIndex(int index) {
        if (items.isEmpty()) return;
        this.selectedIndex = Math.max(0, Math.min(index, items.size() - 1));
        adjustScroll();
    }

    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, offset);
    }

    public ListNode selectedStyle(Style style) {
        this.selectedStyle = style;
        return this;
    }

    public ListNode normalStyle(Style style) {
        this.normalStyle = style;
        return this;
    }

    private void adjustScroll() {
        if (height <= 0) return;
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + height) {
            scrollOffset = selectedIndex - height + 1;
        }
    }
}
