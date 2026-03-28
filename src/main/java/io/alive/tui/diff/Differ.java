package io.alive.tui.diff;

import io.alive.tui.core.Node;
import io.alive.tui.style.Style;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return diff(oldTree, null, newTree, null);
    }

    /**
     * Diff with optional overlay layers. Overlay cells are rendered on top of base cells.
     * Pass {@code null} for overlay parameters when no overlay is active.
     *
     * @param oldBase    previous base frame
     * @param oldOverlay previous overlay frame (may be {@code null})
     * @param newBase    current base frame
     * @param newOverlay current overlay frame (may be {@code null})
     * @return ordered list of cell changes
     */
    public List<CellChange> diff(Node oldBase, Node oldOverlay, Node newBase, Node newOverlay) {
        Map<String, CellState> oldCells = merged(flattener.flatten(oldBase), flattener.flatten(oldOverlay));
        Map<String, CellState> newCells = merged(flattener.flatten(newBase), flattener.flatten(newOverlay));
        return diffCells(oldCells, newCells);
    }

    /**
     * Flattens and merges a base tree with an optional overlay into a single cell map.
     * The returned map is a snapshot and can be stored safely across renders.
     *
     * @param base    the base tree (may be {@code null})
     * @param overlay the overlay tree (may be {@code null})
     * @return merged cell map (overlay cells override base cells)
     */
    public Map<String, CellState> flattenMerged(Node base, Node overlay) {
        return merged(flattener.flatten(base), flattener.flatten(overlay));
    }

    /** Merges base and overlay cell maps; overlay wins on collision. */
    private static Map<String, CellState> merged(Map<String, CellState> base,
                                                  Map<String, CellState> overlay) {
        if (overlay.isEmpty()) return base;
        Map<String, CellState> result = new HashMap<>(base);
        result.putAll(overlay);
        return result;
    }

    /**
     * Returns the minimal list of cell changes between two pre-flattened cell maps.
     * Use this overload when the caller manages cell map snapshots to avoid re-flattening
     * mutable node trees.
     *
     * @param oldCells the previous frame's cell map (from {@link #flattenMerged})
     * @param newCells the current frame's cell map (from {@link #flattenMerged})
     * @return ordered list of cell changes
     */
    public List<CellChange> diffCells(Map<String, CellState> oldCells,
                                      Map<String, CellState> newCells) {
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
