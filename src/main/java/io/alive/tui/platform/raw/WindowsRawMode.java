package io.alive.tui.platform.raw;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Wincon;
import com.sun.jna.ptr.IntByReference;

/**
 * Switches the Windows console to raw (non-canonical) input mode.
 *
 * <p>In raw mode:
 * <ul>
 *   <li>Characters are delivered immediately (no line buffering).</li>
 *   <li>Echo is disabled — typed characters are not echoed back.</li>
 *   <li>Virtual-terminal input is enabled ({@code ENABLE_VIRTUAL_TERMINAL_INPUT}),
 *       so arrow keys and function keys are reported as ANSI escape sequences.</li>
 * </ul>
 *
 * <p>The original console mode is saved on {@link #enable()} and restored by
 * {@link #disable()}.  Always call {@link #disable()} (or register a
 * {@link ShutdownHook}) to avoid leaving the user's console in raw mode.
 *
 * @author Jarvis (AI)
 */
public final class WindowsRawMode {

    // Flags to DISABLE (line-input and echo)
    private static final int DISABLE_FLAGS =
            Wincon.ENABLE_LINE_INPUT | Wincon.ENABLE_ECHO_INPUT | Wincon.ENABLE_PROCESSED_INPUT;

    // Flag to ENABLE (VT sequences in input stream)
    private static final int ENABLE_VT_INPUT = 0x0200; // ENABLE_VIRTUAL_TERMINAL_INPUT

    private static volatile Integer savedMode = null;

    private WindowsRawMode() {}

    /**
     * Enables raw mode on {@code STDIN}.
     *
     * @return {@code true} if raw mode was successfully enabled;
     *         {@code false} on non-Windows, unsupported version, or JNA unavailable
     */
    public static boolean enable() {
        if (!isWindows()) return false;
        try {
            WinNT.HANDLE stdin = Kernel32.INSTANCE.GetStdHandle(Wincon.STD_INPUT_HANDLE);
            if (stdin == null || WinBase.INVALID_HANDLE_VALUE.equals(stdin)) return false;

            IntByReference mode = new IntByReference();
            if (!Kernel32.INSTANCE.GetConsoleMode(stdin, mode)) return false;

            savedMode = mode.getValue();
            int newMode = (savedMode & ~DISABLE_FLAGS) | ENABLE_VT_INPUT;
            return Kernel32.INSTANCE.SetConsoleMode(stdin, newMode);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            return false;
        }
    }

    /**
     * Restores the console input mode saved during {@link #enable()}.
     * No-op if {@link #enable()} was not called or failed.
     */
    public static void disable() {
        if (!isWindows() || savedMode == null) return;
        try {
            WinNT.HANDLE stdin = Kernel32.INSTANCE.GetStdHandle(Wincon.STD_INPUT_HANDLE);
            if (stdin != null && !WinBase.INVALID_HANDLE_VALUE.equals(stdin)) {
                Kernel32.INSTANCE.SetConsoleMode(stdin, savedMode);
            }
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ignored) {
            // best effort
        }
        savedMode = null;
    }

    /**
     * Returns {@code true} on Windows where raw mode setup is required.
     */
    public static boolean isSupported() {
        return isWindows();
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }
}
