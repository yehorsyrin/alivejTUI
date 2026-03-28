package io.alive.tui.node;

import java.util.List;

/**
 * Factory for {@link VirtualListNode}.
 *
 * <pre>{@code
 * VirtualListNode list = VirtualList.of(bigData, 20);
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class VirtualList {

    private VirtualList() {}

    /**
     * Creates a virtual list with the given items and visible row limit.
     *
     * @param items          item list (held by reference — changes are reflected immediately)
     * @param maxVisibleRows maximum visible rows; must be ≥ 1
     */
    public static VirtualListNode of(List<String> items, int maxVisibleRows) {
        return new VirtualListNode(items, maxVisibleRows);
    }
}
