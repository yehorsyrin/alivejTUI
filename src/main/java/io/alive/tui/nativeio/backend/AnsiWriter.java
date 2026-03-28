package io.alive.tui.nativeio.backend;

import io.alive.tui.style.Color;
import io.alive.tui.style.Style;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Standalone ANSI/VT100 escape sequence writer.
 *
 * <p>All terminal control in the native backend funnels through this class.
 * It produces the raw bytes directly to the provided {@link OutputStream},
 * allowing it to be used with any stream (stdout, piped, test).
 *
 * <p>Thread-safety: not thread-safe — callers must synchronise if needed.
 *
 * @author Jarvis (AI)
 */
public final class AnsiWriter {

    private static final String CSI = "\033[";

    private final OutputStream out;

    public AnsiWriter(OutputStream out) {
        if (out == null) throw new IllegalArgumentException("out must not be null");
        this.out = out;
    }

    // --- Screen lifecycle ---

    /** Switch to the alternate screen buffer. */
    public void alternateScreenOn() throws IOException {
        write(CSI + "?1049h");
    }

    /** Switch back to the main screen buffer. */
    public void alternateScreenOff() throws IOException {
        write(CSI + "?1049l");
    }

    /** Hide the cursor. */
    public void hideCursor() throws IOException {
        write(CSI + "?25l");
    }

    /** Show the cursor. */
    public void showCursor() throws IOException {
        write(CSI + "?25h");
    }

    /** Clear the entire screen and move cursor to top-left. */
    public void clearScreen() throws IOException {
        write(CSI + "2J");
        write(CSI + "1;1H");
    }

    /** Reset all SGR attributes. */
    public void resetStyle() throws IOException {
        write(CSI + "0m");
    }

    // --- Cursor positioning ---

    /**
     * Move cursor to the given column and row (0-based).
     * Converts to 1-based ANSI coordinates internally.
     */
    public void moveCursor(int col, int row) throws IOException {
        write(CSI + (row + 1) + ";" + (col + 1) + "H");
    }

    // --- Character output ---

    /**
     * Move cursor to (col, row), apply style, write char, then reset style.
     */
    public void putChar(int col, int row, char c, Style style) throws IOException {
        moveCursor(col, row);
        write(buildSgr(style));
        write(String.valueOf(c));
        write(CSI + "0m");
    }

    // --- Flush ---

    public void flush() throws IOException {
        out.flush();
    }

    // --- SGR builder (package-accessible for tests) ---

    /**
     * Builds an ANSI SGR (Select Graphic Rendition) sequence for the given
     * style.  Always starts with a reset ({@code 0}) so previous attributes
     * do not bleed through.
     */
    public String buildSgr(Style style) {
        if (style == null || style == Style.DEFAULT) return CSI + "0m";
        StringBuilder sb = new StringBuilder(CSI);
        sb.append("0");
        if (style.isBold())          sb.append(";1");
        if (style.isDim())           sb.append(";2");
        if (style.isItalic())        sb.append(";3");
        if (style.isUnderline())     sb.append(";4");
        if (style.isStrikethrough()) sb.append(";9");
        Color fg = style.getForeground();
        if (fg != null) sb.append(";").append(fgCode(fg));
        Color bg = style.getBackground();
        if (bg != null) sb.append(";").append(bgCode(bg));
        sb.append("m");
        return sb.toString();
    }

    // --- Private helpers ---

    private void write(String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String fgCode(Color c) {
        return switch (c.getType()) {
            case ANSI_16  -> c.getAnsiIndex() < 8
                    ? "3" + c.getAnsiIndex()
                    : "9" + (c.getAnsiIndex() - 8);
            case ANSI_256 -> "38;5;" + c.getAnsiIndex();
            case RGB      -> "38;2;" + c.getR() + ";" + c.getG() + ";" + c.getB();
        };
    }

    private static String bgCode(Color c) {
        return switch (c.getType()) {
            case ANSI_16  -> c.getAnsiIndex() < 8
                    ? "4" + c.getAnsiIndex()
                    : "10" + (c.getAnsiIndex() - 8);
            case ANSI_256 -> "48;5;" + c.getAnsiIndex();
            case RGB      -> "48;2;" + c.getR() + ";" + c.getG() + ";" + c.getB();
        };
    }
}
