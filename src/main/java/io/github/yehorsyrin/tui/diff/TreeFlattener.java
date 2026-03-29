package io.github.yehorsyrin.tui.diff;

import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.node.*;
import io.github.yehorsyrin.tui.style.Style;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a virtual tree into a flat map of terminal cell positions to their visual state.
 * Used by the {@link Differ} to find which cells changed between two frames.
 *
 * @author Jarvis (AI)
 */
class TreeFlattener {

    private static final char SPACE = ' ';

    // Box-drawing characters
    private static final char H_LINE   = '─';
    private static final char V_LINE   = '│';
    private static final char TOP_LEFT = '╭';
    private static final char TOP_RIGHT= '╮';
    private static final char BOT_LEFT = '╰';
    private static final char BOT_RIGHT= '╯';

    /**
     * Flattens the given node tree into a cell map.
     * Key = "col,row", Value = {@link CellState}.
     */
    Map<String, CellState> flatten(Node root) {
        Map<String, CellState> cells = new HashMap<>();
        if (root != null) visit(root, cells);
        return cells;
    }

    private void visit(Node node, Map<String, CellState> cells) {
        if (node instanceof TextNode text) {
            flattenText(text, cells);
        } else if (node instanceof ButtonNode btn) {
            flattenButton(btn, cells);
        } else if (node instanceof InputNode input) {
            flattenInput(input, cells);
        } else if (node instanceof ListNode list) {
            flattenList(list, cells);
        } else if (node instanceof ProgressBarNode bar) {
            flattenProgressBar(bar, cells);
        } else if (node instanceof SpinnerNode spinner) {
            flattenSpinner(spinner, cells);
        } else if (node instanceof CheckboxNode cb) {
            flattenCheckbox(cb, cells);
        } else if (node instanceof RadioGroupNode rg) {
            flattenRadioGroup(rg, cells);
        } else if (node instanceof DividerNode div) {
            flattenDivider(div, cells);
        } else if (node instanceof BoxNode box) {
            flattenBox(box, cells);
        } else if (node instanceof ScrollableVBoxNode svbox) {
            flattenScrollableVBox(svbox, cells);
        } else if (node instanceof HelpPanelNode help) {
            flattenHelpPanel(help, cells);
        } else if (node instanceof DialogNode dialog) {
            flattenDialog(dialog, cells);
        } else if (node instanceof ViewportNode vp) {
            flattenViewport(vp, cells);
        } else if (node instanceof SelectNode sel) {
            flattenSelect(sel, cells);
        } else if (node instanceof CollapsibleNode col) {
            flattenCollapsible(col, cells);
        } else if (node instanceof io.github.yehorsyrin.tui.node.TextAreaNode ta) {
            flattenTextArea(ta, cells);
        } else if (node instanceof NotificationNode notif) {
            flattenNotification(notif, cells);
        } else if (node instanceof TableNode table) {
            flattenTable(table, cells);
        } else if (node instanceof io.github.yehorsyrin.tui.node.VirtualListNode vlist) {
            flattenVirtualList(vlist, cells);
        } else {
            // Container node (VBox, HBox) — recurse into children
            for (Node child : node.getChildren()) visit(child, cells);
        }
    }

    // --- Leaf renderers ---

    private void flattenText(TextNode text, Map<String, CellState> cells) {
        int x = text.getX(), y = text.getY(), w = text.getWidth();

        if (text.getWrappedLines() != null) {
            // Multi-line (wrapped) text
            Style style = text.getStyle();
            java.util.List<String> lines = text.getWrappedLines();
            for (int row = 0; row < lines.size(); row++) {
                String line = lines.get(row);
                for (int i = 0; i < line.length() && i < w; i++) {
                    put(cells, x + i, y + row, line.charAt(i), style);
                }
            }
        } else if (text.hasMarkdown()) {
            // Inline markdown — render each styled segment in order, clipped to node width
            int col = 0;
            for (io.github.yehorsyrin.tui.node.StyledSegment seg : text.getSegments()) {
                String segText = seg.text();
                for (int i = 0; i < segText.length() && col < w; i++, col++) {
                    put(cells, x + col, y, segText.charAt(i), seg.style());
                }
                if (col >= w) break;
            }
        } else {
            // Plain single-line text
            String str = text.getText();
            Style style = text.getStyle();
            for (int i = 0; i < str.length() && i < w; i++) {
                put(cells, x + i, y, str.charAt(i), style);
            }
        }
    }

