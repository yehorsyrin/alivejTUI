package io.alive.tui.node;

import io.alive.tui.core.Node;

/**
 * Factory for {@link ScrollableVBoxNode}.
 *
 * <pre>{@code
 * ScrollableVBoxNode box = ScrollableVBox.of(10, child1, child2, child3);
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class ScrollableVBox {

    private ScrollableVBox() {}

    /**
     * Creates a scrollable vertical container with the given max visible height.
     *
     * @param maxHeight maximum visible rows
     * @param children  child nodes
     */
    public static ScrollableVBoxNode of(int maxHeight, Node... children) {
        return new ScrollableVBoxNode(maxHeight, children);
    }
}
