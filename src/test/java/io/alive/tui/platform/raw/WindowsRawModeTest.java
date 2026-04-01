package io.alive.tui.platform.raw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jarvis (AI)
 */
class WindowsRawModeTest {

    @Test
    void isSupportedMatchesOs() {
        boolean isWindows = WindowsRawMode.isWindows();
        assertEquals(isWindows, WindowsRawMode.isSupported());
    }

    @Test
    void enableReturnsFalseOnNonWindows() {
        if (!WindowsRawMode.isWindows()) {
            assertFalse(WindowsRawMode.enable(),
                    "enable() must return false on non-Windows");
        }
    }

    @Test
    void disableIsNoOpWhenNeverEnabled() {
        assertDoesNotThrow(WindowsRawMode::disable);
    }

    @Test
    void disableIsNoOpAfterFailedEnable() {
        if (!WindowsRawMode.isWindows()) {
            WindowsRawMode.enable(); // returns false, savedMode stays null
            assertDoesNotThrow(WindowsRawMode::disable);
        }
    }

    @Test
    void doubleDisableIsIdempotent() {
        assertDoesNotThrow(() -> {
            WindowsRawMode.disable();
            WindowsRawMode.disable();
        });
    }

    @Test
    void isWindowsReflectsOsName() {
        String os = System.getProperty("os.name", "");
        boolean expected = os.toLowerCase().contains("windows");
        assertEquals(expected, WindowsRawMode.isWindows());
    }
}
