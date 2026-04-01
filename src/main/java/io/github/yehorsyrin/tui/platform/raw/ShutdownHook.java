package io.github.yehorsyrin.tui.platform.raw;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registers a JVM shutdown hook that restores terminal state on unexpected exit.
 *
 * <p>Without a shutdown hook, a crash or {@code Ctrl+C} leaves the terminal in
 * raw mode and on the alternate screen buffer — the user's shell is unusable
 * until they run {@code reset} or close the window.
 *
 * <p>Usage:
 * <pre>{@code
 *   ShutdownHook hook = ShutdownHook.register(this::restoreTerminal);
 *   // ... run app ...
 *   hook.cancel(); // normal shutdown — let the caller restore state explicitly
 * }</pre>
 *
 * <p>The hook fires at most once. After {@link #cancel()} the action will never run.
 *
 * @author Jarvis (AI)
 */
public final class ShutdownHook {

    private final Runnable action;
    private final Thread thread;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private ShutdownHook(Runnable action) {
        this.action = action;
        this.thread = new Thread(() -> {
            if (!cancelled.get()) {
                action.run();
            }
        }, "alivejTUI-shutdown-hook");
    }

    /**
     * Registers a new shutdown hook that will execute {@code action} when the JVM exits
     * unless {@link #cancel()} is called first.
     *
     * @param action the cleanup action to run on abnormal exit
     * @return the registered hook (use {@link #cancel()} for clean shutdown)
     */
    public static ShutdownHook register(Runnable action) {
        ShutdownHook hook = new ShutdownHook(action);
        Runtime.getRuntime().addShutdownHook(hook.thread);
        return hook;
    }

    /**
     * Cancels the hook so the action will not be executed on JVM exit.
     * Call this during a normal (expected) shutdown after restoring terminal state.
     *
     * <p>Attempts to deregister the hook thread. If the JVM is already shutting down
     * (the hook thread is running), deregistration will fail — this is harmless
     * because the flag is checked before executing the action.
     */
    public void cancel() {
        cancelled.set(true);
        try {
            Runtime.getRuntime().removeShutdownHook(thread);
        } catch (IllegalStateException ignored) {
            // JVM already shutting down — flag is enough
        }
    }

    /**
     * Returns {@code true} if this hook has been cancelled.
     */
    public boolean isCancelled() {
        return cancelled.get();
    }
}
