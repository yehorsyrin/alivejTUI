package io.alive.tui.nativeio;

import io.alive.tui.backend.MockBackend;
import io.alive.tui.backend.TerminalBackend;
import io.alive.tui.nativeio.backend.Backends;
import io.alive.tui.nativeio.backend.NativeBackend;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Backends} factory.
 *
 * @author Jarvis (AI)
 */
class BackendsTest {

    @Test
    void createNative_returnsNativeBackend() {
        TerminalBackend b = Backends.createNative();
        assertNotNull(b);
        assertInstanceOf(NativeBackend.class, b);
    }

    @Test
    void createLanterna_returnsNonNull() {
        TerminalBackend b = Backends.createLanterna();
        assertNotNull(b);
    }

    @Test
    void createMock_returnsMockBackend() {
        TerminalBackend b = Backends.createMock(80, 24);
        assertNotNull(b);
        assertInstanceOf(MockBackend.class, b);
    }

    @Test
    void createMock_dimensionsReflected() {
        TerminalBackend b = Backends.createMock(120, 40);
        b.init();
        assertEquals(120, b.getWidth());
        assertEquals(40,  b.getHeight());
        b.shutdown();
    }

    @Test
    void createNative_and_createMock_areDistinctInstances() {
        TerminalBackend n = Backends.createNative();
        TerminalBackend m = Backends.createMock(80, 24);
        assertNotSame(n, m);
    }
}
