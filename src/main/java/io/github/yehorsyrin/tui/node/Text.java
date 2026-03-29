package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.style.Style;

/**
 * Factory for {@link TextNode}.
 *
 * <pre>{@code
 * Text.of("Hello").bold().color(Color.GREEN)
 * Text.ofMarkdown("**Hello** and *world*")
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Text {

    private Text() {}

    public static TextNode of(String text) {
        return new TextNode(text, Style.DEFAULT);
    }

    public static TextNode of(String text, Style style) {
        return new TextNode(text, style);
    }

    /**
     * Creates a {@link TextNode} whose content is parsed as inline markdown.
     * Supported syntax: {@code **bold**}, {@code *italic*}, {@code `code`}, {@code ~~strikethrough~~}.
     *
     * @param text markdown-annotated text
     */
    public static TextNode ofMarkdown(String text) {
        return new TextNode(MarkdownParser.parse(text, Style.DEFAULT));
    }

    /**
     * Creates a markdown {@link TextNode} with a custom base style applied to unstyled spans.
     *
     * @param text      markdown-annotated text
     * @param baseStyle style for unstyled runs (merged with span styles)
     */
    public static TextNode ofMarkdown(String text, Style baseStyle) {
        return new TextNode(MarkdownParser.parse(text, baseStyle));
    }
}
