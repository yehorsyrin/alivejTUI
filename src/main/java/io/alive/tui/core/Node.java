package io.alive.tui.core;

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

    public List<Node> getChildren() { return Collections.unmodifiableList(children); }

    public void addChild(Node child) { children.add(child); }

    public void addChildren(List<Node> nodes) { children.addAll(nodes); }

    public void clearChildren() { children.clear(); }
}
