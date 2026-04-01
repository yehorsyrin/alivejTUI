package io.alive.tui.platform.backend;

import io.alive.tui.backend.LanternaBackend;
import io.alive.tui.backend.MockBackend;
import io.alive.tui.backend.TerminalBackend;

/**
 * Factory for all available {@link TerminalBackend} implementations.
 *
 * <p>Call {@link #createNative()} to get a backend that uses the native
 * platform I/O layer without Lanterna.  On a headless server this runs inside
 * the existing terminal; on a GUI workstation with a display server the caller
 * should use {@link #createSwing()} instead (or let {@link #createAuto()} pick
 * the right one).
 *
 * @author Jarvis (AI)
 */
public final class Backends {

    private Backends() {}

    /**
     * Creates a native terminal backend.
     * Reads from {@code System.in} and writes ANSI escape sequences to
     * {@code System.out}.  Suitable for headless/server environments or when
     * running inside an existing terminal emulator.
     */
    public static TerminalBackend createNative() {
        return NativeTerminalBackend.createAuto();
    }

    /**
     * Creates a Swing-based terminal backend that opens its own window.
     * Suitable for desktop (GUI) environments.
     *
     * @throws UnsupportedOperationException if no display is available
     *         (headless environment — use {@link #createNative()} instead)
     */
    public static TerminalBackend createSwing() {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            throw new UnsupportedOperationException(
                    "createSwing() requires a display. Use createNative() in headless environments.");
        }
        return io.alive.tui.platform.swing.SwingBackend.create();
    }

    /**
     * Auto-selects the best backend for the current environment:
     * <ul>
     *   <li>Headless (server, CI): {@link #createNative()}</li>
     *   <li>GUI desktop: {@link #createSwing()}</li>
     * </ul>
     */
    public static TerminalBackend createAuto() {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return createNative();
        }
        return createSwing();
    }

    /**
     * Creates the Lanterna-based backend (the original implementation).
     * Kept for backwards compatibility and migration path.
     */
    public static TerminalBackend createLanterna() {
        return new LanternaBackend();
    }

    /**
     * Creates a mock backend for unit testing.
     *
     * @param width  simulated terminal width
     * @param height simulated terminal height
     */
    public static TerminalBackend createMock(int width, int height) {
        return new MockBackend(width, height);
    }
}
