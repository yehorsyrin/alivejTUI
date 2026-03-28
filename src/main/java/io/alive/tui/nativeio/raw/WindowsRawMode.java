package io.alive.tui.nativeio.raw;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

/**
 * Switches the Windows console stdin into raw mode.
 *
 * <p>Raw mode disables:
 * <ul>
 *   <li>{@code ENABLE_ECHO_INPUT}    (0x0004) — characters are not echoed to the console</li>
 *   <li>{@code ENABLE_LINE_INPUT}    (0x0002) — input is available immediately, not line-buffered</li>
 *   <li>{@code ENABLE_PROCESSED_INPUT} (0x0001) — Ctrl+C is not handled by the system</li>
 * </ul>
 *
 * <p>Raw mode enables:
 * <ul>
 *   <li>{@code ENABLE_VIRTUAL_TERMINAL_INPUT} (0x0200) — allows VT sequences in input</li>
 * </ul>
 *
 * <p>Uses the Win32 Console API via JNA (Kernel32 platform binding).
 *
 * @author Jarvis (AI)
 */
public final class WindowsRawMode {

    private static final int ENABLE_PROCESSED_INPUT        = 0x0001;
    private static final int ENABLE_LINE_INPUT             = 0x0002;
    private static final int ENABLE_ECHO_INPUT             = 0x0004;
    private static final int ENABLE_VIRTUAL_TERMINAL_INPUT = 0x0200;

    private static boolean enabled   = false;
    private static int     savedMode = -1;

    private WindowsRawMode() {}

    /**
     * Saves the current stdin console mode and switches to raw mode.
     *
     * @return {@code true} if raw mode was activated; {@code false} if not
     *         supported (non-Windows, non-console handle, or JNA unavailable).
     */
    public static boolean enable() {
        if (!isSupported()) return false;
        try {
            WinNT.HANDLE hIn = Kernel32.INSTANCE.GetStdHandle(-10); // STD_INPUT_HANDLE
            if (hIn == null || hIn.equals(WinNT.INVALID_HANDLE_VALUE)) return false;

            IntByReference mode = new IntByReference();
            if (!Kernel32.INSTANCE.GetConsoleMode(hIn, mode)) return false;

            savedMode = mode.getValue();
            int newMode = (savedMode
                    & ~ENABLE_ECHO_INPUT
                    & ~ENABLE_LINE_INPUT
                    & ~ENABLE_PROCESSED_INPUT)
                    | ENABLE_VIRTUAL_TERMINAL_INPUT;

            if (!Kernel32.INSTANCE.SetConsoleMode(hIn, newMode)) return false;

            enabled = true;
            return true;
        } catch (UnsatisfiedLinkError | Exception e) {
            return false;
        }
    }

    /**
     * Restores the console mode that was saved by {@link #enable()}.
     * If {@link #enable()} was never called or failed, this is a no-op.
     */
    public static void disable() {
        if (!enabled || savedMode == -1) return;
        try {
            WinNT.HANDLE hIn = Kernel32.INSTANCE.GetStdHandle(-10);
            if (hIn != null && !hIn.equals(WinNT.INVALID_HANDLE_VALUE)) {
                Kernel32.INSTANCE.SetConsoleMode(hIn, savedMode);
            }
        } catch (UnsatisfiedLinkError | Exception ignored) {
        } finally {
            enabled   = false;
            savedMode = -1;
        }
    }

    /** Returns {@code true} only on Windows. */
    public static boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /** Returns {@code true} if raw mode is currently active. */
    public static boolean isEnabled() {
        return enabled;
    }
}
