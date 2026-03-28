package io.alive.tui.layout;

import io.alive.tui.core.Node;
import io.alive.tui.node.*;

import java.util.List;

/**
 * Computes the position (x, y) and dimensions (width, height) for every node in the virtual tree.
 *
 * <p>Must be run before diffing or rendering. After layout, every node's x/y/width/height
 * fields are set to concrete terminal column/row values.
 *
 * @author Jarvis (AI)
 */
public class LayoutEngine {

    /**
     * Recursively lays out the entire tree starting from {@code root}.
     *
     * @param root            the root node
     * @param x               left edge (terminal column, 0-based)
     * @param y               top edge (terminal row, 0-based)
     * @param availableWidth  width available for this node
     * @param availableHeight height available for this node
     */
    public void layout(Node root, int x, int y, int availableWidth, int availableHeight) {
        if (root == null) return;
        availableWidth  = Math.max(0, availableWidth);
        availableHeight = Math.max(0, availableHeight);

        root.setX(x);
        root.setY(y);

        if (root instanceof TextNode text) {
            layoutText(text, availableWidth);
        } else if (root instanceof ScrollableVBoxNode svbox) {
            layoutScrollableVBox(svbox, x, y, availableWidth, availableHeight);
        } else if (root instanceof VBoxNode vbox) {
            layoutVBox(vbox, x, y, availableWidth, availableHeight);
        } else if (root instanceof HBoxNode hbox) {
            layoutHBox(hbox, x, y, availableWidth, availableHeight);
        } else if (root instanceof BoxNode box) {
            layoutBox(box, x, y, availableWidth, availableHeight);
        } else if (root instanceof ButtonNode btn) {
            layoutButton(btn, availableWidth);
        } else if (root instanceof InputNode) {
            root.setWidth(availableWidth);
            root.setHeight(1);
        } else if (root instanceof ListNode list) {
            root.setWidth(availableWidth);
            root.setHeight(Math.min(list.getItems().size(), Math.max(1, availableHeight)));
        } else if (root instanceof ProgressBarNode) {
            root.setWidth(availableWidth);
            root.setHeight(1);
        } else if (root instanceof SpinnerNode) {
            root.setWidth(1);
            root.setHeight(1);
        } else if (root instanceof DividerNode div) {
            layoutDivider(div, availableWidth, availableHeight);
        } else if (root instanceof CheckboxNode cb) {
            layoutCheckbox(cb, availableWidth);
        } else if (root instanceof HelpPanelNode help) {
            layoutHelpPanel(help, availableWidth);
        } else {
            // Unknown node type — fill available space
            root.setWidth(availableWidth);
            root.setHeight(availableHeight);
        }
    }

    // --- TextNode ---

    private void layoutText(TextNode node, int availableWidth) {
        int len = node.getText().length();
        node.setWidth(Math.min(len, availableWidth));
        node.setHeight(1);
    }

    // --- ScrollableVBoxNode ---

    private void layoutScrollableVBox(ScrollableVBoxNode svbox, int x, int y,
                                      int availableWidth, int availableHeight) {
        List<Node> children = svbox.getChildren();
        int gap = svbox.getGap();
        int totalHeight = 0;
        int currentY = y;

        // Layout children with unlimited height (content can exceed maxHeight)
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            layout(child, x, currentY, availableWidth, availableHeight);
            int childH = child.getHeight() + (i < children.size() - 1 ? gap : 0);
            currentY    += childH;
            totalHeight += childH;
        }

