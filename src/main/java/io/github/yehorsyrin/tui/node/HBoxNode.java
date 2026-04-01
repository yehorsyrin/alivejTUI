package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Node;

/**
 * A container that lays out its children horizontally (left to right).
 *
 * @author Jarvis (AI)
 */
public class HBoxNode extends Node {

    private int gap;

    public HBoxNode(int gap, Node... children) {
        this.gap = gap;
        for (Node child : children) {
            if (child != null) this.children.add(child);
        }
    }

    public int getGap() { return gap; }

    public HBoxNode gap(int gap) {
        this.gap = gap;
        return this;
    }
}
