package io.alive.tui.platform.raw;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Wincon;
import com.sun.jna.ptr.IntByReference;

/**
 * Enables ANSI/VT escape sequence processing on Windows 10+.
 *
 * <p>Without {@code ENABLE_VIRTUAL_TERMINAL_PROCESSING}, all {@code \033[...]}
 * sequences are printed as literal text instead of being interpreted as ANSI.
 * Must be called before any ANSI output is written to stdout.
 *
 * <p>Thread-safe: enable/disable may be called from any thread, but
 * concurrent calls to {@link #enable()} are not idempotent — only the first
 * call saves the original mode.
 *
 * @author Jarvis (AI)
 */
public final class WindowsVtOutput {

    /** Windows Console flag — interpret VT/ANSI escape sequences on output. */
    private static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

    private static volatile Integer savedMode = null;

    private WindowsVtOutput() {}

    /**
     * Enables VT processing on {@code STDOUT}.
     *
     * @return {@code true} if VT processing was enabled successfully;
     *         {@code false} on non-Windows, old Windows without support, or JNA unavailable
     */
    public static boolean enable() {
        if (!isWindows()) return false;
        try {
            WinNT.HANDLE stdout = Kernel32.INSTANCE.GetStdHandle(Wincon.STD_OUTPUT_HANDLE);
            if (stdout == null || WinBase.INVALID_HANDLE_VALUE.equals(stdout)) return false;

            IntByReference mode = new IntByReference();
            if (!Kernel32.INSTANCE.GetConsoleMode(stdout, mode)) return false;

            savedMode = mode.getValue();
            return Kernel32.INSTANCE.SetConsoleMode(stdout, savedMode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            return false;
        }
    }

    /**
     * Restores the original console mode saved during {@link #enable()}.
     * No-op if {@link #enable()} was not called or failed.
     */
    public static void disable() {
        if (!isWindows() || savedMode == null) return;
        try {
            WinNT.HANDLE stdout = Kernel32.INSTANCE.GetStdHandle(Wincon.STD_OUTPUT_HANDLE);
            if (stdout != null && !WinBase.INVALID_HANDLE_VALUE.equals(stdout)) {
                Kernel32.INSTANCE.SetConsoleMode(stdout, savedMode);
            }
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ignored) {
            // best effort
        }
        savedMode = null;
    }

    /**
     * Returns {@code true} on Windows, where VT processing may be required.
     * On Unix/macOS the terminal already processes ANSI natively.
     */
    public static boolean isSupported() {
        return isWindows();
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }
}
