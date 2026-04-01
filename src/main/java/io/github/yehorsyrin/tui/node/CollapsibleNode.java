package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Focusable;
import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.style.Style;

/**
 * A collapsible / disclosure node that shows a title row with a triangle indicator
 * and reveals its children when expanded.
 *
 * <pre>
 * Collapsed:  ▶ Section Title
 * Expanded:   ▼ Section Title
 *               child row 1
 *               child row 2
 * </pre>
 *
 * <p>Toggling is typically wired to {@code Space} or {@code Enter} key events:
 * <pre>{@code
 * bus.register(KeyType.Character(' '), () -> { collapsible.toggle(); markDirty(); });
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public class CollapsibleNode extends Node implements Focusable {

    /** Arrow shown when the section is collapsed. */
    public static final char COLLAPSED_ARROW = '▶';
    /** Arrow shown when the section is expanded. */
    public static final char EXPANDED_ARROW  = '▼';
    /** Space between the arrow and the title. */
    public static final String ARROW_PADDING = " ";

    private final String title;
    private boolean expanded;
    private boolean focused = false;

    private Style titleStyle   = Style.DEFAULT;
    private Style focusedStyle = Style.DEFAULT.withBold(true);

    /**
     * Creates a collapsible node.
     *
     * @param title    the section header label
     * @param expanded whether the section starts expanded
     * @param children child nodes to reveal when expanded; may be empty
     */
    public CollapsibleNode(String title, boolean expanded, Node... children) {
        this.title    = title != null ? title : "";
        this.expanded = expanded;
        for (Node child : children) {
            if (child != null) this.children.add(child);
        }
    }

    /** Returns the title label. */
    public String getTitle() { return title; }

    /** Returns {@code true} if the section is currently expanded. */
    public boolean isExpanded() { return expanded; }

    /** Expands the section. */
    public void expand() { this.expanded = true; }

    /** Collapses the section. */
    public void collapse() { this.expanded = false; }

    /** Toggles between expanded and collapsed states. */
    public void toggle() { this.expanded = !this.expanded; }

    /** Returns the style used for the title row. */
    public Style getTitleStyle() { return titleStyle; }

    /** Returns the style used when the node is focused. */
    public Style getFocusedStyle() { return focusedStyle; }

    /** Sets the title style. Returns {@code this} for chaining. */
    public CollapsibleNode titleStyle(Style s)   { this.titleStyle   = s != null ? s : Style.DEFAULT; return this; }

    /** Sets the focused style. Returns {@code this} for chaining. */
    public CollapsibleNode focusedStyle(Style s) { this.focusedStyle = s != null ? s : Style.DEFAULT; return this; }

    // --- Focusable ---

    @Override public boolean isFocused()           { return focused; }
    @Override public void    setFocused(boolean f) { this.focused = f; }
    @Override public String  getFocusId()          { return title.isEmpty() ? "collapsible" : title; }
}
