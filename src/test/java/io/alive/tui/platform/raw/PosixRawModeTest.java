package io.alive.tui.platform.raw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests run on CI (Linux/macOS) and developer machines (Windows).
 * Actual tcgetattr/tcsetattr calls require a real tty — in a non-tty
 * environment (piped stdin) they return -1, which we treat as a graceful
 * "unsupported" rather than an error.
 *
 * @author Jarvis (AI)
 */
class PosixRawModeTest {

    @Test
    void isSupportedReturnsFalseOnWindows() {
        if (PosixRawMode.isWindows()) {
            assertFalse(PosixRawMode.isSupported());
        }
    }

    @Test
    void isSupportedReturnsTrueOnNonWindows() {
        if (!PosixRawMode.isWindows()) {
            assertTrue(PosixRawMode.isSupported());
        }
    }

    @Test
    void enableReturnsFalseOnWindows() {
        if (PosixRawMode.isWindows()) {
            assertFalse(PosixRawMode.enable());
        }
    }

    @Test
    void enableOnNonTtyReturnsFalseGracefully() {
        // In a CI/test environment stdin is piped, not a tty.
        // tcgetattr returns -1 → enable() returns false without throwing.
        if (!PosixRawMode.isWindows()) {
            boolean result = PosixRawMode.enable();
            // result may be true (real tty) or false (piped) — must not throw
            if (result) {
                PosixRawMode.disable(); // restore immediately
            }
        }
    }

    @Test
    void disableIsNoOpWhenNeverEnabled() {
        assertDoesNotThrow(PosixRawMode::disable);
    }

    @Test
    void disableIsNoOpAfterFailedEnable() {
        if (!PosixRawMode.isWindows()) {
            // If stdin is piped, enable() returns false → savedTermios is null
            PosixRawMode.enable();
            assertDoesNotThrow(PosixRawMode::disable);
        }
    }

    @Test
    void doubleDisableIsIdempotent() {
        assertDoesNotThrow(() -> {
            PosixRawMode.disable();
            PosixRawMode.disable();
        });
    }

    @Test
    void enableDisableCycleDoesNotThrow() {
        assertDoesNotThrow(() -> {
            boolean enabled = PosixRawMode.enable();
            if (enabled) PosixRawMode.disable();
        });
    }

    @Test
    void isWindowsReflectsOsName() {
        String os = System.getProperty("os.name", "");
        assertEquals(os.toLowerCase().contains("windows"), PosixRawMode.isWindows());
    }

    @Test
    void termiosCopyConstructorIsDeepCopy() {
        PosixRawMode.Termios original = new PosixRawMode.Termios();
        original.c_iflag = 42;
        original.c_lflag = 99;
        original.c_cc[0] = 7;

        PosixRawMode.Termios copy = new PosixRawMode.Termios(original);
        assertEquals(42, copy.c_iflag);
        assertEquals(99, copy.c_lflag);
        assertEquals(7, copy.c_cc[0]);

        // Mutating copy must not affect original
        copy.c_iflag = 0;
        copy.c_cc[0] = 0;
        assertEquals(42, original.c_iflag);
        assertEquals(7, original.c_cc[0]);
    }
}
