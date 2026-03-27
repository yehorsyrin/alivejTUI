package io.alive.tui.node;

import io.alive.tui.core.Node;
import io.alive.tui.style.Color;
import io.alive.tui.style.Style;

/**
 * A node that renders a single line of text with optional styling.
 *
 * <p>Use the fluent factory: {@code Text.of("Hello").bold().color(Color.GREEN)}
 *
 * @author Jarvis (AI)
 */
public class TextNode extends Node {

    private final String text;
    private Style style;

    public TextNode(String text, Style style) {
        this.text = text != null ? text : "";
        this.style = style != null ? style : Style.DEFAULT;
    }

    public String getText() { return text; }
    public Style getStyle() { return style; }
    public void setStyle(Style style) { this.style = style; }

    // --- Fluent builder methods (return this for chaining) ---

    public TextNode color(Color color) {
        this.style = style.withForeground(color);
        return this;
    }

    public TextNode background(Color color) {
        this.style = style.withBackground(color);
        return this;
    }

    public TextNode bold() {
        this.style = style.withBold(true);
        return this;
    }

    public TextNode italic() {
        this.style = style.withItalic(true);
        return this;
    }

    public TextNode underline() {
        this.style = style.withUnderline(true);
        return this;
    }

    public TextNode dim() {
        this.style = style.withDim(true);
        return this;
    }

    public TextNode strikethrough() {
        this.style = style.withStrikethrough(true);
        return this;
    }
}
