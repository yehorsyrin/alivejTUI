package io.alive.tui.nativeio.size;

/**
 * Immutable terminal dimensions in columns and rows.
 *
 * @author Jarvis (AI)
 */
public record TerminalSize(int cols, int rows) {

    /** Fallback size used when detection is not possible. */
    public static final TerminalSize DEFAULT = new TerminalSize(80, 24);

    public TerminalSize {
        if (cols <= 0) throw new IllegalArgumentException("cols must be > 0, got: " + cols);
        if (rows <= 0) throw new IllegalArgumentException("rows must be > 0, got: " + rows);
    }
}
