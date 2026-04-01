package io.github.yehorsyrin.tui.platform.size;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Wincon;

/**
 * Detects the current terminal dimensions (columns × rows).
 *
 * <p>Detection strategy:
 * <ol>
 *   <li>Windows: {@code GetConsoleScreenBufferInfo} via JNA Kernel32.</li>
 *   <li>POSIX: {@code ioctl(TIOCGWINSZ)} via JNA libc.</li>
 *   <li>Environment variables: {@code $COLUMNS} / {@code $LINES}.</li>
 *   <li>Fallback: {@link TerminalSize#FALLBACK} (80×24).</li>
 * </ol>
 *
 * <p>{@link #detect()} selects the appropriate strategy automatically.
 *
 * @author Jarvis (AI)
 */
public final class TerminalSizeDetector {

    // ── POSIX ioctl ──────────────────────────────────────────────────────────

    /** TIOCGWINSZ ioctl request code — Linux x86-64. */
    private static final int TIOCGWINSZ_LINUX = 0x5413;
    /** TIOCGWINSZ ioctl request code — macOS/BSD. */
    private static final int TIOCGWINSZ_MACOS = 0x40087468;

    private static final int STDOUT_FD = 1;

    interface LibC extends Library {
        LibC INSTANCE = loadLibC();

        int ioctl(int fd, long request, Winsize winsize);

        @SuppressWarnings("unused")
        private static LibC loadLibC() {
            try {
                return Native.load("c", LibC.class);
            } catch (UnsatisfiedLinkError e) {
                return null;
            }
        }
    }

    @FieldOrder({"ws_row", "ws_col", "ws_xpixel", "ws_ypixel"})
    public static class Winsize extends Structure {
        public short ws_row;
        public short ws_col;
        public short ws_xpixel;
        public short ws_ypixel;
    }

    // ── Windows CONSOLE_SCREEN_BUFFER_INFO ───────────────────────────────────

    /** JNA mapping for {@code CONSOLE_SCREEN_BUFFER_INFO}. */
    @FieldOrder({"dwSizeX", "dwSizeY",
                 "dwCursorPositionX", "dwCursorPositionY",
                 "wAttributes",
                 "srWindowLeft", "srWindowTop", "srWindowRight", "srWindowBottom",
                 "dwMaximumWindowSizeX", "dwMaximumWindowSizeY"})
    public static class ConsoleScreenBufferInfo extends Structure {
        public short dwSizeX, dwSizeY;
        public short dwCursorPositionX, dwCursorPositionY;
        public short wAttributes;
        public short srWindowLeft, srWindowTop, srWindowRight, srWindowBottom;
        public short dwMaximumWindowSizeX, dwMaximumWindowSizeY;
    }

    interface Kernel32Ext extends Library {
        Kernel32Ext INSTANCE = loadKernel32Ext();

        boolean GetConsoleScreenBufferInfo(WinNT.HANDLE hConsoleOutput,
                                           ConsoleScreenBufferInfo lpConsoleScreenBufferInfo);

        @SuppressWarnings("unused")
        private static Kernel32Ext loadKernel32Ext() {
            try {
                return Native.load("kernel32", Kernel32Ext.class);
            } catch (UnsatisfiedLinkError e) {
                return null;
            }
        }
    }

    // ── Public API ───────────────────────────────────────────────────────────

    private TerminalSizeDetector() {}

    /**
     * Auto-selects the best detection strategy for the current OS.
     * Never returns {@code null} — falls back to {@link TerminalSize#FALLBACK}.
     */
    public static TerminalSize detect() {
        TerminalSize size = isWindows() ? detectWindows() : detectPosix();
        if (size != null) return size;
        size = detectFromEnv();
        return size != null ? size : TerminalSize.FALLBACK;
    }

    /**
     * Detects size using Windows {@code GetConsoleScreenBufferInfo}.
     *
     * @return the detected size, or {@code null} if unavailable
     */
    public static TerminalSize detectWindows() {
        try {
            Kernel32Ext k32 = Kernel32Ext.INSTANCE;
            if (k32 == null) return null;

            WinNT.HANDLE stdout = Kernel32.INSTANCE.GetStdHandle(Wincon.STD_OUTPUT_HANDLE);
            if (stdout == null || WinBase.INVALID_HANDLE_VALUE.equals(stdout)) return null;

            ConsoleScreenBufferInfo info = new ConsoleScreenBufferInfo();
            if (!k32.GetConsoleScreenBufferInfo(stdout, info)) return null;

            int cols = info.srWindowRight - info.srWindowLeft + 1;
            int rows = info.srWindowBottom - info.srWindowTop + 1;
            if (cols < 1 || rows < 1) return null;
            return new TerminalSize(cols, rows);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            return null;
        }
    }

    /**
     * Detects size using POSIX {@code ioctl(TIOCGWINSZ)}.
     *
     * @return the detected size, or {@code null} if unavailable
     */
    public static TerminalSize detectPosix() {
        LibC libc = LibC.INSTANCE;
        if (libc == null) return null;

        // Try Linux code first, then macOS
        for (long request : new long[]{TIOCGWINSZ_LINUX, TIOCGWINSZ_MACOS}) {
            try {
                Winsize ws = new Winsize();
                if (libc.ioctl(STDOUT_FD, request, ws) == 0
                        && ws.ws_col > 0 && ws.ws_row > 0) {
                    return new TerminalSize(ws.ws_col, ws.ws_row);
                }
            } catch (Exception ignored) {
                // try next strategy
            }
        }
        return null;
    }

    /**
     * Reads {@code $COLUMNS} and {@code $LINES} environment variables.
     *
     * @return the parsed size, or {@code null} if variables are absent/invalid
     */
    public static TerminalSize detectFromEnv() {
        try {
            String cols = System.getenv("COLUMNS");
            String lines = System.getenv("LINES");
            if (cols == null || lines == null) return null;
            int c = Integer.parseInt(cols.trim());
            int r = Integer.parseInt(lines.trim());
            if (c < 1 || r < 1) return null;
            return new TerminalSize(c, r);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }
}