    private void flattenButton(ButtonNode btn, Map<String, CellState> cells) {
        String label = "[" + btn.getLabel() + "]";
        Style style = btn.isFocused() ? btn.getFocusedStyle() : btn.getStyle();
        int x = btn.getX(), y = btn.getY(), w = btn.getWidth();
        for (int i = 0; i < label.length() && i < w; i++) {
            put(cells, x + i, y, label.charAt(i), style);
        }
    }

    private void flattenInput(InputNode input, Map<String, CellState> cells) {
        String value = input.getValue();
        String placeholder = input.getPlaceholder();
        Style style = input.isFocused() ? input.getFocusedStyle() : input.getStyle();
        int x = input.getX(), y = input.getY(), w = input.getWidth();

        String display = value.isEmpty() && !placeholder.isEmpty() ? placeholder : value;
        for (int i = 0; i < w; i++) {
            char c = i < display.length() ? display.charAt(i) : SPACE;
            put(cells, x + i, y, c, style);
        }
    }

    private void flattenList(ListNode list, Map<String, CellState> cells) {
        List<String> items = list.getItems();
        int x = list.getX(), y = list.getY();
        int w = list.getWidth(), h = list.getHeight();
        int offset = list.getScrollOffset();

        for (int row = 0; row < h; row++) {
            int itemIdx = offset + row;
            if (itemIdx >= items.size()) break;
            String item = items.get(itemIdx);
            Style style = itemIdx == list.getSelectedIndex()
                ? list.getSelectedStyle()
                : list.getNormalStyle();
            for (int col = 0; col < w; col++) {
                char c = col < item.length() ? item.charAt(col) : SPACE;
                put(cells, x + col, y + row, c, style);
            }
        }
    }

    private void flattenVirtualList(io.github.yehorsyrin.tui.node.VirtualListNode vlist,
                                    Map<String, CellState> cells) {
        List<String> visible = vlist.getVisibleItems();
        int x = vlist.getX(), y = vlist.getY();
        int w = vlist.getWidth(), h = vlist.getHeight();
        int offset = vlist.getScrollOffset();

        for (int row = 0; row < h && row < visible.size(); row++) {
            int itemIdx = offset + row;
            String item = visible.get(row);
            Style style = itemIdx == vlist.getSelectedIndex()
                    ? vlist.getSelectedStyle()
                    : vlist.getNormalStyle();
            for (int col = 0; col < w; col++) {
                char c = col < item.length() ? item.charAt(col) : SPACE;
                put(cells, x + col, y + row, c, style);
            }
        }
    }

    private void flattenProgressBar(ProgressBarNode bar, Map<String, CellState> cells) {
        int x = bar.getX(), y = bar.getY(), w = bar.getWidth();
        int filled = (int) Math.round(bar.getProgress() * w);
        for (int i = 0; i < w; i++) {
            if (i < filled) {
                put(cells, x + i, y, ProgressBarNode.FILLED_CHAR, bar.getFilledStyle());
            } else {
                put(cells, x + i, y, ProgressBarNode.EMPTY_CHAR, bar.getEmptyStyle());
            }
        }
    }

    private void flattenSpinner(SpinnerNode spinner, Map<String, CellState> cells) {
        String frame = spinner.currentFrame();
        if (!frame.isEmpty()) {
            put(cells, spinner.getX(), spinner.getY(), frame.charAt(0), spinner.getStyle());
        }
    }

