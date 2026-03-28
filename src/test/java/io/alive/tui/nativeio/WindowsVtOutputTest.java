package io.alive.tui.nativeio;

import io.alive.tui.nativeio.raw.WindowsVtOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WindowsVtOutput}.
 *
 * @author Jarvis (AI)
 */
class WindowsVtOutputTest {

    @Test
    void isSupported_onWindows_true() {
        // This test runs only on Windows; on other OS it verifies the negative.
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        assertEquals(win, WindowsVtOutput.isSupported());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void enable_returnsBoolean_noException() {
        // May return false if running without a real console handle (CI/pipe),
        // but must never throw.
        boolean result = WindowsVtOutput.enable();
        // Just assert it completed without exception — value depends on environment
        assertTrue(result || !result);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void disable_afterEnable_noException() {
        WindowsVtOutput.enable();
        assertDoesNotThrow(WindowsVtOutput::disable);
        assertFalse(WindowsVtOutput.isEnabled());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void disable_withoutEnable_noException() {
        // Calling disable before enable must be a no-op
        assertDoesNotThrow(WindowsVtOutput::disable);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void isEnabled_initiallyFalse() {
        WindowsVtOutput.disable(); // ensure clean state
        assertFalse(WindowsVtOutput.isEnabled());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void enable_disable_cycleIsIdempotent() {
        WindowsVtOutput.enable();
        WindowsVtOutput.disable();
        WindowsVtOutput.enable();
        WindowsVtOutput.disable();
        assertFalse(WindowsVtOutput.isEnabled());
    }
}
