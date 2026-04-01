package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Focusable;
import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.style.Style;

import java.util.function.Consumer;

/**
 * A single-line text input field.
 *
 * @author Jarvis (AI)
 */
public class InputNode extends Node implements Focusable {

    private String value;
    private final Consumer<String> onChange;
    private boolean focused;
    private int cursorPos;
    private String placeholder;
    private Style style;
    private Style focusedStyle;

    public InputNode(String value, Consumer<String> onChange) {
        this.value = value != null ? value : "";
        this.onChange = onChange;
        this.cursorPos = this.value.length();
        this.placeholder = "";
        this.style = Style.DEFAULT;
        this.focusedStyle = Style.DEFAULT.withUnderline(true);
    }

    public String getValue() { return value; }

    public void setValue(String value) {
        this.value = value != null ? value : "";
        this.cursorPos = Math.min(cursorPos, this.value.length());
        if (onChange != null) onChange.accept(this.value);
    }

    public Consumer<String> getOnChange() { return onChange; }
    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; }
    public int getCursorPos() { return cursorPos; }
    public void setCursorPos(int cursorPos) {
        this.cursorPos = Math.max(0, Math.min(cursorPos, value.length()));
    }
    public String getPlaceholder() { return placeholder; }

    public Style getStyle() { return style; }
    public Style getFocusedStyle() { return focusedStyle; }

    public InputNode style(Style style) {
        this.style = style != null ? style : Style.DEFAULT;
        return this;
    }

    public InputNode focusedStyle(Style style) {
        this.focusedStyle = style != null ? style : Style.DEFAULT;
        return this;
    }

    public InputNode placeholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        return this;
    }

    @Override
    public String getFocusId() { return getKey(); }
}
