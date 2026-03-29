package io.github.yehorsyrin.tui.debug;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DebugLogger}.
 *
 * @author Jarvis (AI)
 */
class DebugLoggerTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        DebugLogger.disable();
    }

    @Test
    void enable_createsLogFile() {
        String path = tempDir.resolve("test.log").toString();
        DebugLogger.enable(path);
        assertTrue(Files.exists(Path.of(path)));
    }

    @Test
    void isEnabled_falseBeforeEnable() {
        assertFalse(DebugLogger.isEnabled());
    }

    @Test
    void isEnabled_trueAfterEnable() {
        DebugLogger.enable(tempDir.resolve("a.log").toString());
        assertTrue(DebugLogger.isEnabled());
    }

    @Test
    void isEnabled_falseAfterDisable() {
        DebugLogger.enable(tempDir.resolve("b.log").toString());
        DebugLogger.disable();
        assertFalse(DebugLogger.isEnabled());
    }

    @Test
    void logMessage_writesToFile() throws IOException {
        Path log = tempDir.resolve("msg.log");
        DebugLogger.enable(log.toString());
        DebugLogger.logMessage("hello world");
        DebugLogger.disable();

        String content = Files.readString(log);
        assertTrue(content.contains("hello world"));
    }

    @Test
    void logRender_writesRenderLine() throws IOException {
        Path log = tempDir.resolve("render.log");
        DebugLogger.enable(log.toString());
        DebugLogger.logRender(42, 10);
        DebugLogger.disable();

        String content = Files.readString(log);
        assertTrue(content.contains("RENDER"));
        assertTrue(content.contains("changed=42"));
        assertTrue(content.contains("nodes=10"));
    }

    @Test
    void logEvent_writesEventLine() throws IOException {
        Path log = tempDir.resolve("event.log");
        DebugLogger.enable(log.toString());
        DebugLogger.logEvent("KEY_ENTER");
        DebugLogger.disable();

        String content = Files.readString(log);
        assertTrue(content.contains("EVENT"));
        assertTrue(content.contains("KEY_ENTER"));
    }

    @Test
    void logError_writesExceptionInfo() throws IOException {
        Path log = tempDir.resolve("error.log");
        DebugLogger.enable(log.toString());
        DebugLogger.logError("testLabel", new RuntimeException("kaboom"));
        DebugLogger.disable();

        String content = Files.readString(log);
        assertTrue(content.contains("ERROR"));
        assertTrue(content.contains("testLabel"));
        assertTrue(content.contains("kaboom"));
    }

    @Test
    void disable_whenAlreadyDisabled_doesNotThrow() {
        assertDoesNotThrow(DebugLogger::disable);
    }

    @Test
    void enable_nullPath_throws() {
        assertThrows(IllegalArgumentException.class, () -> DebugLogger.enable(null));
    }

    @Test
    void enable_blankPath_throws() {
        assertThrows(IllegalArgumentException.class, () -> DebugLogger.enable("  "));
    }

    @Test
    void logMethods_whenDisabled_doNothing() {
        // All log methods should be no-ops when not enabled
        assertDoesNotThrow(() -> {
            DebugLogger.logMessage("ignored");
            DebugLogger.logRender(1, 1);
            DebugLogger.logEvent("ignored");
            DebugLogger.logError("ignored", new RuntimeException());
        });
    }

    @Test
    void enable_calledTwice_closesFirstLogger() throws IOException {
        Path log1 = tempDir.resolve("first.log");
        Path log2 = tempDir.resolve("second.log");
        DebugLogger.enable(log1.toString());
        DebugLogger.logMessage("in first");
        DebugLogger.enable(log2.toString());  // should close first
        DebugLogger.logMessage("in second");
        DebugLogger.disable();

        assertTrue(Files.readString(log1).contains("in first"));
        assertTrue(Files.readString(log2).contains("in second"));
    }
}