        svbox.setContentHeight(totalHeight);
        svbox.setWidth(availableWidth);
        svbox.setHeight(Math.min(svbox.getMaxHeight(), totalHeight));
    }

    // --- VBoxNode ---

    private void layoutVBox(VBoxNode vbox, int x, int y, int availableWidth, int availableHeight) {
        List<Node> children = vbox.getChildren();
        int gap = vbox.getGap();
        int totalHeight = 0;
        int currentY = y;

        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            int remainingHeight = Math.max(0, availableHeight - totalHeight);
            layout(child, x, currentY, availableWidth, remainingHeight);
            currentY  += child.getHeight() + (i < children.size() - 1 ? gap : 0);
            totalHeight += child.getHeight() + (i < children.size() - 1 ? gap : 0);
        }

        vbox.setWidth(availableWidth);
        vbox.setHeight(totalHeight);
    }

    // --- HBoxNode ---

    private void layoutHBox(HBoxNode hbox, int x, int y, int availableWidth, int availableHeight) {
        List<Node> children = hbox.getChildren();
        if (children.isEmpty()) {
            hbox.setWidth(0);
            hbox.setHeight(0);
            return;
        }

        int gap      = hbox.getGap();
        int totalGap = gap * Math.max(0, children.size() - 1);
        int usable   = Math.max(0, availableWidth - totalGap);
        int baseWidth = usable / children.size();
        int extra     = usable % children.size(); // distribute remainder to first N children

        int currentX  = x;
        int maxHeight = 0;

        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            int childWidth = baseWidth + (i < extra ? 1 : 0);
            layout(child, currentX, y, childWidth, availableHeight);
            // Advance by the allocated slot width, not the actual child width,
            // so that children are evenly spaced regardless of content size.
            currentX += childWidth + (i < children.size() - 1 ? gap : 0);
            maxHeight = Math.max(maxHeight, child.getHeight());
        }

        hbox.setWidth(availableWidth);
        hbox.setHeight(maxHeight);
    }

    // --- BoxNode ---

    private void layoutBox(BoxNode box, int x, int y, int availableWidth, int availableHeight) {
        if (box.hasBorder()) {
            int innerW = Math.max(0, availableWidth  - 2);
            int innerH = Math.max(0, availableHeight - 2);
            if (!box.getChildren().isEmpty()) {
                layout(box.getChildren().get(0), x + 1, y + 1, innerW, innerH);
                box.setWidth(availableWidth);
                box.setHeight(box.getChildren().get(0).getHeight() + 2);
            } else {
                box.setWidth(availableWidth);
                box.setHeight(availableHeight);
            }
        } else {
            if (!box.getChildren().isEmpty()) {
                layout(box.getChildren().get(0), x, y, availableWidth, availableHeight);
                box.setWidth(box.getChildren().get(0).getWidth());
                box.setHeight(box.getChildren().get(0).getHeight());
            } else {
                box.setWidth(availableWidth);
                box.setHeight(availableHeight);
            }
        }
    }

    // --- ButtonNode ---

    private void layoutButton(ButtonNode btn, int availableWidth) {
        // Rendered as "[label]" — 2 brackets + label
        int desired = btn.getLabel().length() + 2;
        btn.setWidth(Math.min(desired, availableWidth));
        btn.setHeight(1);
    }

    // --- CheckboxNode ---

    private void layoutCheckbox(CheckboxNode cb, int availableWidth) {
        // Rendered as "[✓] label" or "[ ] label" — 4 chars prefix + label length
        int desired = "[✓] ".length() + cb.getLabel().length();
        cb.setWidth(Math.min(desired, availableWidth));
        cb.setHeight(1);
    }

    // --- HelpPanelNode ---

    private void layoutHelpPanel(HelpPanelNode help, int availableWidth) {
        int maxLen = help.maxLineLength();
        help.setWidth(Math.min(maxLen, availableWidth));
        help.setHeight(Math.max(1, help.getBindings().size()));
    }

    // --- DividerNode ---

    private void layoutDivider(DividerNode div, int availableWidth, int availableHeight) {
        if (div.getOrientation() == DividerNode.Orientation.HORIZONTAL) {
            div.setWidth(availableWidth);
            div.setHeight(1);
        } else {
            div.setWidth(1);
            div.setHeight(availableHeight);
        }
    }
}
