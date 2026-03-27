package io.alive.tui.node;

import io.alive.tui.style.Style;

/**
 * Factory for {@link TextNode}.
 *
 * <pre>{@code
 * Text.of("Hello").bold().color(Color.GREEN)
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
}
