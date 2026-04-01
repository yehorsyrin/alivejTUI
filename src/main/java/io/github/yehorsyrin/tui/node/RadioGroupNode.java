package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Focusable;
import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.style.Style;

import java.util.List;

/**
 * A radio button group node that displays a vertical list of options with one selected at a time.
 *
 * <p>Rendered as:
 * <pre>
 *   (●) Selected Option
 *   ( ) Other Option
 * </pre>
 *
 * <p>The selected row uses {@code focusedStyle} when this node has keyboard focus,
 * otherwise {@code normalStyle} is used for all rows.
 *
 * @author Jarvis (AI)
 */
public class RadioGroupNode extends Node implements Focusable {

    /** Bullet character rendered for the selected option. */
    public static final char SELECTED_BULLET = '●';

    /** Bullet character rendered for unselected options. */
    public static final char UNSELECTED_BULLET = ' ';

    /** Prefix rendered before a selected option: {@code "(●) "}. */
    public static final String SELECTED_PREFIX = "(●) ";

    /** Prefix rendered before an unselected option: {@code "( ) "}. */
    public static final String UNSELECTED_PREFIX = "( ) ";

    private final List<String> options;
    private int selectedIndex;
    private boolean focused;
    private Style normalStyle;
    private Style focusedStyle;

    /**
     * Creates a new radio group node.
     *
     * @param options list of option labels (must not be {@code null})
     */
    public RadioGroupNode(List<String> options) {
        this.options = options != null ? List.copyOf(options) : List.of();
        this.selectedIndex = 0;
        this.normalStyle = Style.DEFAULT;
        this.focusedStyle = Style.DEFAULT.withBold(true);
    }

    /** Returns the list of option labels. */
    public List<String> getOptions() { return options; }

    /** Returns the 0-based index of the currently selected option. */
    public int getSelectedIndex() { return selectedIndex; }

    /**
     * Sets the selected option by 0-based index.
     *
     * @param selectedIndex the index to select; clamped silently if out of range
     */
    public void setSelectedIndex(int selectedIndex) {
        if (options.isEmpty()) {
            this.selectedIndex = 0;
        } else {
            this.selectedIndex = Math.max(0, Math.min(selectedIndex, options.size() - 1));
        }
    }

    /** Returns the normal (unfocused) style. */
    public Style getNormalStyle() { return normalStyle; }

    /** Returns the focused style applied to the selected row when focused. */
    public Style getFocusedStyle() { return focusedStyle; }

    /** Fluent setter for the normal style. */
    public RadioGroupNode normalStyle(Style style) {
        this.normalStyle = style;
        return this;
    }

    /** Fluent setter for the focused style. */
    public RadioGroupNode focusedStyle(Style style) {
        this.focusedStyle = style;
        return this;
    }

    @Override
    public boolean isFocused() { return focused; }

    @Override
    public void setFocused(boolean focused) { this.focused = focused; }

    @Override
    public String getFocusId() { return key != null ? key : "radio-group"; }
}
