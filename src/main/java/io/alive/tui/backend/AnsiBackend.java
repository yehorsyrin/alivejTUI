package io.alive.tui.backend;

import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.style.Color;
import io.alive.tui.style.Style;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * A terminal backend that uses ANSI escape codes via {@link System#out}.
 *
 * <p>This backend works in any terminal that supports ANSI/VT100 escape codes.
 * On Windows, ANSI support must be enabled (Windows 10+ with Virtual Terminal Processing,
 * or by running in a compatible emulator such as Windows Terminal or ConEmu).
 *
 * <p>Key input is read byte-by-byte from {@link System#in}. Arrow keys and other
 * special sequences are decoded from ANSI escape sequences.
 *
 * <p><b>Limitations</b>: Modifier keys (Ctrl/Alt/Shift) and many special keys are
 * not reliably detectable across all terminals. Mouse events are not supported.
 *
 * @author Jarvis (AI)
 */
public class AnsiBackend implements TerminalBackend {

    // ANSI CSI (Control Sequence Introducer)
    private static final String CSI = "\033[";

    private final PrintStream out;
    private final InputStream in;

    private int width  = 80;
    private int height = 24;
    private Runnable resizeListener;

    /** Creates an AnsiBackend using {@link System#out} and {@link System#in}. */
    public AnsiBackend() {
        this(System.out, System.in);
    }

    /**
     * Creates an AnsiBackend with custom streams (useful for testing).
     *
     * @param out output stream for ANSI codes
     * @param in  input stream for key reading
     */
    public AnsiBackend(PrintStream out, InputStream in) {
        if (out == null) throw new IllegalArgumentException("out must not be null");
        if (in  == null) throw new IllegalArgumentException("in must not be null");
        this.out = out;
        this.in  = in;
    }

    @Override
    public void init() {
        // Switch to alternate screen, hide cursor, enable UTF-8
        out.print(CSI + "?1049h");  // alternate screen
        out.print(CSI + "?25l");    // hide cursor
        out.flush();
        detectSize();
    }

    @Override
    public void shutdown() {
        out.print(CSI + "?25h");    // show cursor
        out.print(CSI + "?1049l");  // restore main screen
        out.print(CSI + "0m");      // reset styles
        out.flush();
    }

    @Override
    public int getWidth()  { return width;  }

    @Override
    public int getHeight() { return height; }

    @Override
    public void putChar(int col, int row, char c, Style style) {
        // Move cursor to position (ANSI is 1-based)
        out.print(CSI + (row + 1) + ";" + (col + 1) + "H");
        out.print(buildSgr(style));
        out.print(c);
        out.print(CSI + "0m");  // reset after each char
    }

    @Override
    public void flush() {
        out.flush();
    }

    @Override
    public void hideCursor() {
        out.print(CSI + "?25l");
    }

    @Override
    public void showCursor() {
        out.print(CSI + "?25h");
    }

    @Override
    public void setCursor(int col, int row) {
        out.print(CSI + (row + 1) + ";" + (col + 1) + "H");
        out.print(CSI + "?25h");
    }

    @Override
    public void clear() {
        out.print(CSI + "2J");     // clear entire screen
        out.print(CSI + "1;1H");   // move cursor to top-left
        out.flush();
    }

    @Override
    public KeyEvent readKey() throws InterruptedException {
        try {
            int b = in.read();
            if (b == -1) return KeyEvent.of(KeyType.EOF);
            return decodeInput(b);
        } catch (IOException e) {
            throw new TerminalRenderException("Failed to read key from ANSI terminal", e);
        }
    }

    @Override
    public void setResizeListener(Runnable onResize) {
        this.resizeListener = onResize;
    }

    // --- Helpers ---

    /**
     * Attempts to detect the terminal size by querying the terminal.
     * Falls back to 80×24 if detection fails.
     */
    private void detectSize() {
        // Try to read from environment first
        String cols = System.getenv("COLUMNS");
        String rows = System.getenv("LINES");
        try {
            if (cols != null) width  = Integer.parseInt(cols.trim());
            if (rows != null) height = Integer.parseInt(rows.trim());
        } catch (NumberFormatException ignored) {}
    }

    private KeyEvent decodeInput(int firstByte) throws IOException {
        if (firstByte == 27) {  // ESC
            int next = readWithTimeout(50);
            if (next == -1 || next == 27) return KeyEvent.of(KeyType.ESCAPE);
            if (next == '[') {
                int seq = readWithTimeout(50);
                return switch (seq) {
                    case 'A' -> KeyEvent.of(KeyType.ARROW_UP);
                    case 'B' -> KeyEvent.of(KeyType.ARROW_DOWN);
                    case 'C' -> KeyEvent.of(KeyType.ARROW_RIGHT);
                    case 'D' -> KeyEvent.of(KeyType.ARROW_LEFT);
                    case 'H' -> KeyEvent.of(KeyType.HOME);
                    case 'F' -> KeyEvent.of(KeyType.END);
                    case '5' -> { readWithTimeout(20); yield KeyEvent.of(KeyType.PAGE_UP); }
                    case '6' -> { readWithTimeout(20); yield KeyEvent.of(KeyType.PAGE_DOWN); }
                    case '3' -> { readWithTimeout(20); yield KeyEvent.of(KeyType.DELETE); }
                    default  -> KeyEvent.of(KeyType.ESCAPE);
                };
            }
            return KeyEvent.of(KeyType.ESCAPE);
        }
        return switch (firstByte) {
            case 13  -> KeyEvent.of(KeyType.ENTER);
            case 127 -> KeyEvent.of(KeyType.BACKSPACE);
            case 9   -> KeyEvent.of(KeyType.TAB);
            default  -> firstByte >= 32
                    ? KeyEvent.ofCharacter((char) firstByte)
                    : KeyEvent.of(KeyType.EOF);
        };
    }

    private int readWithTimeout(long timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (in.available() > 0) return in.read();
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
        }
        return -1;
    }

    /** Builds an ANSI SGR (Select Graphic Rendition) sequence for the given style. */
    String buildSgr(Style style) {
        if (style == null || style == Style.DEFAULT) return CSI + "0m";
        StringBuilder sb = new StringBuilder(CSI);
        sb.append("0");  // always reset first
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

    private String fgCode(Color c) {
        return switch (c.getType()) {
            case ANSI_16  -> c.getAnsiIndex() < 8 ? "3" + c.getAnsiIndex() : "9" + (c.getAnsiIndex() - 8);
            case ANSI_256 -> "38;5;" + c.getAnsiIndex();
            case RGB      -> "38;2;" + c.getR() + ";" + c.getG() + ";" + c.getB();
        };
    }

    private String bgCode(Color c) {
        return switch (c.getType()) {
            case ANSI_16  -> c.getAnsiIndex() < 8 ? "4" + c.getAnsiIndex() : "10" + (c.getAnsiIndex() - 8);
            case ANSI_256 -> "48;5;" + c.getAnsiIndex();
            case RGB      -> "48;2;" + c.getR() + ";" + c.getG() + ";" + c.getB();
        };
    }
}
