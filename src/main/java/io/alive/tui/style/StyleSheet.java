package io.alive.tui.style;

import io.alive.tui.core.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * A CSS-like style sheet that maps {@link Selector}s to {@link Style}s.
 *
 * <p>Rules are evaluated in order of insertion. When multiple selectors match the same
 * node, their styles are merged (later rules override earlier ones).
 *
 * <pre>{@code
 * StyleSheet sheet = new StyleSheet()
 *     .add(Selector.byClass("muted"), Style.DEFAULT.withDim(true))
 *     .add(Selector.byId("title"),   Style.DEFAULT.withBold(true).withForeground(Color.CYAN));
 *
 * sheet.applyToTree(root);   // walks the tree and calls node.setComputedStyle(...)
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class StyleSheet {

    private record Rule(Selector selector, Style style) {}

    private final List<Rule> rules = new ArrayList<>();

    /**
     * Adds a rule mapping {@code selector} → {@code style}.
     * Returns {@code this} for fluent chaining.
     *
     * @param selector the selector; must not be {@code null}
     * @param style    the style to apply; must not be {@code null}
     */
    public StyleSheet add(Selector selector, Style style) {
        if (selector == null) throw new IllegalArgumentException("selector must not be null");
        if (style    == null) throw new IllegalArgumentException("style must not be null");
        rules.add(new Rule(selector, style));
        return this;
    }

    /**
     * Resolves the merged {@link Style} for the given node by applying all matching rules
     * in insertion order. Later rules override earlier ones.
     *
     * @param node the node to evaluate; must not be {@code null}
     * @return the merged style, or {@link Style#DEFAULT} if no rules match
     */
    public Style resolve(Node node) {
        if (node == null) throw new IllegalArgumentException("node must not be null");
        Style result = null;
        for (Rule rule : rules) {
            if (rule.selector().matches(node)) {
                result = result == null ? rule.style() : merge(result, rule.style());
            }
        }
        return result != null ? result : Style.DEFAULT;
    }

    /**
     * Walks the entire node tree depth-first and stores the resolved style on each node
     * via {@link Node#setComputedStyle(Style)}.
     *
     * <p>Nodes with no matching rules receive {@code null} (the computed style is not set).
     *
     * @param root the root of the tree to style; {@code null} is a no-op
     */
    public void applyToTree(Node root) {
        if (root == null) return;
        boolean hasMatch = false;
        for (Rule rule : rules) {
            if (rule.selector().matches(root)) { hasMatch = true; break; }
        }
        if (hasMatch) root.setComputedStyle(resolve(root));
        for (Node child : root.getChildren()) applyToTree(child);
    }

    /** Returns the number of rules registered in this style sheet. */
    public int ruleCount() {
        return rules.size();
    }

    /**
     * Merges two styles: fields from {@code override} take precedence over {@code base}
     * for non-default (non-null, non-false) values.
     */
    private static Style merge(Style base, Style override) {
        Style result = base;
        if (override.getForeground() != null)  result = result.withForeground(override.getForeground());
        if (override.getBackground() != null)  result = result.withBackground(override.getBackground());
        if (override.isBold())                 result = result.withBold(true);
        if (override.isItalic())               result = result.withItalic(true);
        if (override.isUnderline())            result = result.withUnderline(true);
        if (override.isDim())                  result = result.withDim(true);
        if (override.isStrikethrough())        result = result.withStrikethrough(true);
        return result;
    }
}
