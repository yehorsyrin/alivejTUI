package io.alive.tui.node;

import io.alive.tui.core.Focusable;
import io.alive.tui.core.Node;

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

    public InputNode(String value, Consumer<String> onChange) {
        this.value = value != null ? value : "";
        this.onChange = onChange;
        this.cursorPos = this.value.length();
        this.placeholder = "";
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

    public InputNode placeholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        return this;
    }

    @Override
    public String getFocusId() { return getKey(); }
}
