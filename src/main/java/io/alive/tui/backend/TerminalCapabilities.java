package io.alive.tui.backend;

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
     * <p>Lanterna 3.1.x does not expose {@code SGR.FAINT}, so this method currently returns
     * {@code false}. Check this flag before using {@code Style.withDim(true)} if rendering
     * fidelity is important.
     *
     * @return {@code false} — dim is not supported by the default Lanterna backend
     */
    public static boolean supportsDim() {
        return false;
    }
}
