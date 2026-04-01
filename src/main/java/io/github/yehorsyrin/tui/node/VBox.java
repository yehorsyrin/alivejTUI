package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Node;
import java.util.List;

/**
 * Factory for {@link VBoxNode}.
 *
 * <pre>{@code
 * VBox.of(Text.of("A"), Text.of("B")).gap(1)
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class VBox {

    private VBox() {}

    public static VBoxNode of(Node... children) {
        return new VBoxNode(0, children);
    }

    public static VBoxNode of(List<Node> children) {
        return new VBoxNode(0, children.toArray(new Node[0]));
    }
}
