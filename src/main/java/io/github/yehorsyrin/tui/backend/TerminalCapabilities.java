package io.github.yehorsyrin.tui.backend;

/**
 * Runtime query of terminal capability flags.
 *
 * <p>Used to check whether a given terminal backend supports optional text decorations.
 *
 * @author Jarvis (AI)
 */
public final class TerminalCapabilities {

    private TerminalCapabilities() {}

    /**
     * Returns {@code true} if the current backend supports the dim (faint) text decoration.
     *
     * <p>Currently returns {@code true} — both the native ANSI backend and the Swing
     * backend render dim via SGR 2 / reduced opacity. Override in a custom backend if
     * dim is not supported.
     *
     * @return {@code true} — dim is supported by the built-in backends
     */
    public static boolean supportsDim() {
        return true;
    }
}
