package io.github.yehorsyrin.tui.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all virtual tree nodes.
 *
 * @author Jarvis (AI)
 */
public abstract class Node {

    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected String key;
    protected final List<Node> children = new ArrayList<>();

    // CSS-like identity / classification
    private String id;
    private String className;

    // Style resolved by a StyleSheet (set externally; renderers may use it)
    private io.github.yehorsyrin.tui.style.Style computedStyle;

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    // --- CSS-like selectors ---

    /** Returns this node's id, or {@code null} if not set. */
    public String getId() { return id; }

    /** Returns this node's class name, or {@code null} if not set. */
    public String getClassName() { return className; }

    /** Sets the id and returns {@code this} for fluent use. */
    public Node withId(String id) { this.id = id; return this; }

    /** Sets the class name and returns {@code this} for fluent use. */
    public Node withClassName(String className) { this.className = className; return this; }

    /** Returns the computed style set by a {@link io.github.yehorsyrin.tui.style.StyleSheet}, or {@code null}. */
    public io.github.yehorsyrin.tui.style.Style getComputedStyle() { return computedStyle; }

    /** Called by {@link io.github.yehorsyrin.tui.style.StyleSheet} to store the resolved style. */
    public void setComputedStyle(io.github.yehorsyrin.tui.style.Style style) { this.computedStyle = style; }

    public List<Node> getChildren() { return Collections.unmodifiableList(children); }

    public void addChild(Node child) { children.add(child); }

    public void addChildren(List<Node> nodes) { children.addAll(nodes); }

    public void clearChildren() { children.clear(); }
}
