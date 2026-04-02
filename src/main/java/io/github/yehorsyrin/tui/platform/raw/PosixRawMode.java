package io.github.yehorsyrin.tui.platform.raw;

import java.util.concurrent.atomic.AtomicReference;

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
 * <h2>Platform struct layouts</h2>
 * <pre>
 *  Linux x86-64   : c_iflag c_oflag c_cflag c_lflag c_line c_cc[32] c_ispeed c_ospeed → 60 bytes
 *  macOS arm64/x86: c_iflag c_oflag c_cflag c_lflag c_cc[20] c_ispeed c_ospeed        → 44 bytes
 * </pre>
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

    // ── c_cc indices (platform-specific) ────────────────────────────────────

    // Linux: VTIME=5, VMIN=6   |   macOS: VTIME=17, VMIN=16
    private static final int VTIME_IDX = isLinux() ? 5  : 17;
    private static final int VMIN_IDX  = isLinux() ? 6  : 16;

    // ── JNA interface ────────────────────────────────────────────────────────

    interface LibC extends Library {
        LibC INSTANCE = loadLibC();

        int tcgetattr(int fd, Structure termios);
        int tcsetattr(int fd, int action, Structure termios);

        @SuppressWarnings("unused")
        private static LibC loadLibC() {
            try {
                return Native.load("c", LibC.class);
            } catch (UnsatisfiedLinkError e) {
                return null;
            }
        }
    }

    // ── Platform-specific JNA structs ────────────────────────────────────────

    /**
     * Linux x86-64 {@code struct termios}: 4 flag ints + c_line byte
     * + c_cc[32] + c_ispeed + c_ospeed = 60 bytes.
     */
    @FieldOrder({"c_iflag", "c_oflag", "c_cflag", "c_lflag",
                 "c_line", "c_cc", "c_ispeed", "c_ospeed"})
    public static class TermiosLinux extends Structure {
        public int    c_iflag, c_oflag, c_cflag, c_lflag;
        public byte   c_line;
        public byte[] c_cc = new byte[32]; // NCCS = 32 on Linux
        public int    c_ispeed, c_ospeed;
    }

    /**
     * macOS {@code struct termios}: 4 flag ints + c_cc[20]
     * + c_ispeed + c_ospeed = 44 bytes (no c_line).
     */
    @FieldOrder({"c_iflag", "c_oflag", "c_cflag", "c_lflag",
                 "c_cc", "c_ispeed", "c_ospeed"})
    public static class TermiosMacOS extends Structure {
        public int    c_iflag, c_oflag, c_cflag, c_lflag;
        public byte[] c_cc = new byte[20]; // NCCS = 20 on macOS
        public int    c_ispeed, c_ospeed;
    }

    // ── Public API struct (used by tests and deep-copy) ──────────────────────

    /**
     * Platform-neutral view of the terminal attributes used by this class.
     * Not a JNA struct — conversion to/from the JNA structs happens inside
     * {@link #enable()} and {@link #disable()}.
     */
    public static class Termios {
        public int    c_iflag;
        public int    c_oflag;
        public int    c_cflag;
        public int    c_lflag;
        public byte[] c_cc = new byte[32]; // sized for Linux; macOS uses [0..19]

        public Termios() {}

        /** Deep copy constructor. */
        public Termios(Termios src) {
            this.c_iflag = src.c_iflag;
            this.c_oflag = src.c_oflag;
            this.c_cflag = src.c_cflag;
            this.c_lflag = src.c_lflag;
            this.c_cc    = src.c_cc.clone();
        }
    }

    // ── State ────────────────────────────────────────────────────────────────

    private static final AtomicReference<Termios> savedTermios = new AtomicReference<>();

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

        if (isLinux()) {
            TermiosLinux raw = new TermiosLinux();
            if (libc.tcgetattr(STDIN_FD, raw) != 0) return false;
            raw.read();

            savedTermios.set(fromLinux(raw));

            raw.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
            raw.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
            raw.c_cflag &= ~(CSIZE | PARENB);
            raw.c_cflag |= CS8;
            raw.c_cc[VMIN_IDX]  = 1;
            raw.c_cc[VTIME_IDX] = 0;
            raw.write();

            if (libc.tcsetattr(STDIN_FD, TCSANOW, raw) != 0) {
                savedTermios.set(null);
                return false;
            }
        } else { // macOS
            TermiosMacOS raw = new TermiosMacOS();
            if (libc.tcgetattr(STDIN_FD, raw) != 0) return false;
            raw.read();

            savedTermios.set(fromMacOS(raw));

            raw.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
            raw.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
            raw.c_cflag &= ~(CSIZE | PARENB);
            raw.c_cflag |= CS8;
            if (VMIN_IDX < raw.c_cc.length)  raw.c_cc[VMIN_IDX]  = 1;
            if (VTIME_IDX < raw.c_cc.length) raw.c_cc[VTIME_IDX] = 0;
            raw.write();

            if (libc.tcsetattr(STDIN_FD, TCSANOW, raw) != 0) {
                savedTermios.set(null);
                return false;
            }
        }
        return true;
    }

    /**
     * Restores the original {@code termios} saved during {@link #enable()}.
     * No-op if {@link #enable()} was not called or failed.
     */
    public static void disable() {
        Termios saved = savedTermios.get();
        if (!isSupported() || saved == null) return;
        LibC libc = LibC.INSTANCE;
        if (libc == null) return;

        if (isLinux()) {
            TermiosLinux restore = toLinux(saved);
            libc.tcsetattr(STDIN_FD, TCSANOW, restore);
        } else {
            TermiosMacOS restore = toMacOS(saved);
            libc.tcsetattr(STDIN_FD, TCSANOW, restore);
        }
        savedTermios.set(null);
    }

    /** Returns {@code true} on non-Windows platforms where POSIX raw mode applies. */
    public static boolean isSupported() {
        return !isWindows();
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase().contains("linux");
    }

    // ── Conversion helpers ───────────────────────────────────────────────────

    private static Termios fromLinux(TermiosLinux s) {
        Termios t = new Termios();
        t.c_iflag = s.c_iflag; t.c_oflag = s.c_oflag;
        t.c_cflag = s.c_cflag; t.c_lflag = s.c_lflag;
        System.arraycopy(s.c_cc, 0, t.c_cc, 0, Math.min(s.c_cc.length, t.c_cc.length));
        return t;
    }

    private static TermiosLinux toLinux(Termios t) {
        TermiosLinux s = new TermiosLinux();
        s.c_iflag = t.c_iflag; s.c_oflag = t.c_oflag;
        s.c_cflag = t.c_cflag; s.c_lflag = t.c_lflag;
        System.arraycopy(t.c_cc, 0, s.c_cc, 0, Math.min(t.c_cc.length, s.c_cc.length));
        return s;
    }

    private static Termios fromMacOS(TermiosMacOS s) {
        Termios t = new Termios();
        t.c_iflag = s.c_iflag; t.c_oflag = s.c_oflag;
        t.c_cflag = s.c_cflag; t.c_lflag = s.c_lflag;
        System.arraycopy(s.c_cc, 0, t.c_cc, 0, Math.min(s.c_cc.length, t.c_cc.length));
        return t;
    }

    private static TermiosMacOS toMacOS(Termios t) {
        TermiosMacOS s = new TermiosMacOS();
        s.c_iflag = t.c_iflag; s.c_oflag = t.c_oflag;
        s.c_cflag = t.c_cflag; s.c_lflag = t.c_lflag;
        System.arraycopy(t.c_cc, 0, s.c_cc, 0, Math.min(t.c_cc.length, s.c_cc.length));
        return s;
    }
}