    private void flattenCheckbox(CheckboxNode cb, Map<String, CellState> cells) {
        String checkMark = cb.isChecked() ? "✓" : " ";
        String label = "[" + checkMark + "] " + cb.getLabel();
        Style style = cb.isFocused() ? cb.getFocusedStyle() : cb.getStyle();
        int x = cb.getX(), y = cb.getY(), w = cb.getWidth();
        for (int i = 0; i < label.length() && i < w; i++) {
            put(cells, x + i, y, label.charAt(i), style);
        }
    }

    private void flattenRadioGroup(RadioGroupNode rg, Map<String, CellState> cells) {
        java.util.List<String> options = rg.getOptions();
        int x = rg.getX(), y = rg.getY(), w = rg.getWidth();
        int selectedIndex = rg.getSelectedIndex();
        boolean focused = rg.isFocused();

        for (int i = 0; i < options.size(); i++) {
            String prefix = (i == selectedIndex)
                    ? RadioGroupNode.SELECTED_PREFIX
                    : RadioGroupNode.UNSELECTED_PREFIX;
            String row = prefix + options.get(i);
            Style style = (i == selectedIndex && focused)
                    ? rg.getFocusedStyle()
                    : rg.getNormalStyle();
            for (int col = 0; col < w; col++) {
                char c = col < row.length() ? row.charAt(col) : SPACE;
                put(cells, x + col, y + i, c, style);
            }
        }
    }

    private void flattenDivider(DividerNode div, Map<String, CellState> cells) {
        int x = div.getX(), y = div.getY();
        Style style = div.getStyle();
        char ch = div.getCharacter();

        if (div.getOrientation() == DividerNode.Orientation.HORIZONTAL) {
            for (int i = 0; i < div.getWidth(); i++) {
                put(cells, x + i, y, ch, style);
            }
        } else {
            for (int i = 0; i < div.getHeight(); i++) {
                put(cells, x, y + i, ch, style);
            }
        }
    }

    private void flattenBox(BoxNode box, Map<String, CellState> cells) {
        if (box.hasBorder()) {
            int x = box.getX(), y = box.getY();
            int w = box.getWidth(), h = box.getHeight();
            Style bs = box.getBorderStyle();

            // Corners
            put(cells, x,         y,         TOP_LEFT,  bs);
            put(cells, x + w - 1, y,         TOP_RIGHT, bs);
            put(cells, x,         y + h - 1, BOT_LEFT,  bs);
            put(cells, x + w - 1, y + h - 1, BOT_RIGHT, bs);

            // Top and bottom edges
            for (int col = x + 1; col < x + w - 1; col++) {
                put(cells, col, y,         H_LINE, bs);
                put(cells, col, y + h - 1, H_LINE, bs);
            }

            // Left and right edges
            for (int row = y + 1; row < y + h - 1; row++) {
                put(cells, x,         row, V_LINE, bs);
                put(cells, x + w - 1, row, V_LINE, bs);
            }
        }

        // Recurse into children
        for (Node child : box.getChildren()) visit(child, cells);
    }

    private void flattenScrollableVBox(ScrollableVBoxNode svbox, Map<String, CellState> cells) {
        int nodeY      = svbox.getY();
        int offset     = svbox.getScrollOffset();
        int maxH       = svbox.getMaxHeight();
        int minAbsY    = nodeY + offset;          // first absolute row that is visible
        int maxAbsYEx  = minAbsY + maxH;          // first absolute row that is NOT visible

        // Render children into a temporary map, then clip + remap rows
        Map<String, CellState> childCells = new HashMap<>();
        for (Node child : svbox.getChildren()) {
            visit(child, childCells);
        }

        for (Map.Entry<String, CellState> entry : childCells.entrySet()) {
            String key = entry.getKey();
            int comma = key.indexOf(',');
            int col   = Integer.parseInt(key.substring(0, comma));
            int row   = Integer.parseInt(key.substring(comma + 1));
            if (row >= minAbsY && row < maxAbsYEx) {
                int remappedRow = nodeY + (row - minAbsY);
                cells.put(col + "," + remappedRow, entry.getValue());
            }
        }
    }

