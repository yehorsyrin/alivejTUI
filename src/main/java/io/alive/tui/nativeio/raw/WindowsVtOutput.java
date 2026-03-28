package io.alive.tui.nativeio.raw;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

/**
 * Enables ANSI / VT escape code processing on Windows 10+ stdout.
 *
 * Without this, writing {@code \033[...} sequences to stdout produces literal
 * text instead of terminal control codes.  Must be called before any ANSI
 * output is written.
 *
 * Uses the Win32 Console API via JNA (Kernel32 platform binding).
 *
 * @author Jarvis (AI)
 */
public final class WindowsVtOutput {

    /** {@code ENABLE_VIRTUAL_TERMINAL_PROCESSING} flag (0x0004). */
    private static final int ENABLE_VT_PROCESSING = 0x0004;

    private static boolean enabled = false;
    private static int savedMode = -1;

    private WindowsVtOutput() {}

    /**
     * Enables VT processing on stdout.
     *
     * @return {@code true} if successfully enabled; {@code false} if not
     *         supported (older Windows or not a console handle).
     */
    public static boolean enable() {
        if (!isSupported()) return false;
        try {
            WinNT.HANDLE hOut = Kernel32.INSTANCE.GetStdHandle(-11); // STD_OUTPUT_HANDLE
            if (hOut == null || hOut.equals(WinNT.INVALID_HANDLE_VALUE)) return false;

            IntByReference mode = new IntByReference();
            if (!Kernel32.INSTANCE.GetConsoleMode(hOut, mode)) return false;

            savedMode = mode.getValue();
            int newMode = savedMode | ENABLE_VT_PROCESSING;
            if (!Kernel32.INSTANCE.SetConsoleMode(hOut, newMode)) return false;

            enabled = true;
            return true;
        } catch (UnsatisfiedLinkError | Exception e) {
            return false;
        }
    }

    /**
     * Restores the original console mode (disables VT processing if it was
     * not active before {@link #enable()} was called).
     */
    public static void disable() {
        if (!enabled || savedMode == -1) return;
        try {
            WinNT.HANDLE hOut = Kernel32.INSTANCE.GetStdHandle(-11);
            if (hOut != null && !hOut.equals(WinNT.INVALID_HANDLE_VALUE)) {
                Kernel32.INSTANCE.SetConsoleMode(hOut, savedMode);
            }
        } catch (UnsatisfiedLinkError | Exception ignored) {
        } finally {
            enabled = false;
            savedMode = -1;
        }
    }

    /** Returns {@code true} only on Windows (where kernel32.dll is available). */
    public static boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /** Returns {@code true} if VT processing is currently active. */
    public static boolean isEnabled() {
        return enabled;
    }
}
