package io.alive.tui.style;

/**
 * Immutable text style: colors and text decorations.
 *
 * @author Jarvis (AI)
 */
public final class Style {

    public static final Style DEFAULT = new Style(null, null, false, false, false, false, false);

    private final Color foreground;
    private final Color background;
    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final boolean dim;
    private final boolean strikethrough;

    public Style(Color foreground, Color background,
                 boolean bold, boolean italic, boolean underline,
                 boolean dim, boolean strikethrough) {
        this.foreground = foreground;
        this.background = background;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.dim = dim;
        this.strikethrough = strikethrough;
    }

    public Color getForeground() { return foreground; }
    public Color getBackground() { return background; }
    public boolean isBold() { return bold; }
    public boolean isItalic() { return italic; }
    public boolean isUnderline() { return underline; }
    public boolean isDim() { return dim; }
    public boolean isStrikethrough() { return strikethrough; }

    public Style withForeground(Color fg) {
        return new Style(fg, background, bold, italic, underline, dim, strikethrough);
    }

    public Style withBackground(Color bg) {
        return new Style(foreground, bg, bold, italic, underline, dim, strikethrough);
    }

    public Style withBold(boolean bold) {
        return new Style(foreground, background, bold, italic, underline, dim, strikethrough);
    }

    public Style withItalic(boolean italic) {
        return new Style(foreground, background, bold, italic, underline, dim, strikethrough);
    }

    public Style withUnderline(boolean underline) {
        return new Style(foreground, background, bold, italic, underline, dim, strikethrough);
    }

    /**
     * Returns a copy of this style with the dim (faint) decoration set.
     *
     * @apiNote Dim is silently ignored by the default Lanterna backend.
     *          Check {@link io.alive.tui.backend.TerminalCapabilities#supportsDim()} before use
     *          if rendering fidelity matters.
     * @param dim {@code true} to enable dim
     * @return new {@link Style} with updated dim flag
     */
    public Style withDim(boolean dim) {
        return new Style(foreground, background, bold, italic, underline, dim, strikethrough);
    }

    public Style withStrikethrough(boolean strikethrough) {
        return new Style(foreground, background, bold, italic, underline, dim, strikethrough);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Style s)) return false;
        return bold == s.bold && italic == s.italic && underline == s.underline
            && dim == s.dim && strikethrough == s.strikethrough
            && java.util.Objects.equals(foreground, s.foreground)
            && java.util.Objects.equals(background, s.background);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(foreground, background, bold, italic, underline, dim, strikethrough);
    }
}
