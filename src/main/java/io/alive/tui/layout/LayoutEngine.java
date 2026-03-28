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
        } else if (root instanceof VirtualListNode vlist) {
            root.setWidth(availableWidth);
            root.setHeight(Math.min(vlist.getMaxVisibleRows(), Math.max(1, vlist.visibleRowCount())));
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
        } else if (root instanceof RadioGroupNode rg) {
            layoutRadioGroup(rg, availableWidth);
        } else if (root instanceof HelpPanelNode help) {
            layoutHelpPanel(help, availableWidth);
        } else if (root instanceof DialogNode dialog) {
            layoutDialog(dialog, x, y, availableWidth, availableHeight);
        } else if (root instanceof ViewportNode vp) {
            layoutViewport(vp, x, y, availableWidth, availableHeight);
        } else if (root instanceof SelectNode sel) {
            layoutSelect(sel, availableWidth);
        } else if (root instanceof CollapsibleNode col) {
            layoutCollapsible(col, x, y, availableWidth, availableHeight);
        } else if (root instanceof io.alive.tui.node.TextAreaNode ta) {
            layoutTextArea(ta, availableWidth);
        } else if (root instanceof NotificationNode notif) {
            notif.setWidth(Math.min(notif.renderText().length(), availableWidth));
            notif.setHeight(1);
        } else if (root instanceof TableNode table) {
            layoutTable(table, availableWidth);
        } else {
            // Unknown node type — fill available space
            root.setWidth(availableWidth);
            root.setHeight(availableHeight);
        }
    }

    // --- TextNode ---

    private void layoutText(TextNode node, int availableWidth) {
        String text = node.getText();
        boolean needsWrap = node.isWrap() || text.contains("\n");
        if (needsWrap && availableWidth > 0) {
            java.util.List<String> lines = wordWrap(text, availableWidth);
            node.setWrappedLines(lines);
            node.setWidth(availableWidth);
            node.setHeight(Math.max(1, lines.size()));
        } else {
            node.setWrappedLines(null);
            node.setWidth(Math.min(text.length(), availableWidth));
            node.setHeight(1);
        }
    }

    /**
     * Splits {@code text} into lines that fit within {@code width} columns.
     * Newline characters force a line break. Long words are hard-broken.
     */
    static java.util.List<String> wordWrap(String text, int width) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (width <= 0) return result;
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                result.add("");
                continue;
            }
            String[] words = paragraph.split(" ", -1);
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                // Hard-break words that exceed the available width
                while (word.length() > width) {
                    int space = width - line.length();
                    if (line.length() > 0) {
                        result.add(line.toString());
                        line.setLength(0);
                        space = width;
                    }
                    result.add(word.substring(0, space));
                    word = word.substring(space);
                }
                if (line.isEmpty()) {
                    line.append(word);
                } else if (line.length() + 1 + word.length() <= width) {
                    line.append(' ').append(word);
                } else {
                    result.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
            }
            if (!line.isEmpty()) result.add(line.toString());
        }
        return result;
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

    // --- RadioGroupNode ---

    private void layoutRadioGroup(RadioGroupNode rg, int availableWidth) {
        rg.setWidth(availableWidth);
        rg.setHeight(Math.max(1, rg.getOptions().size()));
    }

    // --- CollapsibleNode ---

    private void layoutCollapsible(CollapsibleNode col, int x, int y,
                                   int availableWidth, int availableHeight) {
        col.setWidth(availableWidth);
        if (!col.isExpanded()) {
            col.setHeight(1);
            return;
        }
        // Expanded: title row + lay out children stacked below
        int currentY    = y + 1;
        int totalHeight = 1;
        for (int i = 0; i < col.getChildren().size(); i++) {
            Node child = col.getChildren().get(i);
            int remaining = Math.max(0, availableHeight - totalHeight);
            layout(child, x, currentY, availableWidth, remaining);
            currentY    += child.getHeight();
            totalHeight += child.getHeight();
        }
        col.setHeight(totalHeight);
    }

    // --- TableNode ---

    private void layoutTable(TableNode table, int availableWidth) {
        int[] colWidths = table.computeColWidths(availableWidth);
        table.setColWidths(colWidths);
        table.setWidth(availableWidth);

        // Height = borders + header + header-separator + visible rows + bottom border
        int visibleRows = table.visibleRowCount();
        int height = table.isShowBorders()
                ? 4 + visibleRows  // top + header + sep + rows + bottom
                : 1 + visibleRows; // header + rows
        table.setHeight(height);
    }

    // --- SelectNode ---

    private void layoutSelect(SelectNode sel, int availableWidth) {
        sel.setWidth(availableWidth);
        // Closed: 1 row (the header). Open: 1 header + n options.
        sel.setHeight(sel.isOpen() ? 1 + sel.getOptions().size() : 1);
    }

    // --- ViewportNode ---

    private void layoutViewport(ViewportNode vp, int x, int y,
                                int availableWidth, int availableHeight) {
        boolean hasSb = vp.isShowScrollbar();
        int contentW = hasSb ? Math.max(0, availableWidth - 1) : availableWidth;

        if (!vp.getChildren().isEmpty()) {
            // Lay out content with effectively unlimited height so it shows its natural size
            layout(vp.getChildren().get(0), x, y, contentW, Integer.MAX_VALUE / 2);
            int contentH = vp.getChildren().get(0).getHeight();
            vp.setContentHeight(contentH);
            vp.setWidth(availableWidth);
            vp.setHeight(Math.min(vp.getMaxHeight(), contentH));
        } else {
            vp.setContentHeight(0);
            vp.setWidth(availableWidth);
            vp.setHeight(Math.min(vp.getMaxHeight(), availableHeight));
        }
    }

    // --- DialogNode ---

    private void layoutDialog(DialogNode dialog, int x, int y, int availableWidth, int availableHeight) {
        int innerW = Math.max(0, availableWidth  - 2);
        int innerH = Math.max(0, availableHeight - 2);
        if (!dialog.getChildren().isEmpty()) {
            layout(dialog.getChildren().get(0), x + 1, y + 1, innerW, innerH);
            dialog.setWidth(availableWidth);
            dialog.setHeight(dialog.getChildren().get(0).getHeight() + 2);
        } else {
            dialog.setWidth(availableWidth);
            dialog.setHeight(availableHeight);
        }
    }

    // --- HelpPanelNode ---

    private void layoutHelpPanel(HelpPanelNode help, int availableWidth) {
        int maxLen = help.maxLineLength();
        help.setWidth(Math.min(maxLen, availableWidth));
        help.setHeight(Math.max(1, help.getBindings().size()));
    }

    // --- TextAreaNode ---

    private void layoutTextArea(io.alive.tui.node.TextAreaNode ta, int availableWidth) {
        ta.setWidth(availableWidth);
        ta.setHeight(Math.min(ta.getMaxHeight(), Math.max(1, ta.getLines().size())));
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