    private void flattenHelpPanel(HelpPanelNode help, Map<String, CellState> cells) {
        List<KeyBinding> bindings = help.getBindings();
        int x = help.getX(), y = help.getY(), w = help.getWidth();

        for (int row = 0; row < bindings.size(); row++) {
            String line = HelpPanelNode.format(bindings.get(row));
            for (int col = 0; col < w; col++) {
                char c = col < line.length() ? line.charAt(col) : SPACE;
                put(cells, x + col, y + row, c, Style.DEFAULT);
            }
        }
    }

    private void flattenCollapsible(CollapsibleNode col, Map<String, CellState> cells) {
        int x = col.getX(), y = col.getY(), w = col.getWidth();
        Style style = col.isFocused() ? col.getFocusedStyle() : col.getTitleStyle();

        // Title row: "▶ Title" or "▼ Title"
        char arrow = col.isExpanded() ? CollapsibleNode.EXPANDED_ARROW : CollapsibleNode.COLLAPSED_ARROW;
        String header = arrow + CollapsibleNode.ARROW_PADDING + col.getTitle();
        for (int i = 0; i < w; i++) {
            char c = i < header.length() ? header.charAt(i) : SPACE;
            put(cells, x + i, y, c, style);
        }

        // Children (only when expanded)
        if (col.isExpanded()) {
            for (Node child : col.getChildren()) visit(child, cells);
        }
    }

    private void flattenSelect(SelectNode sel, Map<String, CellState> cells) {
        int x = sel.getX(), y = sel.getY(), w = sel.getWidth();
        Style headerStyle = sel.isFocused() ? sel.getFocusedStyle() : sel.getNormalStyle();
        char arrow = sel.isOpen() ? SelectNode.ARROW_UP : SelectNode.ARROW_DOWN;

        // Header row: "[ Selected Option ▾ ]" or similar
        // Format: "[ " + label + " " + arrow + " ]"  — simplified to fitting within w
        String label = sel.getSelectedOption();
        String header = "[ " + label + " " + arrow + " ]";
        for (int col = 0; col < w; col++) {
            char c = col < header.length() ? header.charAt(col) : SPACE;
            put(cells, x + col, y, c, headerStyle);
        }

        // Option rows (only when open)
        if (sel.isOpen()) {
            List<String> options = sel.getOptions();
            for (int i = 0; i < options.size(); i++) {
                String prefix = (i == sel.getHoverIndex()) ? SelectNode.CURSOR : SelectNode.PADDING;
                String row = prefix + options.get(i);
                Style style = (i == sel.getHoverIndex()) ? sel.getHoverStyle() : sel.getNormalStyle();
                for (int col = 0; col < w; col++) {
                    char c = col < row.length() ? row.charAt(col) : SPACE;
                    put(cells, x + col, y + 1 + i, c, style);
                }
            }
        }
    }

    private void flattenViewport(ViewportNode vp, Map<String, CellState> cells) {
        int nodeX     = vp.getX();
        int nodeY     = vp.getY();
        int offset    = vp.getScrollOffset();
        int visH      = vp.getHeight();
        boolean hasSb = vp.isShowScrollbar() && vp.isScrollable();
        int contentW  = hasSb ? vp.getWidth() - 1 : vp.getWidth();

        int minAbsY  = nodeY + offset;
        int maxAbsYEx = minAbsY + visH;

        // Render children into a temp map, then clip + remap rows (and clip cols)
        Map<String, CellState> childCells = new HashMap<>();
        for (Node child : vp.getChildren()) visit(child, childCells);

        for (Map.Entry<String, CellState> entry : childCells.entrySet()) {
            String key = entry.getKey();
            int comma = key.indexOf(',');
            int col   = Integer.parseInt(key.substring(0, comma));
            int row   = Integer.parseInt(key.substring(comma + 1));
            if (row >= minAbsY && row < maxAbsYEx && col < nodeX + contentW) {
                int remappedRow = nodeY + (row - minAbsY);
                cells.put(col + "," + remappedRow, entry.getValue());
            }
        }

        // Draw scroll bar when content is scrollable
        if (hasSb) {
            int sbX       = nodeX + vp.getWidth() - 1;
            int trackH    = visH;
            int contentH  = vp.getContentHeight();
            int thumbSize = Math.max(1, (int) Math.round((double) visH / contentH * trackH));
            int maxThumbStart = trackH - thumbSize;
            int thumbStart = vp.scrollableRows() == 0 ? 0
                    : (int) Math.round((double) vp.getScrollOffset() / vp.scrollableRows() * maxThumbStart);

            for (int row = 0; row < trackH; row++) {
                char ch = (row >= thumbStart && row < thumbStart + thumbSize)
                        ? ViewportNode.THUMB_CHAR : ViewportNode.TRACK_CHAR;
                put(cells, sbX, nodeY + row, ch, Style.DEFAULT);
            }
        }
    }

