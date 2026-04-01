package io.github.yehorsyrin.tui.node;

import java.util.List;

/**
 * Factory for {@link TableNode}.
 *
 * <pre>{@code
 * TableNode table = Table.of(
 *     List.of("Name", "Age", "City"),
 *     List.of(
 *         List.of("Alice", "30", "NYC"),
 *         List.of("Bob",   "25", "LA")
 *     ),
 *     5   // max visible rows
 * );
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Table {

    private Table() {}

    /**
     * Creates a table with the given headers, rows, and visible row limit.
     *
     * @param headers   column header labels
     * @param rows      data rows
     * @param maxHeight maximum number of visible data rows
     */
    public static TableNode of(List<String> headers, List<List<String>> rows, int maxHeight) {
        return new TableNode(headers, rows, maxHeight);
    }
}
