package io.alive.tui.nativeio.backend;

import io.alive.tui.backend.MockBackend;
import io.alive.tui.backend.TerminalBackend;

/**
 * Factory for all available {@link TerminalBackend} implementations.
 *
 * <p>Central entry point for selecting a backend at startup:
 * <pre>
 *   AliveJTUI.run(new MyApp(), Backends.createNative());
 * </pre>
 *
 * @author Jarvis (AI)
 */
public final class Backends {

    private Backends() {}

    /**
     * Creates a native backend appropriate for the current platform.
     *
     * <p>On Windows: enables VT processing and raw console mode via Win32 API.
     * On POSIX:   enables raw mode via libc tcsetattr.
     *
     * @return a new {@link NativeBackend} instance
     */
    public static TerminalBackend createNative() {
        if (isWindows()) return NativeBackend.createWindows();
        return NativeBackend.createPosix();
    }

    /**
     * Creates the Lanterna-based backend.
     *
     * @return a new {@code LanternaBackend}
     * @deprecated Will be removed once the native backend is stable.
     *             Use {@link #createNative()} instead.
     */
    @Deprecated
    public static TerminalBackend createLanterna() {
        return new io.alive.tui.backend.LanternaBackend();
    }

    /**
     * Creates an in-memory mock backend for testing.
     *
     * @param width  terminal width in columns
     * @param height terminal height in rows
     * @return a new {@link MockBackend}
     */
    public static TerminalBackend createMock(int width, int height) {
        return new MockBackend(width, height);
    }

    // ---

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
