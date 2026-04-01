package io.github.yehorsyrin.tui.style;

/**
 * A color palette that maps semantic roles to {@link Style} values.
 *
 * <p>Use {@link io.github.yehorsyrin.tui.core.AliveJTUI#setTheme} to apply a theme globally and
 * {@link io.github.yehorsyrin.tui.core.AliveJTUI#getTheme} to retrieve it inside components.
 *
 * <pre>{@code
 * AliveJTUI.setTheme(Theme.DARK);
 * // In a component:
 * Style primary = AliveJTUI.getTheme().primary();
 * return Text.of("Hello").style(primary);
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public interface Theme {

    /** Default (foreground) text style. */
    Style foreground();

    /** Background / muted text style. */
    Style muted();

    /** Primary accent style — used for highlighted UI elements. */
    Style primary();

    /** Secondary accent style. */
    Style secondary();

    /** Style for successful states (green-ish). */
    Style success();

    /** Style for warning states (yellow-ish). */
    Style warning();

    /** Style for error states (red-ish). */
    Style error();

    /** Style for focused / selected elements. */
    Style focused();

    /**
     * The default screen background colour for this theme, or {@code null} for
     * the terminal's own default (typically black).
     */
    default Color background() { return null; }

    // --- Built-in themes ---

    /** Dark terminal theme (white-on-dark). */
    Theme DARK  = new BuiltinTheme(
            Style.DEFAULT,
            Style.DEFAULT.withDim(true),
            Style.DEFAULT.withForeground(Color.CYAN).withBold(true),
            Style.DEFAULT.withForeground(Color.BLUE),
            Style.DEFAULT.withForeground(Color.GREEN),
            Style.DEFAULT.withForeground(Color.YELLOW),
            Style.DEFAULT.withForeground(Color.RED),
            Style.DEFAULT.withForeground(Color.CYAN).withBold(true),
            null
    );

    /** Light terminal theme (dark text on light background). */
    Theme LIGHT = new BuiltinTheme(
            Style.DEFAULT.withForeground(Color.BLACK),
            Style.DEFAULT.withForeground(Color.BRIGHT_BLACK),
            Style.DEFAULT.withForeground(Color.BLUE).withBold(true),
            Style.DEFAULT.withForeground(Color.MAGENTA),
            Style.DEFAULT.withForeground(Color.GREEN),
            Style.DEFAULT.withForeground(Color.YELLOW),
            Style.DEFAULT.withForeground(Color.RED),
            Style.DEFAULT.withForeground(Color.BLUE).withBold(true),
            Color.BRIGHT_WHITE
    );

    /**
     * Immutable record-like implementation used by the built-in themes.
     *
     * @author Jarvis (AI)
     */
    final class BuiltinTheme implements Theme {
        private final Style foreground, muted, primary, secondary, success, warning, error, focused;
        private final Color background;

        /** 8-arg constructor — background defaults to {@code null} (terminal default). */
        public BuiltinTheme(Style foreground, Style muted, Style primary, Style secondary,
                            Style success, Style warning, Style error, Style focused) {
            this(foreground, muted, primary, secondary, success, warning, error, focused, null);
        }

        /** Full constructor including a screen background colour. */
        public BuiltinTheme(Style foreground, Style muted, Style primary, Style secondary,
                            Style success, Style warning, Style error, Style focused,
                            Color background) {
            this.foreground = foreground;
            this.muted      = muted;
            this.primary    = primary;
            this.secondary  = secondary;
            this.success    = success;
            this.warning    = warning;
            this.error      = error;
            this.focused    = focused;
            this.background = background;
        }

        @Override public Style foreground() { return foreground; }
        @Override public Style muted()      { return muted; }
        @Override public Style primary()    { return primary; }
        @Override public Style secondary()  { return secondary; }
        @Override public Style success()    { return success; }
        @Override public Style warning()    { return warning; }
        @Override public Style error()      { return error; }
        @Override public Style focused()    { return focused; }
        @Override public Color background() { return background; }
    }
}
