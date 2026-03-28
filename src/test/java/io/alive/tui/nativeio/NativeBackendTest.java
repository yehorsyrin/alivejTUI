package io.alive.tui.nativeio;

import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.nativeio.backend.NativeBackend;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link NativeBackend} using piped streams.
 * No real terminal required.
 *
 * @author Jarvis (AI)
 */
class NativeBackendTest {

    private ByteArrayOutputStream outBuf;
    private NativeBackend backend;

    /** Creates a backend with the given preset input bytes (int values for convenience). */
    private NativeBackend makeBackend(int... inputInts) {
        outBuf = new ByteArrayOutputStream();
        byte[] bytes = new byte[inputInts.length];
        for (int i = 0; i < inputInts.length; i++) bytes[i] = (byte) inputInts[i];
        InputStream in = new ByteArrayInputStream(bytes);
        return new NativeBackend(outBuf, in);
    }

    @BeforeEach
    void setUp() {
        outBuf  = new ByteArrayOutputStream();
        InputStream emptyIn = new ByteArrayInputStream(new byte[0]);
        backend = new NativeBackend(outBuf, emptyIn);
    }

    @AfterEach
    void tearDown() {
        backend.shutdown();
    }

    private String output() {
        return outBuf.toString(StandardCharsets.UTF_8);
    }

    // --- Constructor ---

    @Test
    void constructor_nullOut_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new NativeBackend(null, new ByteArrayInputStream(new byte[0])));
    }

    @Test
    void constructor_nullIn_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new NativeBackend(new ByteArrayOutputStream(), null));
    }

    // --- Factory methods ---

    @Test
    void create_doesNotThrow() {
        // create() references System.out/in — just verify no exception
        assertDoesNotThrow(NativeBackend::create);
    }

    // --- init / shutdown ---

    @Test
    void init_writesAlternateScreenAndHideCursor() {
        backend.init();
        String out = output();
        assertTrue(out.contains("\033[?1049h"), "alternate screen on");
        assertTrue(out.contains("\033[?25l"),   "hide cursor");
    }

    @Test
    void shutdown_writesMainScreenAndShowCursor() {
        backend.init();
        outBuf.reset();
        backend.shutdown();
        String out = output();
        assertTrue(out.contains("\033[?25h"),   "show cursor");
        assertTrue(out.contains("\033[?1049l"), "alternate screen off");
    }

    // --- Dimensions ---

    @Test
    void getWidth_afterInit_positive() {
        backend.init();
        assertTrue(backend.getWidth() > 0);
    }

    @Test
    void getHeight_afterInit_positive() {
        backend.init();
        assertTrue(backend.getHeight() > 0);
    }

    // --- putChar ---

    @Test
    void putChar_writesCharToOutput() {
        backend.init();
        outBuf.reset();
        backend.putChar(0, 0, 'X', Style.DEFAULT);
        assertTrue(output().contains("X"));
    }

    @Test
    void putChar_boldStyle_writesBoldEscape() {
        backend.init();
        outBuf.reset();
        backend.putChar(0, 0, 'A', Style.DEFAULT.withBold(true));
        String out = output();
        assertTrue(out.contains(";1"), "Expected bold SGR code");
    }

    // --- flush ---

    @Test
    void flush_noException() {
        backend.init();
        assertDoesNotThrow(backend::flush);
    }

    // --- hideCursor / showCursor ---

    @Test
    void hideCursor_writesHideSequence() {
        outBuf.reset();
        backend.hideCursor();
        assertTrue(output().contains("\033[?25l"));
    }

    @Test
    void showCursor_writesShowSequence() {
        outBuf.reset();
        backend.showCursor();
        assertTrue(output().contains("\033[?25h"));
    }

    // --- setCursor ---

    @Test
    void setCursor_writesCursorMoveSequence() {
        outBuf.reset();
        backend.setCursor(4, 2);
        // Row 2 → ANSI row 3, Col 4 → ANSI col 5
        assertTrue(output().contains("\033[3;5H"));
    }

    // --- clear ---

    @Test
    void clear_writesEraseAndHome() {
        outBuf.reset();
        backend.clear();
        String out = output();
        assertTrue(out.contains("\033[2J"));
        assertTrue(out.contains("\033[1;1H"));
    }

    // --- readKey ---

    @Test
    void readKey_singleChar_decodesCorrectly() throws InterruptedException {
        NativeBackend b = makeBackend('a');
        b.init();
        KeyEvent ev = b.readKey();
        assertEquals(KeyType.CHARACTER, ev.type());
        assertEquals('a', ev.character());
        b.shutdown();
    }

    @Test
    void readKey_enter_decodesCorrectly() throws InterruptedException {
        NativeBackend b = makeBackend(13);
        b.init();
        KeyEvent ev = b.readKey();
        assertEquals(KeyType.ENTER, ev.type());
        b.shutdown();
    }

    @Test
    void readKey_arrowUp_decodesCorrectly() throws InterruptedException {
        NativeBackend b = makeBackend(0x1B, '[', 'A');
        b.init();
        KeyEvent ev = b.readKey();
        assertEquals(KeyType.ARROW_UP, ev.type());
        b.shutdown();
    }

    @Test
    void readKey_timeout_noData_returnsNull() throws InterruptedException {
        NativeBackend b = makeBackend(); // empty input
        b.init();
        KeyEvent ev = b.readKey(50);
        assertNull(ev);
        b.shutdown();
    }

    // --- resizeListener ---

    @Test
    void setResizeListener_acceptsRunnable() {
        assertDoesNotThrow(() -> backend.setResizeListener(() -> {}));
    }
}
