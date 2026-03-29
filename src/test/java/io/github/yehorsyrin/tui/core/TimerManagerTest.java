package io.github.yehorsyrin.tui.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TimerManager} — scheduling, ticking, repeating, cancellation.
 *
 * @author Jarvis (AI)
 */
class TimerManagerTest {

    private TimerManager timers;

    @BeforeEach
    void setUp() {
        timers = new TimerManager();
    }

    // --- Initial state ---

    @Test
    void newManager_hasNoPendingTimers() {
        assertFalse(timers.hasPendingTimers());
        assertEquals(0, timers.size());
    }

    @Test
    void newManager_msUntilNext_returnsMaxValue() {
        assertEquals(Long.MAX_VALUE, timers.msUntilNext());
    }

    // --- One-shot timers ---

    @Test
    void scheduleZeroDelay_firesOnNextTick() {
        AtomicInteger count = new AtomicInteger(0);
        timers.schedule(0, count::incrementAndGet);
        timers.tick(null);
        assertEquals(1, count.get());
    }

    @Test
    void scheduleZeroDelay_removedAfterFiring() {
        timers.schedule(0, () -> {});
        timers.tick(null);
        assertFalse(timers.hasPendingTimers());
        assertEquals(0, timers.size());
    }

    @Test
    void scheduleFutureTimer_doesNotFireImmediately() {
        AtomicInteger count = new AtomicInteger(0);
        timers.schedule(60_000, count::incrementAndGet); // 60 seconds in the future
        timers.tick(null);
        assertEquals(0, count.get());
        assertTrue(timers.hasPendingTimers());
    }

    @Test
    void tick_returnsTrueWhenTimerFired() {
        timers.schedule(0, () -> {});
        assertTrue(timers.tick(null));
    }

    @Test
    void tick_returnsFalseWhenNoTimerFired() {
        timers.schedule(60_000, () -> {});
        assertFalse(timers.tick(null));
    }

    @Test
    void tick_withNoTimers_returnsFalse() {
        assertFalse(timers.tick(null));
    }

    // --- onFire callback ---

    @Test
    void tick_callsOnFireWhenTimerFired() {
        AtomicInteger fireCalls = new AtomicInteger(0);
        timers.schedule(0, () -> {});
        timers.tick(fireCalls::incrementAndGet);
        assertEquals(1, fireCalls.get());
    }

    @Test
    void tick_doesNotCallOnFireWhenNoTimerFired() {
        AtomicInteger fireCalls = new AtomicInteger(0);
        timers.schedule(60_000, () -> {});
        timers.tick(fireCalls::incrementAndGet);
        assertEquals(0, fireCalls.get());
    }

    @Test
    void tick_multipleDueTimers_onFireCalledOnce() {
        AtomicInteger fireCalls = new AtomicInteger(0);
        timers.schedule(0, () -> {});
        timers.schedule(0, () -> {});
        timers.tick(fireCalls::incrementAndGet);
        assertEquals(1, fireCalls.get());  // onFire called once after all timers fired
    }

    // --- Repeating timers ---

    @Test
    void scheduleRepeating_rescheduledAfterFiring() throws InterruptedException {
        AtomicInteger count = new AtomicInteger(0);
        timers.scheduleRepeating(1, count::incrementAndGet); // 1ms interval
        Thread.sleep(5);
        timers.tick(null);
        assertTrue(count.get() >= 1, "Repeating timer should have fired");
        assertTrue(timers.hasPendingTimers(), "Repeating timer should remain registered");
    }

    @Test
    void scheduleRepeating_zeroInterval_ignored() {
        timers.scheduleRepeating(0, () -> {});
        assertFalse(timers.hasPendingTimers());
    }

    // --- Cancellation ---

    @Test
    void cancel_removesTimer() {
        Runnable task = () -> {};
        timers.schedule(60_000, task);
        assertTrue(timers.hasPendingTimers());
        timers.cancel(task);
        assertFalse(timers.hasPendingTimers());
    }

    @Test
    void cancel_byReferenceEquality_onlyMatchingRemoved() {
        Runnable taskA = () -> {};
        Runnable taskB = () -> {};
        timers.schedule(60_000, taskA);
        timers.schedule(60_000, taskB);
        timers.cancel(taskA);
        assertEquals(1, timers.size());
    }

    @Test
    void cancel_nullTask_ignored() {
        assertDoesNotThrow(() -> timers.cancel(null));
    }

    @Test
    void clear_removesAllTimers() {
        timers.schedule(0, () -> {});
        timers.schedule(1_000, () -> {});
        timers.clear();
        assertFalse(timers.hasPendingTimers());
        assertEquals(0, timers.size());
    }

    // --- msUntilNext ---

    @Test
    void msUntilNext_withImmediateTimer_returnsZeroOrNegativeClampedToZero() {
        timers.schedule(0, () -> {});
        long ms = timers.msUntilNext();
        assertEquals(0, ms); // already due or exactly at 0
    }

    @Test
    void msUntilNext_withFutureTimer_returnsPositiveValue() {
        timers.schedule(1_000, () -> {});
        long ms = timers.msUntilNext();
        assertTrue(ms > 0 && ms <= 1_000, "Expected 0..1000 ms, got: " + ms);
    }

    // --- Null safety ---

    @Test
    void scheduleNullTask_ignored() {
        timers.schedule(0, null);
        assertFalse(timers.hasPendingTimers());
    }

    @Test
    void scheduleRepeatingNullTask_ignored() {
        timers.scheduleRepeating(100, null);
        assertFalse(timers.hasPendingTimers());
    }

    // --- Multiple firings order ---

    @Test
    void multipleDueTimers_allFire() {
        List<String> fired = new ArrayList<>();
        timers.schedule(0, () -> fired.add("A"));
        timers.schedule(0, () -> fired.add("B"));
        timers.schedule(0, () -> fired.add("C"));
        timers.tick(null);
        assertEquals(3, fired.size());
        assertTrue(fired.containsAll(List.of("A", "B", "C")));
    }
}