    private void flattenDialog(DialogNode dialog, Map<String, CellState> cells) {
        int x = dialog.getX(), y = dialog.getY();
        int w = dialog.getWidth(), h = dialog.getHeight();
        Style bs = dialog.getBorderStyle();

        // Corners
        put(cells, x,         y,         TOP_LEFT,  bs);
        put(cells, x + w - 1, y,         TOP_RIGHT, bs);
        put(cells, x,         y + h - 1, BOT_LEFT,  bs);
        put(cells, x + w - 1, y + h - 1, BOT_RIGHT, bs);

        // Top border — with optional title: ╭─ Title ──╮
        if (dialog.hasTitle()) {
            String titlePart = "\u2500 " + dialog.getTitle() + " ";
            int col = x + 1;
            for (int i = 0; i < titlePart.length() && col < x + w - 1; i++, col++) {
                put(cells, col, y, titlePart.charAt(i), bs);
            }
            for (; col < x + w - 1; col++) {
                put(cells, col, y, H_LINE, bs);
            }
        } else {
            for (int col = x + 1; col < x + w - 1; col++) {
                put(cells, col, y, H_LINE, bs);
            }
        }

        // Bottom border
        for (int col = x + 1; col < x + w - 1; col++) {
            put(cells, col, y + h - 1, H_LINE, bs);
        }

        // Left and right edges
        for (int row = y + 1; row < y + h - 1; row++) {
            put(cells, x,         row, V_LINE, bs);
            put(cells, x + w - 1, row, V_LINE, bs);
        }

        // Recurse into children
        for (Node child : dialog.getChildren()) visit(child, cells);
    }

    private void flattenTextArea(io.github.yehorsyrin.tui.node.TextAreaNode ta, Map<String, CellState> cells) {
        int baseX     = ta.getX();
        int baseY     = ta.getY();
        int w         = ta.getWidth();
        int maxH      = ta.getMaxHeight();
        int scrollRow = ta.getScrollRow();
        int cursorRow = ta.getCursorRow();
        int cursorCol = ta.getCursorCol();
        boolean focused = ta.isFocused();
        Style normalStyle = focused ? ta.getFocusedStyle() : ta.getStyle();
        Style cursorStyle = ta.getCursorStyle();
        java.util.List<String> lines = ta.getLines();

        int visibleRows = Math.min(maxH, lines.size());
        for (int visRow = 0; visRow < visibleRows; visRow++) {
            int lineIdx = scrollRow + visRow;
            if (lineIdx >= lines.size()) break;
            String line = lines.get(lineIdx);
            for (int col = 0; col < w; col++) {
                char c = col < line.length() ? line.charAt(col) : SPACE;
                boolean isCursorCell = focused && lineIdx == cursorRow && col == cursorCol;
                Style cellStyle = isCursorCell ? cursorStyle : normalStyle;
                put(cells, baseX + col, baseY + visRow, c, cellStyle);
            }
        }
    }

