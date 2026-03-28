package io.alive.tui.nativeio;

import io.alive.tui.nativeio.signal.ResizePoller;
import io.alive.tui.nativeio.size.TerminalSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ResizePoller}.
 *
 * @author Jarvis (AI)
 */
class ResizePollerTest {

    private ResizePoller poller;

    @AfterEach
    void tearDown() {
        if (poller != null) poller.stop();
    }

    // --- Constructor ---

    @Test
    void constructor_nullListener_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ResizePoller(null));
    }

    @Test
    void constructor_zeroInterval_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ResizePoller(s -> {}, 0));
    }

    @Test
    void constructor_negativeInterval_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ResizePoller(s -> {}, -1));
    }

    // --- Lifecycle ---

    @Test
    void start_isRunning() {
        poller = new ResizePoller(s -> {}, 100);
        poller.start();
        assertTrue(poller.isRunning());
    }

    @Test
    void stop_isNotRunning() {
        poller = new ResizePoller(s -> {}, 100);
        poller.start();
        poller.stop();
        assertFalse(poller.isRunning());
    }

    @Test
    void stop_withoutStart_noException() {
        poller = new ResizePoller(s -> {});
        assertDoesNotThrow(poller::stop);
    }

    @Test
    void start_twice_isIdempotent() {
        poller = new ResizePoller(s -> {}, 100);
        poller.start();
        poller.start(); // should be no-op
        assertTrue(poller.isRunning());
    }

    @Test
    void notRunning_beforeStart() {
        poller = new ResizePoller(s -> {});
        assertFalse(poller.isRunning());
    }

    // --- Initial size ---

    @Test
    void getLastSize_beforeStart_null() {
        poller = new ResizePoller(s -> {});
        assertNull(poller.getLastSize());
    }

    @Test
    void getLastSize_afterStart_notNull() {
        poller = new ResizePoller(s -> {}, 100);
        poller.start();
        assertNotNull(poller.getLastSize());
    }

    @Test
    void getLastSize_afterStart_validDimensions() {
        poller = new ResizePoller(s -> {}, 100);
        poller.start();
        TerminalSize sz = poller.getLastSize();
        assertTrue(sz.cols() > 0);
        assertTrue(sz.rows() > 0);
    }

    // --- Listener robustness ---

    @Test
    void listenerException_doesNotKillPoller() throws InterruptedException {
        AtomicInteger callCount = new AtomicInteger(0);
        // Listener that throws on first call but should not kill the thread
        poller = new ResizePoller(s -> {
            callCount.incrementAndGet();
            throw new RuntimeException("Boom");
        }, 50);
        poller.start();
        Thread.sleep(200);
        // Poller should still be running despite the exception
        assertTrue(poller.isRunning());
    }

    // --- Default interval constant ---

    @Test
    void defaultIntervalMs_is200() {
        assertEquals(200, ResizePoller.DEFAULT_INTERVAL_MS);
    }
}
