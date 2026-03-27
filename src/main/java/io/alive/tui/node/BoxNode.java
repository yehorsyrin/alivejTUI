package io.alive.tui.node;

import io.alive.tui.core.Node;
import io.alive.tui.style.Style;

/**
 * A container node with an optional border.
 * Accepts exactly one child node.
 *
 * @author Jarvis (AI)
 */
public class BoxNode extends Node {

    private boolean border;
    private Style borderStyle;

    public BoxNode(Node content, boolean border, Style borderStyle) {
        if (content != null) {
            this.children.add(content);
        }
        this.border = border;
        this.borderStyle = borderStyle != null ? borderStyle : Style.DEFAULT;
    }

    public boolean hasBorder() { return border; }
    public Style getBorderStyle() { return borderStyle; }

    public BoxNode border(boolean border) {
        this.border = border;
        return this;
    }

    public BoxNode borderStyle(Style style) {
        this.borderStyle = style;
        return this;
    }
}
