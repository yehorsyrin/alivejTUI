package io.alive.tui.diff;

import io.alive.tui.core.Node;
import io.alive.tui.style.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes the minimal set of cell changes between two virtual tree snapshots.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Flatten both trees into {@code Map<"col,row", CellState>}.</li>
 *   <li>For every cell in the new tree that differs from the old tree → emit a change.</li>
 *   <li>For every cell in the old tree not present in the new tree → emit a clear (space).</li>
 * </ol>
 *
 * @author Jarvis (AI)
 */
public class Differ {

    private final TreeFlattener flattener = new TreeFlattener();

    /**
     * Returns the minimal list of cell changes to transform {@code oldTree} into {@code newTree}.
     *
     * @param oldTree previous frame (may be {@code null} on the first render)
     * @param newTree current frame
     * @return ordered list of cell changes (empty if no visual difference)
     */
    public List<CellChange> diff(Node oldTree, Node newTree) {
        Map<String, CellState> oldCells = flattener.flatten(oldTree);
        Map<String, CellState> newCells = flattener.flatten(newTree);

        List<CellChange> changes = new ArrayList<>();

        // Cells added or changed
        for (Map.Entry<String, CellState> entry : newCells.entrySet()) {
            String key = entry.getKey();
            CellState newState = entry.getValue();
            CellState oldState = oldCells.get(key);

            if (!newState.equals(oldState)) {
                int[] pos = parseKey(key);
                changes.add(new CellChange(pos[0], pos[1], newState.character(), newState.style()));
            }
        }

        // Cells removed (were in old, not in new) → erase with space
        for (String key : oldCells.keySet()) {
            if (!newCells.containsKey(key)) {
                int[] pos = parseKey(key);
                changes.add(new CellChange(pos[0], pos[1], ' ', Style.DEFAULT));
            }
        }

        return changes;
    }

    private static int[] parseKey(String key) {
        int comma = key.indexOf(',');
        return new int[]{
            Integer.parseInt(key, 0, comma, 10),
            Integer.parseInt(key, comma + 1, key.length(), 10)
        };
    }
}
