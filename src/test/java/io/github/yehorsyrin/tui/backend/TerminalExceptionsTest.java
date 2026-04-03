package io.github.yehorsyrin.tui.backend;

import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.style.Style;
import io.github.yehorsyrin.tui.style.Theme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TerminalInitException}, {@link TerminalRenderException},
 * and the default methods on {@link TerminalBackend}.
 *
 * @author Jarvis (AI)
 */
class TerminalExceptionsTest {

    // ── TerminalInitException ─────────────────────────────────────────────────

    @Test
    void terminalInitException_storesMessageAndCause() {
        Throwable cause = new IllegalStateException("bad state");
        TerminalInitException ex = new TerminalInitException("init failed", cause);
        assertEquals("init failed", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void terminalInitException_isRuntimeException() {
        assertInstanceOf(RuntimeException.class,
                new TerminalInitException("msg", new Exception()));
    }

    // ── TerminalRenderException ───────────────────────────────────────────────

    @Test
    void terminalRenderException_storesMessageAndCause() {
        Throwable cause = new java.io.IOException("io error");
        TerminalRenderException ex = new TerminalRenderException("render failed", cause);
        assertEquals("render failed", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void terminalRenderException_isRuntimeException() {
        assertInstanceOf(RuntimeException.class,
                new TerminalRenderException("msg", new Exception()));
    }

    // ── TerminalBackend default methods ───────────────────────────────────────

    @Test
    void applyTheme_defaultImpl_isNoOp() {
        // MockBackend does not override applyTheme — exercises the default
        MockBackend backend = new MockBackend(80, 24);
        assertDoesNotThrow(() -> backend.applyTheme(Theme.DARK));
        assertDoesNotThrow(() -> backend.applyTheme(null));
    }

    @Test
    void readKeyWithTimeout_defaultImpl_delegatesToReadKey() throws InterruptedException {
        // Minimal backend that delegates readKey() but uses the default readKey(long)
        TerminalBackend backend = new TerminalBackend() {
            @Override public void init() {}
            @Override public void shutdown() {}
            @Override public int getWidth()  { return 80; }
            @Override public int getHeight() { return 24; }
            @Override public void putChar(int col, int row, char c, Style style) {}
            @Override public void flush() {}
            @Override public void hideCursor() {}
            @Override public void showCursor() {}
            @Override public void setCursor(int col, int row) {}
            @Override public void clear() {}
            @Override public KeyEvent readKey() { return KeyEvent.of(KeyType.EOF); }
            @Override public void setResizeListener(Runnable onResize) {}
        };
        KeyEvent result = backend.readKey(100);
        assertEquals(KeyType.EOF, result.type());
    }
}
