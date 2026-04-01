package io.github.yehorsyrin.tui.platform.raw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jarvis (AI)
 */
class WindowsVtOutputTest {

    @Test
    void isSupportedMatchesOs() {
        boolean isWindows = WindowsVtOutput.isWindows();
        assertEquals(isWindows, WindowsVtOutput.isSupported());
    }

    @Test
    void enableReturnsFalseOnNonWindows() {
        if (!WindowsVtOutput.isWindows()) {
            assertFalse(WindowsVtOutput.enable(),
                    "enable() must return false on non-Windows");
        }
    }

    @Test
    void disableIsNoOpWhenNeverEnabled() {
        // Must not throw regardless of OS
        assertDoesNotThrow(WindowsVtOutput::disable);
    }

    @Test
    void disableIsNoOpAfterFailedEnable() {
        if (!WindowsVtOutput.isWindows()) {
            WindowsVtOutput.enable(); // returns false, savedMode stays null
            assertDoesNotThrow(WindowsVtOutput::disable);
        }
    }

    @Test
    void doubleDisableIsIdempotent() {
        assertDoesNotThrow(() -> {
            WindowsVtOutput.disable();
            WindowsVtOutput.disable();
        });
    }

    @Test
    void isWindowsReflectsOsName() {
        String os = System.getProperty("os.name", "");
        boolean expected = os.toLowerCase().contains("windows");
        assertEquals(expected, WindowsVtOutput.isWindows());
    }
}
