package io.alive.tui.node;

import io.alive.tui.core.Node;
import io.alive.tui.style.Color;
import io.alive.tui.style.Style;

import java.util.List;

/**
 * A node that renders a single line of text with optional styling.
 *
 * <p>Plain usage: {@code Text.of("Hello").bold().color(Color.GREEN)}<br>
 * Markdown usage: {@code Text.ofMarkdown("**Hello** *world*")}
 *
 * @author Jarvis (AI)
 */
public class TextNode extends Node {

    private final String text;
    private Style style;

    /**
     * Non-null when this node was created via {@link Text#ofMarkdown}.
     * Each segment carries its own style; {@link #style} is unused in this mode.
     */
    private final List<StyledSegment> segments;

    /** Plain-text constructor (used by {@link Text#of}). */
    public TextNode(String text, Style style) {
        this.text     = text != null ? text : "";
        this.style    = style != null ? style : Style.DEFAULT;
        this.segments = null;
    }

    /** Markdown constructor (used by {@link Text#ofMarkdown}). */
    TextNode(List<StyledSegment> segments) {
        this.segments = List.copyOf(segments);
        // Derive plain text from segments for width calculation
        StringBuilder sb = new StringBuilder();
        for (StyledSegment s : segments) sb.append(s.text());
        this.text  = sb.toString();
        this.style = Style.DEFAULT;
    }

    public String getText() { return text; }
    public Style getStyle() { return style; }
    public void setStyle(Style style) { this.style = style; }

    /**
     * Returns the styled segments if this node was created via {@link Text#ofMarkdown},
     * or {@code null} for plain-text nodes.
     */
    public List<StyledSegment> getSegments() { return segments; }

    /** Returns {@code true} if this node holds markdown-parsed segments. */
    public boolean hasMarkdown() { return segments != null; }

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
