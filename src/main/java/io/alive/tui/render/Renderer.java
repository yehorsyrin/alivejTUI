package io.alive.tui.render;

import io.alive.tui.backend.TerminalBackend;
import io.alive.tui.core.Node;
import io.alive.tui.diff.CellChange;
import io.alive.tui.diff.Differ;
import io.alive.tui.layout.LayoutEngine;
import io.alive.tui.node.InputNode;

import java.util.List;

/**
 * Drives the render pipeline: layout → diff → terminal output.
 *
 * <p>Each call to {@link #render(Node)} only flushes cells that actually changed,
 * minimising terminal I/O. {@link #forceFullRender(Node)} discards the previous
 * frame (e.g. after a terminal resize) and redraws everything.
 *
 * @author Jarvis (AI)
 */
public class Renderer {

    private final TerminalBackend backend;
    private final Differ differ;
    private final LayoutEngine layoutEngine;
    private Node previousTree;
    private Node previousOverlayTree;
    private Node overlayTree;

    public Renderer(TerminalBackend backend) {
        if (backend == null) throw new IllegalArgumentException("backend must not be null");
        this.backend      = backend;
        this.differ       = new Differ();
        this.layoutEngine = new LayoutEngine();
    }

    /**
     * Renders {@code newTree} to the terminal, emitting only the cells that changed
     * since the last call. If an overlay has been pushed via {@link #pushOverlay(Node)},
     * its cells are rendered on top of the base tree.
     *
     * @param newTree the current virtual tree (may be {@code null} to clear the screen)
     */
    public void render(Node newTree) {
        int w = backend.getWidth(), h = backend.getHeight();

        // 1. Layout base tree
        if (newTree != null) {
            layoutEngine.layout(newTree, 0, 0, w, h);
        }

        // 2. Layout overlay tree (if present)
        if (overlayTree != null) {
            layoutEngine.layout(overlayTree, 0, 0, w, h);
        }

        // 3. Diff: find what changed (overlay cells override base)
        List<CellChange> changes = differ.diff(previousTree, previousOverlayTree, newTree, overlayTree);
        if (changes.isEmpty()) return;

        // 4. Apply: push changes to the terminal
        backend.hideCursor();
        for (CellChange change : changes) {
            backend.putChar(change.col(), change.row(), change.character(), change.style());
        }

        // 5. Cursor: prefer focused InputNode inside the overlay, then in the base tree
        InputNode focused = findFocusedInput(overlayTree);
        if (focused == null) focused = findFocusedInput(newTree);
        if (focused != null) {
            backend.setCursor(focused.getX() + focused.getCursorPos(), focused.getY());
        }

        backend.flush();

        previousTree = newTree;
        previousOverlayTree = overlayTree;
    }

    /**
     * Sets a node to render as an overlay on top of the base tree.
     * Call {@link #clearOverlay()} to remove it.
     *
     * @param node the overlay node (e.g., a dialog or popup)
     */
    public void pushOverlay(Node node) {
        this.overlayTree = node;
    }

    /**
     * Removes the current overlay. The next {@link #render(Node)} call will
     * restore the base tree without any overlay.
     */
    public void clearOverlay() {
        this.overlayTree = null;
    }

    /** Returns the current overlay node, or {@code null} if none is active. */
    public Node getOverlay() {
        return overlayTree;
    }

    /**
     * Forces a complete redraw by discarding the previous frame.
     * Use after terminal resize or when the tree structure changes significantly.
     */
    public void forceFullRender(Node newTree) {
        previousTree        = null;
        previousOverlayTree = null;
        render(newTree);
    }

    /**
     * Called when the terminal is resized. Forces a full redraw using the last tree.
     */
    public void onResize() {
        forceFullRender(previousTree);
    }

    /** Returns the most recently rendered tree (may be {@code null} before first render). */
    public Node getPreviousTree() {
        return previousTree;
    }

    /**
     * Walks the node tree depth-first and returns the first {@link InputNode} that is focused,
     * or {@code null} if none is found.
     */
    private InputNode findFocusedInput(Node root) {
        if (root == null) return null;
        if (root instanceof InputNode in && in.isFocused()) return in;
        for (Node child : root.getChildren()) {
            InputNode found = findFocusedInput(child);
            if (found != null) return found;
        }
        return null;
    }
}
