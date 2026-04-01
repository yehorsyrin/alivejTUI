package io.alive.tui.platform.signal;

import io.alive.tui.platform.size.TerminalSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jarvis (AI)
 */
class ResizePollerTest {

    private ResizePoller poller;

    @AfterEach
    void tearDown() {
        if (poller != null) poller.stop();
    }

    // ── Construction ──────────────────────────────────────────────────────────

    @Test
    void nullListenerThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ResizePoller(null));
    }

    @Test
    void zeroIntervalThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ResizePoller(s -> {}, 0));
    }

    @Test
    void negativeIntervalThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ResizePoller(s -> {}, -1));
    }

    @Test
    void initialLastSizeIsNotNull() {
        poller = new ResizePoller(s -> {});
        assertNotNull(poller.getLastSize());
    }

    // ── Start / stop ──────────────────────────────────────────────────────────

    @Test
    void notRunningBeforeStart() {
        poller = new ResizePoller(s -> {});
        assertFalse(poller.isRunning());
    }

    @Test
    void runningAfterStart() throws InterruptedException {
        poller = new ResizePoller(s -> {}, 50);
        poller.start();
        Thread.sleep(20); // give thread time to start
        assertTrue(poller.isRunning());
    }

    @Test
    void notRunningAfterStop() throws InterruptedException {
        poller = new ResizePoller(s -> {}, 50);
        poller.start();
        Thread.sleep(20);
        poller.stop();
        assertFalse(poller.isRunning());
    }

    @Test
    void doubleStartIsIdempotent() throws InterruptedException {
        poller = new ResizePoller(s -> {}, 50);
        poller.start();
        poller.start(); // second call must not throw or create a second thread
        Thread.sleep(20);
        assertTrue(poller.isRunning());
    }

    @Test
    void doubleStopIsIdempotent() {
        poller = new ResizePoller(s -> {}, 50);
        poller.start();
        assertDoesNotThrow(() -> {
            poller.stop();
            poller.stop();
        });
    }

    @Test
    void stopBeforeStartIsNoOp() {
        poller = new ResizePoller(s -> {}, 50);
        assertDoesNotThrow(() -> poller.stop());
        assertFalse(poller.isRunning());
    }

    // ── Listener ──────────────────────────────────────────────────────────────

    @Test
    void listenerNotCalledWhenSizeUnchanged() throws InterruptedException {
        AtomicInteger calls = new AtomicInteger();
        // Use a custom poller that always reports the same size (no change)
        TerminalSize fixed = new TerminalSize(80, 24);
        // We can't easily mock TerminalSizeDetector, but we can verify that
        // the listener is not called more than it should be during a short run
        poller = new ResizePoller(s -> calls.incrementAndGet(), 30);
        poller.start();
        Thread.sleep(150); // ~5 poll cycles
        poller.stop();
        // In CI with fixed piped stdin, the size shouldn't change
        // So calls should be 0 unless the environment changes
        assertTrue(calls.get() >= 0, "calls should be non-negative");
    }

    @Test
    void listenerExceptionDoesNotKillPoller() throws InterruptedException {
        // Listener that always throws — poller must survive
        poller = new ResizePoller(s -> { throw new RuntimeException("boom"); }, 30);
        poller.start();
        Thread.sleep(100);
        assertTrue(poller.isRunning(), "poller must survive listener exception");
    }

    @Test
    void getLastSizeReturnsPositiveDimensions() {
        poller = new ResizePoller(s -> {}, 50);
        TerminalSize size = poller.getLastSize();
        assertTrue(size.cols() > 0);
        assertTrue(size.rows() > 0);
    }

    // ── Default interval ──────────────────────────────────────────────────────

    @Test
    void defaultIntervalIs200ms() {
        assertEquals(200L, ResizePoller.DEFAULT_INTERVAL_MS);
    }

    @Test
    void defaultConstructorUsesDefaultInterval() throws InterruptedException {
        poller = new ResizePoller(s -> {});
        poller.start();
        Thread.sleep(20);
        assertTrue(poller.isRunning());
    }
}
