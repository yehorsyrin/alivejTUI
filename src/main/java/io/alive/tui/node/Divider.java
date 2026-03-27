package io.alive.tui.node;

/**
 * Factory for {@link DividerNode}.
 *
 * <pre>{@code
 * Divider.horizontal()
 * Divider.vertical()
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Divider {

    private Divider() {}

    public static DividerNode horizontal() {
        return new DividerNode(DividerNode.Orientation.HORIZONTAL);
    }

    public static DividerNode vertical() {
        return new DividerNode(DividerNode.Orientation.VERTICAL);
    }
}
