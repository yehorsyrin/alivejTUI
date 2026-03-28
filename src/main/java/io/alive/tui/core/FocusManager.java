package io.alive.tui.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages keyboard focus across {@link Focusable} nodes.
 *
 * <p>Maintains an ordered list of focusable nodes in registration order.
 * Exactly one node is focused at a time; {@link #focusNext()} and {@link #focusPrev()}
 * cycle through the list. Focus state is set automatically via {@link Focusable#setFocused(boolean)}.
 *
 * <p>Usage:
 * <pre>{@code
 * // In a Component, during mount or render:
 * registerFocusable(myButton);
 * registerFocusable(myInput);
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public class FocusManager {

    private final List<Focusable> focusables = new ArrayList<>();
    private int focusedIndex = -1;

    /**
     * Register a focusable node. Duplicate registrations (same reference) are ignored.
     *
     * @param node the node to register; {@code null} is ignored
     */
    public void register(Focusable node) {
        if (node == null) return;
        if (!focusables.contains(node)) {
            focusables.add(node);
        }
    }

    /**
     * Unregister a focusable node. If it was focused, focus is cleared.
     *
     * @param node the node to remove; {@code null} is ignored
     */
    public void unregister(Focusable node) {
        if (node == null) return;
        int idx = focusables.indexOf(node);
        if (idx < 0) return;
        if (idx == focusedIndex) {
            node.setFocused(false);
            focusedIndex = -1;
        } else if (idx < focusedIndex) {
            focusedIndex--;
        }
        focusables.remove(idx);
    }

    /**
     * Remove all registrations and clear focus state.
     */
    public void clear() {
        defocusAll();
        focusables.clear();
        focusedIndex = -1;
    }

    /**
     * Move focus to the next registered node (wraps around).
     * Does nothing if no nodes are registered.
     */
    public void focusNext() {
        if (focusables.isEmpty()) return;
        defocusCurrent();
        focusedIndex = (focusedIndex + 1) % focusables.size();
        focusables.get(focusedIndex).setFocused(true);
    }

    /**
     * Move focus to the previous registered node (wraps around).
     * Does nothing if no nodes are registered.
     */
    public void focusPrev() {
        if (focusables.isEmpty()) return;
        defocusCurrent();
        if (focusedIndex < 0) {
            focusedIndex = focusables.size() - 1;
        } else {
            focusedIndex = (focusedIndex - 1 + focusables.size()) % focusables.size();
        }
        focusables.get(focusedIndex).setFocused(true);
    }

    /**
     * Move focus to the node with the given focus ID.
     * Does nothing if no node has the given ID.
     *
     * @param id the {@link Focusable#getFocusId()} to search for; {@code null} is ignored
     */
    public void focusById(String id) {
        if (id == null) return;
        for (int i = 0; i < focusables.size(); i++) {
            if (id.equals(focusables.get(i).getFocusId())) {
                defocusCurrent();
                focusedIndex = i;
                focusables.get(focusedIndex).setFocused(true);
                return;
            }
        }
    }

    /**
     * Returns the currently focused node, or {@code null} if nothing is focused.
     */
    public Focusable getFocused() {
        if (focusedIndex < 0 || focusedIndex >= focusables.size()) return null;
        return focusables.get(focusedIndex);
    }

    /**
     * Returns the number of registered focusable nodes.
     */
    public int size() {
        return focusables.size();
    }

    // --- Internal helpers ---

    private void defocusCurrent() {
        if (focusedIndex >= 0 && focusedIndex < focusables.size()) {
            focusables.get(focusedIndex).setFocused(false);
        }
    }

    private void defocusAll() {
        for (Focusable f : focusables) f.setFocused(false);
    }
}
