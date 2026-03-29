package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.style.Style;

/**
 * A text fragment paired with a display style.
 * Used by markdown-parsed {@link TextNode}s to represent inline-styled text.
 *
 * <pre>{@code
 * List<StyledSegment> segments = MarkdownParser.parse("**bold** and *italic*", Style.DEFAULT);
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public record StyledSegment(String text, Style style) {

    public StyledSegment {
        if (text  == null) text  = "";
        if (style == null) style = Style.DEFAULT;
    }
}
