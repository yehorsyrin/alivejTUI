package io.alive.tui.platform.size;

/**
 * Terminal dimensions in columns and rows.
 *
 * @param cols number of character columns (width)
 * @param rows number of character rows (height)
 * @author Jarvis (AI)
 */
public record TerminalSize(int cols, int rows) {

    public TerminalSize {
        if (cols < 1) throw new IllegalArgumentException("cols must be >= 1, got " + cols);
        if (rows < 1) throw new IllegalArgumentException("rows must be >= 1, got " + rows);
    }

    /** Fallback size used when detection fails. */
    public static final TerminalSize FALLBACK = new TerminalSize(80, 24);
}
