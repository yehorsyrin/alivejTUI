package io.alive.tui.platform.raw;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * Switches POSIX terminals (Linux, macOS) to raw (non-canonical) input mode
 * via {@code tcgetattr} / {@code tcsetattr} from libc.
 *
 * <p>In raw mode the terminal delivers every keystroke immediately without
 * buffering, and does not echo typed characters back.  This is required for
 * interactive TUI applications.
 *
 * <p>The original {@code termios} struct is saved on {@link #enable()} and
 * restored by {@link #disable()}.  Always call {@link #disable()} (or register
 * a {@link ShutdownHook}) — leaving the terminal in raw mode makes the shell
 * unusable until the user runs {@code reset}.
 *
 * @author Jarvis (AI)
 */
public final class PosixRawMode {

    // ── termios flags ────────────────────────────────────────────────────────

    // c_iflag bits
    private static final int IGNBRK  = 0x00000001;
    private static final int BRKINT  = 0x00000002;
    private static final int PARMRK  = 0x00000008;
    private static final int ISTRIP  = 0x00000020;
    private static final int INLCR   = 0x00000040;
    private static final int IGNCR   = 0x00000080;
    private static final int ICRNL   = 0x00000100;
    private static final int IXON    = 0x00000400;

    // c_lflag bits
    private static final int ECHO    = 0x00000008;
    private static final int ECHONL  = 0x00000040;
    private static final int ICANON  = 0x00000002;
    private static final int ISIG    = 0x00000001;
    private static final int IEXTEN  = 0x00008000;

    // c_cflag bits
    private static final int CSIZE   = 0x00000030;
    private static final int PARENB  = 0x00000100;
    private static final int CS8     = 0x00000030;

    // tcsetattr action
    private static final int TCSANOW = 0;

    // STDIN file descriptor
    private static final int STDIN_FD = 0;

    // ── JNA interfaces ───────────────────────────────────────────────────────

    interface LibC extends Library {
        LibC INSTANCE = loadLibC();

        int tcgetattr(int fd, Termios termios);
        int tcsetattr(int fd, int action, Termios termios);

        @SuppressWarnings("unused")
        private static LibC loadLibC() {
            try {
                return Native.load("c", LibC.class);
            } catch (UnsatisfiedLinkError e) {
                return null; // Windows or JNA unavailable
            }
        }
    }

    /**
     * Portable {@code struct termios}.  The layout matches both Linux x86-64
     * and macOS (arm64 / x86-64): 4 ints for flags + 20-byte c_cc array.
     * The struct is intentionally larger than required on some platforms —
     * extra bytes are ignored by the kernel.
     */
    @FieldOrder({"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc"})
    public static class Termios extends Structure {
        public int c_iflag;
        public int c_oflag;
        public int c_cflag;
        public int c_lflag;
        public byte[] c_cc = new byte[20];

        public Termios() { super(); }

        /** Deep copy constructor. */
        public Termios(Termios src) {
            this.c_iflag = src.c_iflag;
            this.c_oflag = src.c_oflag;
            this.c_cflag = src.c_cflag;
            this.c_lflag = src.c_lflag;
            this.c_cc = src.c_cc.clone();
        }
    }

    // ── State ────────────────────────────────────────────────────────────────

    private static volatile Termios savedTermios = null;

    private PosixRawMode() {}

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Enables raw mode on {@code STDIN}.
     *
     * @return {@code true} if raw mode was successfully enabled;
     *         {@code false} on Windows, if libc is unavailable, or if
     *         stdin is not a real tty (e.g. piped input in tests)
     */
    public static boolean enable() {
        if (!isSupported()) return false;
        LibC libc = LibC.INSTANCE;
        if (libc == null) return false;

        Termios current = new Termios();
        if (libc.tcgetattr(STDIN_FD, current) != 0) return false;

        savedTermios = new Termios(current);

        // Apply POSIX "cfmakeraw" equivalent
        current.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
        current.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
        current.c_cflag &= ~(CSIZE | PARENB);
        current.c_cflag |= CS8;
        // Read returns after 1 byte with no timeout
        if (current.c_cc.length > 6) {
            current.c_cc[6] = 1; // VMIN  = 1
            current.c_cc[5] = 0; // VTIME = 0
        }

        if (libc.tcsetattr(STDIN_FD, TCSANOW, current) != 0) {
            savedTermios = null;
            return false;
        }
        return true;
    }

    /**
     * Restores the original {@code termios} saved during {@link #enable()}.
     * No-op if {@link #enable()} was not called or failed.
     */
    public static void disable() {
        if (!isSupported() || savedTermios == null) return;
        LibC libc = LibC.INSTANCE;
        if (libc != null) {
            libc.tcsetattr(STDIN_FD, TCSANOW, savedTermios);
        }
        savedTermios = null;
    }

    /**
     * Returns {@code true} on non-Windows platforms where POSIX raw mode applies.
     */
    public static boolean isSupported() {
        return !isWindows();
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }
}
