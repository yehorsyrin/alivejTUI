package io.alive.tui.nativeio.size;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Wincon;
import com.sun.jna.platform.win32.WinNT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Detects the current terminal size (columns × rows) using the most appropriate
 * method for the current platform.
 *
 * <p>Strategy selection (in order):
 * <ol>
 *   <li>Windows: {@link #detectWindows()} — {@code GetConsoleScreenBufferInfo} via JNA</li>
 *   <li>ANSI cursor hack: {@link #detectAnsi(InputStream, OutputStream)} — write
 *       {@code ESC[999;999H ESC[6n}, read {@code ESC[r;cR}</li>
 *   <li>Environment variables {@code COLUMNS} / {@code LINES}</li>
 *   <li>Fallback: 80×24</li>
 * </ol>
 *
 * @author Jarvis (AI)
 */
public final class TerminalSizeDetector {

    private TerminalSizeDetector() {}

    /**
     * Auto-selects the best detection strategy for the current platform.
     *
     * @return detected terminal size, never {@code null}
     */
    public static TerminalSize detect() {
        if (isWindows()) {
            TerminalSize win = detectWindows();
            if (win != null) return win;
        }
        TerminalSize env = detectFromEnvironment();
        if (env != null) return env;
        return TerminalSize.DEFAULT;
    }

    /**
     * Windows: uses {@code GetConsoleScreenBufferInfo} from kernel32.dll.
     *
     * @return size or {@code null} if not on Windows / no console handle
     */
    public static TerminalSize detectWindows() {
        if (!isWindows()) return null;
        try {
            WinNT.HANDLE hOut = Kernel32.INSTANCE.GetStdHandle(-11); // STD_OUTPUT_HANDLE
            if (hOut == null || hOut.equals(WinNT.INVALID_HANDLE_VALUE)) return null;

            Wincon.CONSOLE_SCREEN_BUFFER_INFO info = new Wincon.CONSOLE_SCREEN_BUFFER_INFO();
            if (!Kernel32.INSTANCE.GetConsoleScreenBufferInfo(hOut, info)) return null;

            // srWindow is the visible window, not the full buffer
            int cols = info.srWindow.Right  - info.srWindow.Left + 1;
            int rows = info.srWindow.Bottom - info.srWindow.Top  + 1;
            if (cols > 0 && rows > 0) return new TerminalSize(cols, rows);
        } catch (UnsatisfiedLinkError | Exception ignored) {
        }
        return null;
    }

    /**
     * ANSI cursor position query hack.
     *
     * <p>Writes {@code ESC[999;999H} (move cursor to extreme bottom-right) then
     * {@code ESC[6n} (request cursor position).  The terminal responds with
     * {@code ESC[r;cR} where {@code r} and {@code c} are the actual dimensions.
     *
     * <p>Requires the terminal to be in raw mode so the response is not
     * line-buffered.
     *
     * @param in  raw input stream (must be in raw mode, non-blocking preferred)
     * @param out output stream connected to the terminal
     * @return detected size or {@code null} if the response could not be parsed
     */
    public static TerminalSize detectAnsi(InputStream in, OutputStream out) {
        try {
            // Save cursor, move to extremes, query position, restore cursor
            out.write("\033[s\033[999;999H\033[6n\033[u".getBytes(StandardCharsets.UTF_8));
            out.flush();

            // Read ESC [ rows ; cols R  (timeout ~500 ms)
            StringBuilder sb = new StringBuilder();
            long deadline = System.currentTimeMillis() + 500;
            int b;
            boolean inEscape = false;
            while (System.currentTimeMillis() < deadline) {
                if (in.available() <= 0) {
                    Thread.sleep(5);
                    continue;
                }
                b = in.read();
                if (b == 27) { inEscape = true; continue; }
                if (inEscape && b == '[') continue;
                if (inEscape && b == 'R') break;
                if (inEscape) sb.append((char) b);
            }
            String response = sb.toString(); // e.g. "24;80"
            String[] parts = response.split(";");
            if (parts.length == 2) {
                int rows = Integer.parseInt(parts[0].trim());
                int cols = Integer.parseInt(parts[1].trim());
                if (cols > 0 && rows > 0) return new TerminalSize(cols, rows);
            }
        } catch (IOException | InterruptedException | NumberFormatException ignored) {
            if (Thread.currentThread().isInterrupted()) Thread.currentThread().interrupt();
        }
        return null;
    }

    // --- Private helpers ---

    private static TerminalSize detectFromEnvironment() {
        try {
            String cols = System.getenv("COLUMNS");
            String rows = System.getenv("LINES");
            if (cols != null && rows != null) {
                int c = Integer.parseInt(cols.trim());
                int r = Integer.parseInt(rows.trim());
                if (c > 0 && r > 0) return new TerminalSize(c, r);
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
