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
            Style.DEFAULT.withForeground(Color.CYAN).withBold(true)
    );

    /** Light terminal theme (dark-on-light, best-effort — most terminals ignore background). */
    Theme LIGHT = new BuiltinTheme(
            Style.DEFAULT,
            Style.DEFAULT.withDim(true),
            Style.DEFAULT.withForeground(Color.BLUE).withBold(true),
            Style.DEFAULT.withForeground(Color.MAGENTA),
            Style.DEFAULT.withForeground(Color.GREEN),
            Style.DEFAULT.withForeground(Color.YELLOW),
            Style.DEFAULT.withForeground(Color.RED),
            Style.DEFAULT.withForeground(Color.BLUE).withBold(true)
    );

    /**
     * Immutable record-like implementation used by the built-in themes.
     *
     * @author Jarvis (AI)
     */
    final class BuiltinTheme implements Theme {
        private final Style foreground, muted, primary, secondary, success, warning, error, focused;

        public BuiltinTheme(Style foreground, Style muted, Style primary, Style secondary,
                            Style success, Style warning, Style error, Style focused) {
            this.foreground = foreground;
            this.muted      = muted;
            this.primary    = primary;
            this.secondary  = secondary;
            this.success    = success;
            this.warning    = warning;
            this.error      = error;
            this.focused    = focused;
        }

        @Override public Style foreground() { return foreground; }
        @Override public Style muted()      { return muted; }
        @Override public Style primary()    { return primary; }
        @Override public Style secondary()  { return secondary; }
        @Override public Style success()    { return success; }
        @Override public Style warning()    { return warning; }
        @Override public Style error()      { return error; }
        @Override public Style focused()    { return focused; }
    }
}
