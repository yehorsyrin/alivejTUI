package io.alive.tui.node;

import io.alive.tui.style.Style;

/**
 * Factory for word-wrapped paragraph text nodes.
 *
 * <p>A paragraph is a {@link TextNode} with word-wrap enabled. Long text and text
 * containing {@code \n} newlines will be broken into multiple lines by the layout engine.
 *
 * <pre>{@code
 * Paragraph.of("This is a long paragraph that will be wrapped at the available width.")
 * Paragraph.ofMarkdown("**Bold** intro followed by *italic* content.")
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Paragraph {

    private Paragraph() {}

    /**
     * Creates a word-wrapped plain-text paragraph node.
     *
     * @param text the paragraph content (may contain {@code \n} for explicit breaks)
     */
    public static TextNode of(String text) {
        return new TextNode(text, Style.DEFAULT).wrap();
    }

    /**
     * Creates a word-wrapped paragraph with a custom base style.
     *
     * @param text  the paragraph content
     * @param style the style to apply
     */
    public static TextNode of(String text, Style style) {
        return new TextNode(text, style).wrap();
    }

    /**
     * Creates a word-wrapped paragraph whose content is parsed as inline markdown.
     * Supported syntax: {@code **bold**}, {@code *italic*}, {@code `code`}, {@code ~~strikethrough~~}.
     *
     * @param text markdown-annotated paragraph content
     */
    public static TextNode ofMarkdown(String text) {
        return new TextNode(MarkdownParser.parse(text, Style.DEFAULT)).wrap();
    }

    /**
     * Creates a word-wrapped markdown paragraph with a custom base style.
     *
     * @param text      markdown-annotated content
     * @param baseStyle base style for unstyled runs
     */
    public static TextNode ofMarkdown(String text, Style baseStyle) {
        return new TextNode(MarkdownParser.parse(text, baseStyle)).wrap();
    }
}
