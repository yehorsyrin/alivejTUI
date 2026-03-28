package io.alive.tui.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AsyncTask} and {@link AliveJTUI#runAsync(AsyncTask)}.
 *
 * <p>Uses package-private {@code AliveJTUI.drainAsyncQueue()} to pump the event-loop
 * queue without a live event loop.
 *
 * @author Jarvis (AI)
 */
class AsyncTaskTest {

    @AfterEach
    void drainQueue() throws InterruptedException {
        // Give background threads time to finish and drain the queue
        Thread.sleep(50);
        AliveJTUI.drainAsyncQueue();
    }

    // --- AsyncTask factory ---

    @Test
    void of_nullWork_throws() {
        assertThrows(IllegalArgumentException.class, () -> AsyncTask.of(null, r -> {}));
    }

    @Test
    void of_nullOnSuccess_throws() {
        assertThrows(IllegalArgumentException.class, () -> AsyncTask.of(() -> 1, null));
    }

    @Test
    void of_withError_storesCallbacks() {
        AtomicReference<Exception> caught = new AtomicReference<>();
        AsyncTask<Integer> task = AsyncTask.of(
                () -> { throw new RuntimeException("boom"); },
                r  -> {},
                caught::set
        );
        assertNotNull(task.getWork());
        assertNotNull(task.getOnSuccess());
        assertNotNull(task.getOnError());
    }

    // --- runAsync ---

    @Test
    void runAsync_successCallbackFiredWithResult() throws InterruptedException {
        List<String> received = new ArrayList<>();
        AliveJTUI.runAsync(AsyncTask.of(
                () -> "hello",
                received::add
        ));
        Thread.sleep(100);
        AliveJTUI.drainAsyncQueue();
        assertEquals(List.of("hello"), received);
    }

    @Test
    void runAsync_errorCallbackFiredOnException() throws InterruptedException {
        AtomicReference<Exception> caught = new AtomicReference<>();
        RuntimeException boom = new RuntimeException("boom");

        AliveJTUI.runAsync(AsyncTask.of(
                () -> { throw boom; },
                r  -> fail("should not call onSuccess"),
                caught::set
        ));
        Thread.sleep(100);
        AliveJTUI.drainAsyncQueue();
        assertSame(boom, caught.get());
    }

    @Test
    void runAsync_errorWithNoHandler_silentlyIgnored() throws InterruptedException {
        // Should not throw
        AliveJTUI.runAsync(AsyncTask.of(
                () -> { throw new RuntimeException("silent"); },
                r  -> fail("should not call onSuccess")
        ));
        Thread.sleep(100);
        AliveJTUI.drainAsyncQueue();
        // If we reach here without exception, the test passes
    }

    @Test
    void runAsync_null_doesNothing() {
        // Should not throw
        AliveJTUI.runAsync(null);
    }

    @Test
    void runAsync_multipleTasksAllDelivered() throws InterruptedException {
        AtomicInteger sum = new AtomicInteger(0);
        for (int i = 1; i <= 5; i++) {
            final int val = i;
            AliveJTUI.runAsync(AsyncTask.of(() -> val, sum::addAndGet));
        }
        Thread.sleep(200);
        AliveJTUI.drainAsyncQueue();
        assertEquals(15, sum.get()); // 1+2+3+4+5
    }

    @Test
    void asyncTask_run_delegatesToRunAsync() throws InterruptedException {
        List<Integer> received = new ArrayList<>();
        AsyncTask<Integer> task = AsyncTask.of(() -> 42, received::add);
        task.run();
        Thread.sleep(100);
        AliveJTUI.drainAsyncQueue();
        assertEquals(List.of(42), received);
    }
}
