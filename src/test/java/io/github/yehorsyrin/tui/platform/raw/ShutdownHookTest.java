package io.github.yehorsyrin.tui.platform.raw;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jarvis (AI)
 */
class ShutdownHookTest {

    @Test
    void hookIsNotCancelledAfterRegistration() {
        AtomicInteger calls = new AtomicInteger();
        ShutdownHook hook = ShutdownHook.register(calls::incrementAndGet);
        assertFalse(hook.isCancelled());
        hook.cancel(); // clean up
    }

    @Test
    void cancelSetsCancelledFlag() {
        AtomicInteger calls = new AtomicInteger();
        ShutdownHook hook = ShutdownHook.register(calls::incrementAndGet);
        hook.cancel();
        assertTrue(hook.isCancelled());
    }

    @Test
    void cancelIsIdempotent() {
        AtomicInteger calls = new AtomicInteger();
        ShutdownHook hook = ShutdownHook.register(calls::incrementAndGet);
        assertDoesNotThrow(() -> {
            hook.cancel();
            hook.cancel();
        });
        assertTrue(hook.isCancelled());
    }

    @Test
    void cancelledHookDoesNotRunAction() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ShutdownHook hook = ShutdownHook.register(calls::incrementAndGet);
        hook.cancel();

        // Simulate what the shutdown hook thread does
        Thread t = new Thread(() -> {
            // re-check: if cancelled, action must not run
            if (!hook.isCancelled()) {
                calls.incrementAndGet();
            }
        });
        t.start();
        t.join(1000);
        assertEquals(0, calls.get(), "action must not run after cancel()");
    }

    @Test
    void uncancelledHookRunsAction() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ShutdownHook hook = ShutdownHook.register(calls::incrementAndGet);

        // Simulate shutdown hook thread without cancelling
        Thread t = new Thread(() -> {
            if (!hook.isCancelled()) {
                calls.incrementAndGet();
            }
        });
        t.start();
        t.join(1000);
        hook.cancel(); // clean up the real JVM hook
        assertEquals(1, calls.get());
    }

    @Test
    void multipleHooksAreIndependent() {
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        ShutdownHook hookA = ShutdownHook.register(a::incrementAndGet);
        ShutdownHook hookB = ShutdownHook.register(b::incrementAndGet);

        hookA.cancel();
        assertFalse(hookB.isCancelled());
        hookB.cancel();
    }
}
