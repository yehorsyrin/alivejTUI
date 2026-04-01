package io.github.yehorsyrin.tui.platform.backend;

import io.github.yehorsyrin.tui.backend.TerminalBackend;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests run with -Djava.awt.headless=true (configured in pom.xml surefire),
 * so createSwing() must throw and createAuto()/createNative() must succeed.
 *
 * @author Jarvis (AI)
 */
class BackendsTest {

    @Test
    void createNativeReturnsNonNull() {
        TerminalBackend b = Backends.createNative();
        assertNotNull(b);
    }

    @Test
    void createNativeReturnsNativeTerminalBackend() {
        assertTrue(Backends.createNative() instanceof NativeTerminalBackend);
    }

    @Test
    void createMockReturnsNonNull() {
        assertNotNull(Backends.createMock(80, 24));
    }

    @Test
    void createMockRespectsFixedDimensions() {
        TerminalBackend b = Backends.createMock(100, 30);
        b.init();
        assertEquals(100, b.getWidth());
        assertEquals(30, b.getHeight());
        b.shutdown();
    }

    @Test
    void createLanternaReturnsNonNull() {
        assertNotNull(Backends.createLanterna());
    }

    @Test
    void createSwingThrowsInHeadlessEnvironment() {
        // pom.xml configures -Djava.awt.headless=true for Surefire
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            assertThrows(UnsupportedOperationException.class, Backends::createSwing);
        }
    }

    @Test
    void createAutoReturnsNativeInHeadlessEnvironment() {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            assertTrue(Backends.createAuto() instanceof NativeTerminalBackend);
        }
    }
}
