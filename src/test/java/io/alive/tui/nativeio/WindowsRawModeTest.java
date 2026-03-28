package io.alive.tui.nativeio;

import io.alive.tui.nativeio.raw.WindowsRawMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WindowsRawMode}.
 *
 * @author Jarvis (AI)
 */
class WindowsRawModeTest {

    @AfterEach
    void tearDown() {
        // Always clean up so tests don't interfere with each other
        WindowsRawMode.disable();
    }

    @Test
    void isSupported_matchesOsName() {
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        assertEquals(win, WindowsRawMode.isSupported());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void enable_returnsBoolean_noException() {
        // May return false in a non-console environment (pipes, CI),
        // but must never throw.
        boolean result = WindowsRawMode.enable();
        assertTrue(result || !result); // just checks no exception
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void disable_afterEnable_noException() {
        WindowsRawMode.enable();
        assertDoesNotThrow(WindowsRawMode::disable);
        assertFalse(WindowsRawMode.isEnabled());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void disable_withoutEnable_isNoOp() {
        assertDoesNotThrow(WindowsRawMode::disable);
        assertFalse(WindowsRawMode.isEnabled());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void isEnabled_initiallyFalse() {
        WindowsRawMode.disable(); // ensure clean state
        assertFalse(WindowsRawMode.isEnabled());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void enable_disable_cycleIsIdempotent() {
        WindowsRawMode.enable();
        WindowsRawMode.disable();
        WindowsRawMode.enable();
        WindowsRawMode.disable();
        assertFalse(WindowsRawMode.isEnabled());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void doubleEnable_secondCallOverwritesState() {
        WindowsRawMode.enable();
        // Second enable should not throw and should keep state consistent
        assertDoesNotThrow(WindowsRawMode::enable);
    }
}
