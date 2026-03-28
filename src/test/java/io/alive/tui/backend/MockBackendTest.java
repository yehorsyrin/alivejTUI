package io.alive.tui.backend;

import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MockBackend}.
 *
 * @author Jarvis (AI)
 */
class MockBackendTest {

    private MockBackend backend;

    @BeforeEach
    void setUp() {
        backend = new MockBackend(80, 24);
    }

    @Test
    void constructor_invalidWidth_throws() {
        assertThrows(IllegalArgumentException.class, () -> new MockBackend(0, 24));
    }

    @Test
    void constructor_invalidHeight_throws() {
        assertThrows(IllegalArgumentException.class, () -> new MockBackend(80, 0));
    }

    @Test
    void init_setsInitialized() {
        assertFalse(backend.isInitialized());
        backend.init();
        assertTrue(backend.isInitialized());
    }

    @Test
    void shutdown_clearsInitialized() {
        backend.init();
        backend.shutdown();
        assertFalse(backend.isInitialized());
    }

    @Test
    void getDimensions() {
        assertEquals(80, backend.getWidth());
        assertEquals(24, backend.getHeight());
    }

    @Test
    void putChar_storesInGrid() {
        backend.putChar(5, 3, 'X', Style.DEFAULT);
        assertEquals('X', backend.getCharAt(5, 3));
    }

    @Test
    void putChar_storesStyle() {
        Style bold = Style.DEFAULT.withBold(true);
        backend.putChar(0, 0, 'A', bold);
        assertEquals(bold, backend.getStyleAt(0, 0));
    }

    @Test
    void putChar_outOfBounds_doesNotThrow() {
        assertDoesNotThrow(() -> backend.putChar(100, 100, 'Z', Style.DEFAULT));
    }

    @Test
    void getCharAt_empty_returnsNul() {
        assertEquals('\0', backend.getCharAt(0, 0));
    }

    @Test
    void getCharAt_outOfBounds_returnsNul() {
        assertEquals('\0', backend.getCharAt(-1, 0));
        assertEquals('\0', backend.getCharAt(0, -1));
        assertEquals('\0', backend.getCharAt(80, 0));
    }

    @Test
    void getRow_returnsRowString() {
        backend.putChar(0, 2, 'H', Style.DEFAULT);
        backend.putChar(1, 2, 'i', Style.DEFAULT);
        String row = backend.getRow(2);
        assertEquals('H', row.charAt(0));
        assertEquals('i', row.charAt(1));
    }

    @Test
    void clear_resetsGrid() {
        backend.putChar(0, 0, 'A', Style.DEFAULT);
        backend.clear();
        assertEquals('\0', backend.getCharAt(0, 0));
    }

    @Test
    void clear_emptiesCallLog() {
        backend.putChar(0, 0, 'A', Style.DEFAULT);
        backend.clear();
        assertTrue(backend.getCallLog().isEmpty());
    }

    @Test
    void callLog_recordedInOrder() {
        backend.putChar(1, 0, 'A', Style.DEFAULT);
        backend.putChar(2, 0, 'B', Style.DEFAULT);
        var log = backend.getCallLog();
        assertEquals(2, log.size());
        assertEquals('A', log.get(0).character());
        assertEquals('B', log.get(1).character());
    }

    @Test
    void pushKey_readKey_returnsEnqueuedEvent() throws InterruptedException {
        KeyEvent enter = KeyEvent.of(KeyType.ENTER);
        backend.pushKey(enter);
        assertSame(enter, backend.readKey());
    }

    @Test
    void readKey_emptyQueue_returnsEof() throws InterruptedException {
        assertEquals(KeyType.EOF, backend.readKey().type());
    }

    @Test
    void readKey_timeout_emptyQueue_returnsNull() throws InterruptedException {
        assertNull(backend.readKey(10));
    }

    @Test
    void pushKey_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> backend.pushKey(null));
    }

    @Test
    void setCursor_updatesPosition() {
        backend.setCursor(10, 5);
        assertEquals(10, backend.getCursorCol());
        assertEquals(5,  backend.getCursorRow());
    }

    @Test
    void hideCursor_setsMinusOne() {
        backend.setCursor(5, 5);
        backend.hideCursor();
        assertEquals(-1, backend.getCursorCol());
        assertEquals(-1, backend.getCursorRow());
    }

    @Test
    void simulateResize_firesListener() {
        boolean[] fired = { false };
        backend.setResizeListener(() -> fired[0] = true);
        backend.simulateResize();
        assertTrue(fired[0]);
    }
}
