package io.alive.tui.style;

import io.alive.tui.core.Node;

import java.util.Objects;

/**
 * A CSS-like selector that matches {@link Node}s by id, class name, or node type.
 *
 * <p>Selectors can be combined with {@link #and(Selector)} for conjunction matching.
 *
 * <pre>{@code
 * Selector.byId("submit")                           // matches id="submit"
 * Selector.byClass("primary")                       // matches className="primary"
 * Selector.byType(ButtonNode.class)                 // matches any ButtonNode
 * Selector.byClass("error").and(Selector.byType(TextNode.class))  // both conditions
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Selector {

    /** Selector predicate. */
    @FunctionalInterface
    private interface Predicate {
        boolean test(Node node);
    }

    private final Predicate predicate;
    private final String    description;

    private Selector(Predicate predicate, String description) {
        this.predicate   = predicate;
        this.description = description;
    }

    // --- Factories ---

    /**
     * Matches nodes whose {@link Node#getId()} equals {@code id}.
     *
     * @param id the id to match; must not be {@code null}
     */
    public static Selector byId(String id) {
        Objects.requireNonNull(id, "id must not be null");
        return new Selector(n -> id.equals(n.getId()), "#" + id);
    }

    /**
     * Matches nodes whose {@link Node#getClassName()} equals {@code className}.
     *
     * @param className the class name to match; must not be {@code null}
     */
    public static Selector byClass(String className) {
        Objects.requireNonNull(className, "className must not be null");
        return new Selector(n -> className.equals(n.getClassName()), "." + className);
    }

    /**
     * Matches nodes that are instances of the given type.
     *
     * @param type the node class; must not be {@code null}
     */
    public static Selector byType(Class<? extends Node> type) {
        Objects.requireNonNull(type, "type must not be null");
        return new Selector(n -> type.isInstance(n), type.getSimpleName());
    }

    /**
     * Returns a selector that matches only when both this selector AND {@code other} match.
     *
     * @param other the second condition; must not be {@code null}
     */
    public Selector and(Selector other) {
        Objects.requireNonNull(other, "other must not be null");
        return new Selector(
                n -> this.matches(n) && other.matches(n),
                this.description + " && " + other.description);
    }

    /**
     * Returns a selector that matches when either this selector OR {@code other} matches.
     *
     * @param other the alternative condition; must not be {@code null}
     */
    public Selector or(Selector other) {
        Objects.requireNonNull(other, "other must not be null");
        return new Selector(
                n -> this.matches(n) || other.matches(n),
                this.description + " || " + other.description);
    }

    /**
     * Tests whether this selector matches the given node.
     *
     * @param node the node to test; {@code null} always returns {@code false}
     * @return {@code true} if the node matches
     */
    public boolean matches(Node node) {
        return node != null && predicate.test(node);
    }

    @Override
    public String toString() {
        return "Selector[" + description + "]";
    }
}
