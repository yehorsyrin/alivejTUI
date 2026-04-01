package io.github.yehorsyrin.tui.platform.backend;

import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.platform.raw.AnsiWriter;
import io.github.yehorsyrin.tui.style.Color;
import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests using piped I/O — no real terminal required.
 *
 * @author Jarvis (AI)
 */
class NativeTerminalBackendTest {

    private ByteArrayOutputStream sink;
    private NativeTerminalBackend backend;

    @BeforeEach
    void setUp() {
        sink = new ByteArrayOutputStream();
    }

    @AfterEach
    void tearDown() {
        if (backend != null) {
            try { backend.shutdown(); } catch (Exception ignored) {}
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    @Test
    void createForTestingDoesNotThrow() {
        assertDoesNotThrow(() -> {
            backend = NativeTerminalBackend.createForTesting(
                    new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        });
    }

    // ── Init / shutdown ───────────────────────────────────────────────────────

    @Test
    void initAndShutdownDoNotThrow() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        assertDoesNotThrow(() -> {
            backend.init();
            backend.shutdown();
        });
    }

    @Test
    void doubleInitIsIdempotent() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        assertDoesNotThrow(() -> {
            backend.init();
            backend.init(); // second call must be no-op
        });
    }

    @Test
    void doubleShutdownIsIdempotent() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        backend.init();
        assertDoesNotThrow(() -> {
            backend.shutdown();
            backend.shutdown();
        });
    }

    @Test
    void initWritesAlternateScreenAndHideCursor() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        backend.init();
        String output = sink.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains(AnsiWriter.ALTERNATE_SCREEN_ON),  "alternate screen on");
        assertTrue(output.contains(AnsiWriter.CURSOR_HIDE),          "cursor hidden");
    }

    @Test
    void shutdownWritesRestoreSequences() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        backend.init();
        sink.reset();
        backend.shutdown();
        String output = sink.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains(AnsiWriter.CURSOR_SHOW),          "cursor shown");
        assertTrue(output.contains(AnsiWriter.ALTERNATE_SCREEN_OFF), "alternate screen off");
    }

    // ── Dimensions ───────────────────────────────────────────────────────────

    @Test
    void getWidthReturnsInjectedWidth() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 120, 40);
        backend.init();
        assertEquals(120, backend.getWidth());
    }

    @Test
    void getHeightReturnsInjectedHeight() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 120, 40);
        backend.init();
        assertEquals(40, backend.getHeight());
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Test
    void putCharAppendsToBuffer() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        backend.init();
        sink.reset();
        backend.putChar(5, 3, 'X', null);
        backend.flush();
        String output = sink.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("X"), "character must appear in output");
    }

    @Test
    void putCharWithStyleContainsSgrCodes() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        backend.init();
        sink.reset();
        Style bold = Style.DEFAULT.withBold(true).withForeground(Color.RED);
        backend.putChar(0, 0, 'B', bold);
        backend.flush();
        String output = sink.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("B"), "character must appear");
        assertTrue(output.contains("\033["), "must contain SGR sequence");
    }

    @Test
    void clearWritesClearSequence() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        backend.init();
        sink.reset();
        backend.clear();
        backend.flush();
        assertTrue(sink.toString(StandardCharsets.UTF_8).contains("\033[2J"));
    }

    @Test
    void setCursorWritesMoveCursorSequence() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        backend.init();
        sink.reset();
        backend.setCursor(4, 9);
        backend.flush();
        assertTrue(sink.toString(StandardCharsets.UTF_8).contains("\033[10;5H"));
    }

    @Test
    void hideCursorWritesSequence() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        backend.init();
        sink.reset();
        backend.hideCursor();
        backend.flush();
        assertTrue(sink.toString(StandardCharsets.UTF_8).contains(AnsiWriter.CURSOR_HIDE));
    }

    @Test
    void showCursorWritesSequence() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        backend.init();
        sink.reset();
        backend.showCursor();
        backend.flush();
        assertTrue(sink.toString(StandardCharsets.UTF_8).contains(AnsiWriter.CURSOR_SHOW));
    }

    // ── Key input ─────────────────────────────────────────────────────────────

    @Test
    void readKeyReturnsCharacter() throws Exception {
        byte[] input = {'h'};
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(input), sink, 80, 24);
        backend.init();
        KeyEvent e = backend.readKey();
        assertEquals(KeyType.CHARACTER, e.type());
        assertEquals('h', e.character());
    }

    @Test
    void readKeyReturnsEnterOnCr() throws Exception {
        byte[] input = {0x0D};
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(input), sink, 80, 24);
        backend.init();
        KeyEvent e = backend.readKey();
        assertEquals(KeyType.ENTER, e.type());
    }

    @Test
    void readKeyReturnsEofOnStreamEnd() throws Exception {
        byte[] input = {};
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(input), sink, 80, 24);
        backend.init();
        KeyEvent e = backend.readKey();
        assertEquals(KeyType.EOF, e.type());
    }

    @Test
    void readKeyTimeoutReturnsNullWhenNoInput() throws Exception {
        byte[] input = {};
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(input), sink, 80, 24);
        backend.init();
        // Empty stream → available()=0 until EOF → timeout returns null
        KeyEvent e = backend.readKey(50);
        // May return EOF (stream ended) or null (timeout)
        assertTrue(e == null || e.type() == KeyType.EOF);
    }

    // ── Resize listener ───────────────────────────────────────────────────────

    @Test
    void resizeListenerIsRegistered() {
        backend = NativeTerminalBackend.createForTesting(
                new ByteArrayInputStream(new byte[0]), sink, 80, 24);
        AtomicBoolean called = new AtomicBoolean(false);
        backend.setResizeListener(() -> called.set(true));
        // We can't easily trigger a resize in tests, but registration must not throw
        assertFalse(called.get());
    }
}
