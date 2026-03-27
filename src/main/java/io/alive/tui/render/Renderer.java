package io.alive.tui.render;

import io.alive.tui.backend.TerminalBackend;
import io.alive.tui.core.Node;
import io.alive.tui.diff.CellChange;
import io.alive.tui.diff.Differ;
import io.alive.tui.layout.LayoutEngine;

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

    public Renderer(TerminalBackend backend) {
        if (backend == null) throw new IllegalArgumentException("backend must not be null");
        this.backend      = backend;
        this.differ       = new Differ();
        this.layoutEngine = new LayoutEngine();
    }

    /**
     * Renders {@code newTree} to the terminal, emitting only the cells that changed
     * since the last call.
     *
     * @param newTree the current virtual tree (may be {@code null} to clear the screen)
     */
    public void render(Node newTree) {
        // 1. Layout: compute x/y/width/height for every node
        if (newTree != null) {
            layoutEngine.layout(newTree, 0, 0, backend.getWidth(), backend.getHeight());
        }

        // 2. Diff: find what changed
        List<CellChange> changes = differ.diff(previousTree, newTree);
        if (changes.isEmpty()) return;

        // 3. Apply: push changes to the terminal
        backend.hideCursor();
        for (CellChange change : changes) {
            backend.putChar(change.col(), change.row(), change.character(), change.style());
        }
        backend.flush();
        backend.showCursor();

        previousTree = newTree;
    }

    /**
     * Forces a complete redraw by discarding the previous frame.
     * Use after terminal resize or when the tree structure changes significantly.
     */
    public void forceFullRender(Node newTree) {
        previousTree = null;
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
}
