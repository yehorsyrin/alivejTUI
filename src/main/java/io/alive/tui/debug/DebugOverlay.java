package io.alive.tui.debug;

import io.alive.tui.core.Node;
import io.alive.tui.node.TextNode;
import io.alive.tui.node.VBoxNode;
import io.alive.tui.style.Color;
import io.alive.tui.style.Style;

/**
 * Optional HUD overlay showing render stats, node count, and memory usage.
 *
 * <p>Builds a lightweight {@link Node} subtree that can be pushed as an overlay:
 *
 * <pre>{@code
 * AliveJTUI.pushOverlay(DebugOverlay.build(renderCount, nodeCount));
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class DebugOverlay {

    private static final Style OVERLAY_STYLE = Style.DEFAULT
            .withForeground(Color.BRIGHT_GREEN)
            .withBold(true);

    private DebugOverlay() {}

    /**
     * Builds a debug HUD node displaying the given stats.
     *
     * @param renderCount  total number of renders since application start
     * @param nodeCount    number of nodes in the current tree
     * @return a node ready to be pushed as an overlay
     */
    public static Node build(long renderCount, int nodeCount) {
        Runtime rt = Runtime.getRuntime();
        long usedMb  = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long totalMb = rt.totalMemory() / (1024 * 1024);

        VBoxNode hud = new VBoxNode(0);
        hud.addChild(new TextNode(
                String.format("renders: %d  nodes: %d", renderCount, nodeCount),
                OVERLAY_STYLE));
        hud.addChild(new TextNode(
                String.format("mem: %dMB / %dMB", usedMb, totalMb),
                OVERLAY_STYLE));
        return hud;
    }

    /**
     * Counts the total number of nodes in the given tree (depth-first).
     *
     * @param root root node; {@code null} returns 0
     * @return total node count
     */
    public static int countNodes(Node root) {
        if (root == null) return 0;
        int count = 1;
        for (Node child : root.getChildren()) count += countNodes(child);
        return count;
    }
}
