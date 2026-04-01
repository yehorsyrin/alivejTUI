package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Focusable;
import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.style.Style;

/**
 * A clickable button node with a label and an action.
 *
 * @author Jarvis (AI)
 */
public class ButtonNode extends Node implements Focusable {

    private final String label;
    private final Runnable onClick;
    private boolean focused;
    private Style style;
    private Style focusedStyle;

    public ButtonNode(String label, Runnable onClick) {
        this.label = label != null ? label : "";
        this.onClick = onClick;
        this.style = Style.DEFAULT;
        this.focusedStyle = Style.DEFAULT.withBold(true);
    }

    public String getLabel() { return label; }
    public Runnable getOnClick() { return onClick; }
    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; }
    public Style getStyle() { return style; }
    public Style getFocusedStyle() { return focusedStyle; }

    public ButtonNode style(Style style) {
        this.style = style;
        return this;
    }

    public ButtonNode focusedStyle(Style style) {
        this.focusedStyle = style;
        return this;
    }

    @Override
    public String getFocusId() { return getKey(); }

    public void click() {
        if (onClick != null) onClick.run();
    }
}
