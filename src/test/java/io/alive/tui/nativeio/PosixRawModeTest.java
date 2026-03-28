package io.alive.tui.nativeio;

import io.alive.tui.nativeio.raw.PosixRawMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PosixRawMode}.
 *
 * @author Jarvis (AI)
 */
class PosixRawModeTest {

    @AfterEach
    void tearDown() {
        PosixRawMode.disable();
    }

    @Test
    void isSupported_windows_false() {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            assertFalse(PosixRawMode.isSupported());
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void isSupported_nonWindows_true() {
        assertTrue(PosixRawMode.isSupported());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void enable_onWindows_returnsFalse() {
        assertFalse(PosixRawMode.enable());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void isEnabled_onWindows_alwaysFalse() {
        PosixRawMode.enable();
        assertFalse(PosixRawMode.isEnabled());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void disable_onWindows_noException() {
        assertDoesNotThrow(PosixRawMode::disable);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void enable_onPosix_noException() {
        // May return false if not a tty (CI pipe), but must not throw
        assertDoesNotThrow(PosixRawMode::enable);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void disable_afterEnable_noException() {
        PosixRawMode.enable();
        assertDoesNotThrow(PosixRawMode::disable);
        assertFalse(PosixRawMode.isEnabled());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void disable_withoutEnable_noException() {
        assertDoesNotThrow(PosixRawMode::disable);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void enable_disable_cycleIsIdempotent() {
        PosixRawMode.enable();
        PosixRawMode.disable();
        PosixRawMode.enable();
        PosixRawMode.disable();
        assertFalse(PosixRawMode.isEnabled());
    }
}
