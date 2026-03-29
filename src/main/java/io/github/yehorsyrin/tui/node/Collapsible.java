package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Node;

/**
 * Factory for {@link CollapsibleNode}.
 *
 * <pre>{@code
 * CollapsibleNode section = Collapsible.of("Settings",
 *     Text.of("Theme: Dark"),
 *     Text.of("Font size: 14")
 * );
 * // Starts collapsed; toggle with:
 * bus.register(KeyType.Character(' '), () -> { section.toggle(); markDirty(); });
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Collapsible {

    private Collapsible() {}

    /**
     * Creates a collapsible section that starts collapsed.
     *
     * @param title    the section header
     * @param children child nodes to reveal when expanded
     */
    public static CollapsibleNode of(String title, Node... children) {
        return new CollapsibleNode(title, false, children);
    }

    /**
     * Creates a collapsible section that starts expanded.
     *
     * @param title    the section header
     * @param children child nodes to reveal when expanded
     */
    public static CollapsibleNode expanded(String title, Node... children) {
        return new CollapsibleNode(title, true, children);
    }
}
