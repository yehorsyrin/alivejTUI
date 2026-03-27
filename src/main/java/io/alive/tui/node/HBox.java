package io.alive.tui.node;

import io.alive.tui.core.Node;
import java.util.List;

/**
 * Factory for {@link HBoxNode}.
 *
 * <pre>{@code
 * HBox.of(input, button).gap(1)
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class HBox {

    private HBox() {}

    public static HBoxNode of(Node... children) {
        return new HBoxNode(0, children);
    }

    public static HBoxNode of(List<Node> children) {
        return new HBoxNode(0, children.toArray(new Node[0]));
    }
}
