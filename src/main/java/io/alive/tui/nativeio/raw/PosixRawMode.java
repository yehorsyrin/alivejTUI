package io.alive.tui.nativeio.raw;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * Switches the POSIX terminal (stdin) into raw mode.
 *
 * <p>Raw mode disables line-buffering and echo so each keystroke is immediately
 * available to the application.  Achieved via {@code tcgetattr} / {@code tcsetattr}
 * from libc, exposed through JNA.
 *
 * <p>Falls back to a no-op on Windows (use {@link WindowsRawMode} there).
 *
 * @author Jarvis (AI)
 */
public final class PosixRawMode {

    // -- termios field bitmasks (POSIX) --
    private static final int ICANON = 0x0100;  // canonical mode (line buffering)
    private static final int ECHO   = 0x0008;  // echo input chars
    private static final int ISIG   = 0x0080;  // generate signals on Ctrl+C etc.
    private static final int IXON   = 0x0200;  // enable XON/XOFF flow control
    private static final int IEXTEN = 0x8000;  // extended processing

    private static final int VMIN  = 16; // minimum chars for non-canonical read
    private static final int VTIME = 17; // timeout for non-canonical read

    private static boolean enabled   = false;
    private static Termios savedTermios;

    // ---- JNA interface -------------------------------------------------------

    /** Minimal JNA binding to libc termios functions. */
    interface LibC extends Library {
        LibC INSTANCE = loadLibC();

        int tcgetattr(int fd, Termios termios);
        int tcsetattr(int fd, int action, Termios termios);
    }

    @FieldOrder({"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_line", "c_cc"})
    public static class Termios extends Structure {
        public int    c_iflag;
        public int    c_oflag;
        public int    c_cflag;
        public int    c_lflag;
        public byte   c_line;
        public byte[] c_cc = new byte[32];

        public Termios() {}
        public Termios(Termios other) {
            this.c_iflag = other.c_iflag;
            this.c_oflag = other.c_oflag;
            this.c_cflag = other.c_cflag;
            this.c_lflag = other.c_lflag;
            this.c_line  = other.c_line;
            System.arraycopy(other.c_cc, 0, this.c_cc, 0, this.c_cc.length);
        }
    }

    private static final int TCSAFLUSH = 2;
    private static final int STDIN_FD  = 0;

    // ---- Public API ----------------------------------------------------------

    private PosixRawMode() {}

    /**
     * Saves the current terminal attributes and switches stdin to raw mode.
     *
     * @return {@code true} if raw mode was activated; {@code false} if not
     *         supported (Windows, JNA unavailable, or not a tty).
     */
    public static boolean enable() {
        if (!isSupported()) return false;
        try {
            LibC libc = LibC.INSTANCE;
            if (libc == null) return false;

            Termios current = new Termios();
            if (libc.tcgetattr(STDIN_FD, current) != 0) return false;

            savedTermios = new Termios(current);

            // Build raw-mode termios (inspired by cfmakeraw)
            current.c_lflag &= ~(ICANON | ECHO | ISIG | IEXTEN);
            current.c_iflag &= ~IXON;
            current.c_cc[VMIN]  = 1;  // read at least 1 byte
            current.c_cc[VTIME] = 0;  // no timeout

            current.write();
            if (libc.tcsetattr(STDIN_FD, TCSAFLUSH, current) != 0) return false;

            enabled = true;
            return true;
        } catch (UnsatisfiedLinkError | Exception e) {
            return false;
        }
    }

    /**
     * Restores the terminal attributes saved by {@link #enable()}.
     * If {@link #enable()} was never called or failed, this is a no-op.
     */
    public static void disable() {
        if (!enabled || savedTermios == null) return;
        try {
            LibC libc = LibC.INSTANCE;
            if (libc != null) {
                savedTermios.write();
                libc.tcsetattr(STDIN_FD, TCSAFLUSH, savedTermios);
            }
        } catch (UnsatisfiedLinkError | Exception ignored) {
        } finally {
            enabled      = false;
            savedTermios = null;
        }
    }

    /**
     * Returns {@code true} on POSIX platforms (Linux, macOS).
     * Returns {@code false} on Windows.
     */
    public static boolean isSupported() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return !os.contains("win");
    }

    /** Returns {@code true} if raw mode is currently active. */
    public static boolean isEnabled() {
        return enabled;
    }

    // ---- Private helpers ----------------------------------------------------

    private static LibC loadLibC() {
        try {
            return Native.load("c", LibC.class);
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }
}
