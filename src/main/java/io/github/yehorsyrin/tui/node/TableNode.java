package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.style.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A scrollable data table with header row, optional borders, and row selection.
 *
 * <pre>
 * ┌────────────┬──────────┬──────────┐
 * │ Name       │ Age      │ City     │
 * ├────────────┼──────────┼──────────┤
 * │ Alice      │ 30       │ NYC      │
 * │ Bob        │ 25       │ LA       │  ← selected
 * └────────────┴──────────┴──────────┘
 * </pre>
 *
 * @author Jarvis (AI)
 */
public class TableNode extends Node {

    // --- Box-drawing characters ---
    public static final char H    = '─';
    public static final char V    = '│';
    public static final char TL   = '┌';
    public static final char TR   = '┐';
    public static final char BL   = '└';
    public static final char BR   = '┘';
    public static final char TT   = '┬';  // top T-junction
    public static final char BT   = '┴';  // bottom T-junction
    public static final char LT   = '├';  // left T-junction
    public static final char RT   = '┤';  // right T-junction
    public static final char PLUS = '┼';  // cross

    private final List<String>       headers;
    private final List<List<String>> rows;
    private final int                maxHeight; // visible data rows

    private int     selectedRow  = 0;
    private int     scrollOffset = 0;
    private boolean showBorders  = true;

    // Column widths computed by the layout engine (pixels = terminal columns)
    private int[] colWidths;

    // Styles
    private Style headerStyle   = Style.DEFAULT.withBold(true);
    private Style rowStyle      = Style.DEFAULT;
    private Style selectedStyle = Style.DEFAULT.withBold(true);
    private Style borderStyle   = Style.DEFAULT;

    /**
     * Creates a table node.
     *
     * @param headers   column header labels; must not be null or empty
     * @param rows      data rows; each row should have the same number of cells as headers
     * @param maxHeight maximum number of visible data rows (excluding header and borders)
     */
    public TableNode(List<String> headers, List<List<String>> rows, int maxHeight) {
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("TableNode requires at least one header");
        }
        this.headers   = List.copyOf(headers);
        this.rows      = rows != null ? copyRows(rows) : List.of();
        this.maxHeight = Math.max(1, maxHeight);
        this.selectedRow = this.rows.isEmpty() ? -1 : 0;
    }

    private static List<List<String>> copyRows(List<List<String>> rows) {
        List<List<String>> copy = new ArrayList<>(rows.size());
        for (List<String> row : rows) copy.add(List.copyOf(row != null ? row : List.of()));
        return Collections.unmodifiableList(copy);
    }

    // --- Accessors ---

    public List<String>       getHeaders()      { return headers; }
    public List<List<String>> getRows()         { return rows; }
    public int                getMaxHeight()    { return maxHeight; }
    public int                getSelectedRow()  { return selectedRow; }
    public int                getScrollOffset() { return scrollOffset; }
    public boolean            isShowBorders()   { return showBorders; }
    public int[]              getColWidths()    { return colWidths; }

    /** Returns the number of columns. */
    public int columnCount() { return headers.size(); }

    /** Returns the number of data rows. */
    public int rowCount() { return rows.size(); }

    /** Returns the number of visible data rows. */
    public int visibleRowCount() { return Math.min(maxHeight, rows.size()); }

    /** Returns the number of rows that can be scrolled past (0 when all rows fit). */
    public int scrollableRows() { return Math.max(0, rows.size() - maxHeight); }

    /** Returns {@code true} if the table has more rows than the visible area. */
    public boolean isScrollable() { return scrollableRows() > 0; }

    // --- Column widths ---

    /** Called by the layout engine after computing column widths. */
    public void setColWidths(int[] widths) { this.colWidths = widths; }

    /**
     * Computes column widths to fill {@code availableWidth}.
     * Each column gets at least {@code minColWidth} characters and expands proportionally.
     *
     * @param availableWidth available terminal columns
     * @return column widths (content area, excluding border separators)
     */
    public int[] computeColWidths(int availableWidth) {
        int numCols = headers.size();
        // Border separators: numCols + 1 for left/right/inter-column borders
        int separators = showBorders ? (numCols + 1) : (numCols - 1);
        int available  = Math.max(numCols, availableWidth - separators);

        // Natural width = max(header.len, max cell len) + 2 for padding
        int[] natural = new int[numCols];
        for (int col = 0; col < numCols; col++) {
            int maxLen = headers.get(col).length();
            for (List<String> row : rows) {
                if (col < row.size()) maxLen = Math.max(maxLen, row.get(col).length());
            }
            natural[col] = maxLen + 2; // 1 space padding each side
        }

        int totalNatural = 0;
        for (int w : natural) totalNatural += w;

        if (totalNatural <= available) {
            // Distribute remaining space equally to the last column
            int extra = available - totalNatural;
            natural[numCols - 1] += extra;
            return natural;
        }

        // Scale down proportionally; each column minimum = 3 (space + 1 char + space)
        int[] scaled = new int[numCols];
        int assigned = 0;
        for (int col = 0; col < numCols - 1; col++) {
            scaled[col] = Math.max(3, (int) ((double) natural[col] / totalNatural * available));
            assigned += scaled[col];
        }
        scaled[numCols - 1] = Math.max(3, available - assigned);
        return scaled;
    }

    // --- Navigation ---

    /** Selects the previous row (with scroll). */
    public void selectUp() {
        if (rows.isEmpty()) return;
        if (selectedRow > 0) {
            selectedRow--;
            if (selectedRow < scrollOffset) scrollOffset = selectedRow;
        }
    }

    /** Selects the next row (with scroll). */
    public void selectDown() {
        if (rows.isEmpty()) return;
        if (selectedRow < rows.size() - 1) {
            selectedRow++;
            if (selectedRow >= scrollOffset + maxHeight) scrollOffset = selectedRow - maxHeight + 1;
        }
    }

    /** Returns the currently selected row's data, or an empty list if nothing is selected. */
    public List<String> getSelectedRowData() {
        if (selectedRow < 0 || selectedRow >= rows.size()) return List.of();
        return rows.get(selectedRow);
    }

    // --- Configuration ---

    public TableNode showBorders(boolean show) { this.showBorders = show; return this; }

    // --- Styles ---

    public Style getHeaderStyle()   { return headerStyle; }
    public Style getRowStyle()      { return rowStyle; }
    public Style getSelectedStyle() { return selectedStyle; }
    public Style getBorderStyle()   { return borderStyle; }

    public TableNode headerStyle(Style s)   { this.headerStyle   = s != null ? s : Style.DEFAULT; return this; }
    public TableNode rowStyle(Style s)      { this.rowStyle      = s != null ? s : Style.DEFAULT; return this; }
    public TableNode selectedStyle(Style s) { this.selectedStyle = s != null ? s : Style.DEFAULT; return this; }
    public TableNode borderStyle(Style s)   { this.borderStyle   = s != null ? s : Style.DEFAULT; return this; }
}