    private void flattenTable(TableNode table, Map<String, CellState> cells) {
        int x = table.getX(), y = table.getY(), w = table.getWidth();
        int[] cw       = table.getColWidths();
        int   numCols  = table.columnCount();
        boolean borders = table.isShowBorders();
        Style bs       = table.getBorderStyle();

        if (cw == null || cw.length == 0) return;

        int row = y;

        if (borders) {
            // Top border: ┌───┬───┐
            putTableHLine(cells, x, row, w, cw, TableNode.TL, TableNode.TT, TableNode.TR, bs);
            row++;
        }

        // Header row
        putTableDataRow(cells, x, row, cw, table.getHeaders(), borders, table.getHeaderStyle(), bs, w);
        row++;

        if (borders) {
            // Header separator: ├───┼───┤
            putTableHLine(cells, x, row, w, cw, TableNode.LT, TableNode.PLUS, TableNode.RT, bs);
            row++;
        }

        // Data rows
        int offset = table.getScrollOffset();
        int visibleRows = table.visibleRowCount();
        for (int i = 0; i < visibleRows; i++) {
            int dataIdx = offset + i;
            if (dataIdx >= table.getRows().size()) break;
            List<String> rowData = table.getRows().get(dataIdx);
            Style rowStyle = (dataIdx == table.getSelectedRow())
                    ? table.getSelectedStyle() : table.getRowStyle();
            putTableDataRow(cells, x, row, cw, rowData, borders, rowStyle, bs, w);
            row++;
        }

        if (borders) {
            // Bottom border: └───┴───┘
            putTableHLine(cells, x, row, w, cw, TableNode.BL, TableNode.BT, TableNode.BR, bs);
        }
    }

    /**
     * Renders a horizontal separator line for the table.
     * Format: {@code left + (H * colWidth) + mid + (H * colWidth) + ... + right}
     */
    private void putTableHLine(Map<String, CellState> cells, int x, int y, int totalW,
                               int[] cw, char left, char mid, char right, Style style) {
        put(cells, x, y, left, style);
        int col = x + 1;
        for (int c = 0; c < cw.length; c++) {
            for (int k = 0; k < cw[c]; k++) put(cells, col++, y, TableNode.H, style);
            if (c < cw.length - 1) put(cells, col++, y, mid, style);
        }
        put(cells, col, y, right, style);
    }

    /**
     * Renders a single data row (header or body row).
     * Format: {@code │ cell1 │ cell2 │ ... │} (with borders) or {@code cell1 cell2 ...} (no borders)
     */
    private void putTableDataRow(Map<String, CellState> cells, int x, int y,
                                 int[] cw, List<String> data, boolean borders,
                                 Style rowStyle, Style borderStyle, int totalW) {
        int col = x;
        for (int c = 0; c < cw.length; c++) {
            if (borders) put(cells, col++, y, TableNode.V, borderStyle);
            String cell  = (c < data.size()) ? data.get(c) : "";
            // Pad cell to colWidth: " text... "
            put(cells, col++, y, SPACE, rowStyle);  // leading space
            int usable = cw[c] - 2;                 // -2 for the two padding spaces
            for (int k = 0; k < usable; k++) {
                char ch = k < cell.length() ? cell.charAt(k) : SPACE;
                put(cells, col++, y, ch, rowStyle);
            }
            put(cells, col++, y, SPACE, rowStyle);  // trailing space
        }
        if (borders) put(cells, col, y, TableNode.V, borderStyle);
    }

    private void flattenNotification(NotificationNode notif, Map<String, CellState> cells) {
        String text  = notif.renderText();
        Style  style = notif.getStyle();
        int x = notif.getX(), y = notif.getY(), w = notif.getWidth();
        for (int i = 0; i < w; i++) {
            char c = i < text.length() ? text.charAt(i) : SPACE;
            put(cells, x + i, y, c, style);
        }
    }

    private static void put(Map<String, CellState> cells, int col, int row, char ch, Style style) {
        cells.put(col + "," + row, new CellState(ch, style != null ? style : Style.DEFAULT));
    }
}
