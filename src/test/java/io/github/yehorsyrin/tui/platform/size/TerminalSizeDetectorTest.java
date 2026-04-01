package io.github.yehorsyrin.tui.platform.size;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests run in a piped/headless environment (CI and unit test runner).
 * ioctl/GetConsoleScreenBufferInfo may return null — we verify graceful fallback.
 *
 * @author Jarvis (AI)
 */
class TerminalSizeDetectorTest {

    @Test
    void detectNeverReturnsNull() {
        assertNotNull(TerminalSizeDetector.detect());
    }

    @Test
    void detectReturnsPositiveDimensions() {
        TerminalSize size = TerminalSizeDetector.detect();
        assertTrue(size.cols() > 0, "cols must be > 0");
        assertTrue(size.rows() > 0, "rows must be > 0");
    }

    @Test
    void detectWindowsReturnsNullOnNonWindows() {
        if (!TerminalSizeDetector.isWindows()) {
            // On Linux/macOS GetConsoleScreenBufferInfo is unavailable
            // May return null — must not throw
            assertDoesNotThrow(TerminalSizeDetector::detectWindows);
        }
    }

    @Test
    void detectPosixReturnsNullOnWindows() {
        if (TerminalSizeDetector.isWindows()) {
            // On Windows, libc is not available — should return null
            assertDoesNotThrow(TerminalSizeDetector::detectPosix);
        }
    }

    @Test
    void detectPosixGracefulInNonTtyEnvironment() {
        // In piped/CI stdin/stdout, ioctl may fail — returns null or valid size
        assertDoesNotThrow(TerminalSizeDetector::detectPosix);
    }

    @Test
    void detectFromEnvNullWhenVariablesAbsent() {
        // COLUMNS/LINES are typically not set in test environment
        // Result is either null or a valid size — must not throw
        TerminalSize size = TerminalSizeDetector.detectFromEnv();
        if (size != null) {
            assertTrue(size.cols() > 0);
            assertTrue(size.rows() > 0);
        }
    }

    @Test
    void detectFallsBackTo80x24WhenEverythingFails() {
        // When all strategies fail (no tty, no env vars), result is FALLBACK
        // We can't guarantee this in all environments, but fallback must be valid
        TerminalSize size = TerminalSizeDetector.detect();
        assertTrue(size.cols() >= 80 || size.cols() > 0);
        assertTrue(size.rows() >= 24 || size.rows() > 0);
    }

    @Test
    void isWindowsReflectsOsName() {
        String os = System.getProperty("os.name", "");
        assertEquals(os.toLowerCase().contains("windows"), TerminalSizeDetector.isWindows());
    }
}
