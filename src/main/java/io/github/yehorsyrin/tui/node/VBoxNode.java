package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Node;

/**
 * A container that lays out its children vertically (top to bottom).
 *
 * @author Jarvis (AI)
 */
public class VBoxNode extends Node {

    private int gap;

    public VBoxNode(int gap, Node... children) {
        this.gap = gap;
        for (Node child : children) {
            if (child != null) this.children.add(child);
        }
    }

    public int getGap() { return gap; }

    public VBoxNode gap(int gap) {
        this.gap = gap;
        return this;
    }
}
