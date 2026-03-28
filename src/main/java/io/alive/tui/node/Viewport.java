package io.alive.tui.node;

import io.alive.tui.core.Node;

/**
 * Factory for {@link ViewportNode}.
 *
 * <pre>{@code
 * ViewportNode vp = Viewport.of(VBox.of(lines), 10);
 * vp.scrollDown();
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Viewport {

    private Viewport() {}

    /**
     * Creates a viewport that displays up to {@code height} rows of {@code content}.
     *
     * @param content   the node to clip
     * @param height    number of visible rows
     */
    public static ViewportNode of(Node content, int height) {
        return new ViewportNode(content, height);
    }

    /**
     * Creates a viewport with the scroll bar hidden.
     *
     * @param content the node to clip
     * @param height  number of visible rows
     */
    public static ViewportNode plain(Node content, int height) {
        return new ViewportNode(content, height).showScrollbar(false);
    }
}
