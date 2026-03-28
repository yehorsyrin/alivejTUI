package io.alive.tui.node;

import io.alive.tui.core.Focusable;
import io.alive.tui.core.Node;
import io.alive.tui.style.Color;
import io.alive.tui.style.Style;

/**
 * A toggleable checkbox node with a label and an optional change callback.
 *
 * <p>Rendered as {@code [✓] Label} when checked, or {@code [ ] Label} when unchecked.
 * Space or Enter toggles the state when this node has keyboard focus.
 *
 * @author Jarvis (AI)
 */
public class CheckboxNode extends Node implements Focusable {

    private final String label;
    private boolean checked;
    private final Runnable onChange;
    private boolean focused;
    private Style style;
    private Style focusedStyle;

    /**
     * Creates a new checkbox node.
     *
     * @param label    the display label (must not be {@code null})
     * @param checked  initial checked state
     * @param onChange optional callback invoked after each toggle; may be {@code null}
     */
    public CheckboxNode(String label, boolean checked, Runnable onChange) {
        this.label = label != null ? label : "";
        this.checked = checked;
        this.onChange = onChange;
        this.style = Style.DEFAULT;
        this.focusedStyle = Style.DEFAULT.withForeground(Color.YELLOW).withBold(true);
    }

    /** Returns the checkbox label. */
    public String getLabel() { return label; }

    /** Returns {@code true} if this checkbox is currently checked. */
    public boolean isChecked() { return checked; }

    /**
     * Toggles the checked state and invokes the {@code onChange} callback if present.
     */
    public void toggle() {
        checked = !checked;
        if (onChange != null) onChange.run();
    }

    @Override
    public boolean isFocused() { return focused; }

    @Override
    public void setFocused(boolean focused) { this.focused = focused; }

    @Override
    public String getFocusId() { return label; }

    /** Returns the normal (unfocused) style. */
    public Style getStyle() { return style; }

    /** Returns the focused style. */
    public Style getFocusedStyle() { return focusedStyle; }

    /** Fluent setter for the normal style. */
    public CheckboxNode style(Style style) {
        this.style = style;
        return this;
    }

    /** Fluent setter for the focused style. */
    public CheckboxNode focusedStyle(Style style) {
        this.focusedStyle = style;
        return this;
    }
}
