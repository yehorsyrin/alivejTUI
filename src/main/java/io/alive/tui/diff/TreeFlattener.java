package io.alive.tui.diff;

import io.alive.tui.core.Node;
import io.alive.tui.node.*;
import io.alive.tui.style.Style;

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
        } else if (node instanceof DividerNode div) {
            flattenDivider(div, cells);
        } else if (node instanceof BoxNode box) {
            flattenBox(box, cells);
        } else {
            // Container node (VBox, HBox) — recurse into children
            for (Node child : node.getChildren()) visit(child, cells);
        }
    }

    // --- Leaf renderers ---

    private void flattenText(TextNode text, Map<String, CellState> cells) {
        String str = text.getText();
        Style style = text.getStyle();
        int x = text.getX(), y = text.getY(), w = text.getWidth();
        for (int i = 0; i < str.length() && i < w; i++) {
            put(cells, x + i, y, str.charAt(i), style);
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
        Style style = Style.DEFAULT;
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

    private static void put(Map<String, CellState> cells, int col, int row, char ch, Style style) {
        cells.put(col + "," + row, new CellState(ch, style != null ? style : Style.DEFAULT));
    }
}
