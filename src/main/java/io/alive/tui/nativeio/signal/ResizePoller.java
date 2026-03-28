package io.alive.tui.nativeio.signal;

import io.alive.tui.nativeio.size.TerminalSize;
import io.alive.tui.nativeio.size.TerminalSizeDetector;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Polls for terminal resize events by periodically querying {@link TerminalSizeDetector}.
 *
 * <p>Runs a daemon thread that checks the terminal size every {@value #DEFAULT_INTERVAL_MS} ms
 * and invokes the registered listener when a change is detected.
 *
 * <p>This is the portable cross-platform approach (works on Windows and POSIX alike).
 * On POSIX, {@code SigwinchWatcher} can be layered on top for lower latency.
 *
 * <p>Lifecycle:
 * <pre>
 *   ResizePoller poller = new ResizePoller(size -> handleResize(size));
 *   poller.start();
 *   // ...
 *   poller.stop();
 * </pre>
 *
 * @author Jarvis (AI)
 */
public final class ResizePoller {

    /** Default polling interval in milliseconds. */
    public static final int DEFAULT_INTERVAL_MS = 200;

    private final Consumer<TerminalSize> listener;
    private final int                   intervalMs;
    private TerminalSize                lastSize;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?>        task;

    /**
     * Creates a poller with the default interval ({@value #DEFAULT_INTERVAL_MS} ms).
     *
     * @param listener called on the poller thread when the terminal size changes.
     *                 Must not be {@code null}.
     */
    public ResizePoller(Consumer<TerminalSize> listener) {
        this(listener, DEFAULT_INTERVAL_MS);
    }

    /**
     * Creates a poller with a custom interval.
     *
     * @param listener   resize callback
     * @param intervalMs polling interval in milliseconds (must be > 0)
     */
    public ResizePoller(Consumer<TerminalSize> listener, int intervalMs) {
        if (listener   == null) throw new IllegalArgumentException("listener must not be null");
        if (intervalMs <= 0)    throw new IllegalArgumentException("intervalMs must be > 0");
        this.listener   = listener;
        this.intervalMs = intervalMs;
    }

    /**
     * Starts the background polling thread.
     * Captures the current size as the baseline.
     * Calling {@code start()} on an already-running poller is a no-op.
     */
    public synchronized void start() {
        if (executor != null) return;
        lastSize = TerminalSizeDetector.detect();
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "resize-poller");
            t.setDaemon(true);
            return t;
        });
        task = executor.scheduleAtFixedRate(this::poll, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the polling thread.
     * Calling {@code stop()} on an already-stopped poller is a no-op.
     */
    public synchronized void stop() {
        if (executor == null) return;
        if (task != null) task.cancel(false);
        executor.shutdownNow();
        executor = null;
        task     = null;
    }

    /** Returns {@code true} if the poller is currently running. */
    public synchronized boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }

    /** Returns the last detected terminal size, or {@code null} if not started. */
    public TerminalSize getLastSize() {
        return lastSize;
    }

    // --- Private ---

    private void poll() {
        TerminalSize current = TerminalSizeDetector.detect();
        if (!current.equals(lastSize)) {
            lastSize = current;
            try {
                listener.accept(current);
            } catch (Exception ignored) {
                // Never let listener exceptions kill the poller thread
            }
        }
    }
}
