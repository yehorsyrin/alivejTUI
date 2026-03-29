package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.style.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal inline-markdown parser that converts a marked-up string into a list of
 * {@link StyledSegment}s, each carrying its own {@link Style}.
 *
 * <h2>Supported syntax</h2>
 * <ul>
 *   <li>{@code **text**} — bold</li>
 *   <li>{@code *text*}   — italic</li>
 *   <li>{@code `text`}   — code (bold + underline)</li>
 *   <li>{@code ~~text~~} — strikethrough</li>
 * </ul>
 *
 * <p>Markers without a matching close are emitted as plain text.
 * Nesting is not supported.
 *
 * @author Jarvis (AI)
 */
public final class MarkdownParser {

    private MarkdownParser() {}

    /**
     * Parses {@code input} and returns a list of styled segments.
     * The {@code baseStyle} is applied to unstyled runs and merged with span styles.
     *
     * @param input     the markdown-annotated string
     * @param baseStyle the base style for unstyled text
     * @return ordered list of {@link StyledSegment}s
     */
    public static List<StyledSegment> parse(String input, Style baseStyle) {
        if (input == null || input.isEmpty()) return List.of();
        if (baseStyle == null) baseStyle = Style.DEFAULT;

        List<StyledSegment> result = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        int i = 0;
        int len = input.length();

        while (i < len) {
            // ** bold **
            if (i + 1 < len && input.charAt(i) == '*' && input.charAt(i + 1) == '*') {
                int end = input.indexOf("**", i + 2);
                if (end >= 0) {
                    flushPlain(plain, baseStyle, result);
                    result.add(new StyledSegment(input.substring(i + 2, end),
                                                 baseStyle.withBold(true)));
                    i = end + 2;
                    continue;
                }
            }
            // ~~ strikethrough ~~
            if (i + 1 < len && input.charAt(i) == '~' && input.charAt(i + 1) == '~') {
                int end = input.indexOf("~~", i + 2);
                if (end >= 0) {
                    flushPlain(plain, baseStyle, result);
                    result.add(new StyledSegment(input.substring(i + 2, end),
                                                 baseStyle.withStrikethrough(true)));
                    i = end + 2;
                    continue;
                }
            }
            // ` code `
            if (input.charAt(i) == '`') {
                int end = input.indexOf('`', i + 1);
                if (end >= 0) {
                    flushPlain(plain, baseStyle, result);
                    result.add(new StyledSegment(input.substring(i + 1, end),
                                                 baseStyle.withBold(true).withUnderline(true)));
                    i = end + 1;
                    continue;
                }
            }
            // * italic *  (checked after ** to avoid double-consuming)
            // Require end > i+1 to avoid matching an empty span like the second * of **
            if (input.charAt(i) == '*') {
                int end = input.indexOf('*', i + 1);
                if (end > i + 1) {
                    flushPlain(plain, baseStyle, result);
                    result.add(new StyledSegment(input.substring(i + 1, end),
                                                 baseStyle.withItalic(true)));
                    i = end + 1;
                    continue;
                }
            }
            // Plain character
            plain.append(input.charAt(i++));
        }

        flushPlain(plain, baseStyle, result);
        return List.copyOf(result);
    }

    /** Parses with {@link Style#DEFAULT} as the base style. */
    public static List<StyledSegment> parse(String input) {
        return parse(input, Style.DEFAULT);
    }

    private static void flushPlain(StringBuilder buf, Style style, List<StyledSegment> out) {
        if (buf.isEmpty()) return;
        out.add(new StyledSegment(buf.toString(), style));
        buf.setLength(0);
    }
}
