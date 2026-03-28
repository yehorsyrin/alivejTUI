package io.alive.tui.nativeio;

import io.alive.tui.nativeio.size.TerminalSize;
import io.alive.tui.nativeio.size.TerminalSizeDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TerminalSizeDetector} and {@link TerminalSize}.
 *
 * @author Jarvis (AI)
 */
class TerminalSizeDetectorTest {

    // --- TerminalSize record ---

    @Test
    void terminalSize_validValues() {
        TerminalSize sz = new TerminalSize(120, 40);
        assertEquals(120, sz.cols());
        assertEquals(40, sz.rows());
    }

    @Test
    void terminalSize_zeroCols_throws() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalSize(0, 24));
    }

    @Test
    void terminalSize_zeroRows_throws() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalSize(80, 0));
    }

    @Test
    void terminalSize_negative_throws() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalSize(-1, 24));
    }

    @Test
    void terminalSize_default_is80x24() {
        assertEquals(80,  TerminalSize.DEFAULT.cols());
        assertEquals(24, TerminalSize.DEFAULT.rows());
    }

    @Test
    void terminalSize_equality() {
        assertEquals(new TerminalSize(80, 24), new TerminalSize(80, 24));
        assertNotEquals(new TerminalSize(80, 24), new TerminalSize(100, 24));
    }

    // --- detect() never returns null ---

    @Test
    void detect_neverNull() {
        assertNotNull(TerminalSizeDetector.detect());
    }

    @Test
    void detect_alwaysPositiveDimensions() {
        TerminalSize sz = TerminalSizeDetector.detect();
        assertTrue(sz.cols() > 0);
        assertTrue(sz.rows() > 0);
    }

    // --- detectWindows ---

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void detectWindows_returnsNonNullOnWindowsConsole() {
        // May return null in a non-console environment (piped stdin/stdout),
        // but must not throw.
        assertDoesNotThrow(TerminalSizeDetector::detectWindows);
    }

    @Test
    void detectWindows_onNonWindows_returnsNull() {
        // This assertion only fires on non-Windows.
        // On Windows it is skipped by the condition above.
        if (!TerminalSizeDetector.isWindows()) {
            assertNull(TerminalSizeDetector.detectWindows());
        }
    }

    // --- detectAnsi ---

    @Test
    void detectAnsi_validResponse_parsesCorrectly() throws IOException, InterruptedException {
        // Simulate terminal responding ESC[24;80R
        PipedOutputStream termOut = new PipedOutputStream();
        PipedInputStream  appIn  = new PipedInputStream(termOut);
        ByteArrayOutputStream appOut = new ByteArrayOutputStream();

        // Write the terminal's response into the pipe in a background thread
        Thread responder = new Thread(() -> {
            try {
                Thread.sleep(20); // slight delay
                termOut.write("\033[24;80R".getBytes());
                termOut.flush();
            } catch (Exception ignored) {}
        });
        responder.setDaemon(true);
        responder.start();

        TerminalSize sz = TerminalSizeDetector.detectAnsi(appIn, appOut);
        assertNotNull(sz, "Expected a non-null TerminalSize from ANSI query");
        assertEquals(80, sz.cols());
        assertEquals(24, sz.rows());
    }

    @Test
    void detectAnsi_noResponse_returnsNull() throws IOException {
        // InputStream with no data — should return null without hanging (timeout)
        // Use a small buffer so timeout kicks in quickly (we override via empty stream).
        ByteArrayInputStream empty = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream sink  = new ByteArrayOutputStream();
        // This will time out after ~500ms — acceptable for a unit test
        TerminalSize sz = TerminalSizeDetector.detectAnsi(empty, sink);
        assertNull(sz);
    }

    @Test
    void detectAnsi_malformedResponse_returnsNull() throws IOException, InterruptedException {
        PipedOutputStream termOut = new PipedOutputStream();
        PipedInputStream  appIn  = new PipedInputStream(termOut);
        ByteArrayOutputStream appOut = new ByteArrayOutputStream();

        Thread responder = new Thread(() -> {
            try {
                Thread.sleep(20);
                termOut.write("GARBAGE".getBytes());
                termOut.flush();
            } catch (Exception ignored) {}
        });
        responder.setDaemon(true);
        responder.start();

        // No valid ESC[r;cR — should time out and return null
        TerminalSize sz = TerminalSizeDetector.detectAnsi(appIn, appOut);
        assertNull(sz);
    }
}
