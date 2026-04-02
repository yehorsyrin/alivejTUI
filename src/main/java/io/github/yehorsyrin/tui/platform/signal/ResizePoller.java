package io.github.yehorsyrin.tui.platform.signal;

import io.github.yehorsyrin.tui.platform.size.TerminalSize;
import io.github.yehorsyrin.tui.platform.size.TerminalSizeDetector;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Daemon thread that polls terminal size at a fixed interval and fires a
 * listener whenever the dimensions change.
 *
 * <p>Polling is a portable alternative to SIGWINCH signal handling and works
 * identically on Windows, Linux, and macOS.  The default poll interval is
 * {@value #DEFAULT_INTERVAL_MS} ms — fast enough for responsive resize handling
 * without measurable CPU overhead.
 *
 * <p>Usage:
 * <pre>{@code
 *   ResizePoller poller = new ResizePoller(newSize -> handleResize(newSize));
 *   poller.start();
 *   // ... run app ...
 *   poller.stop();
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class ResizePoller {

    /** Default poll interval in milliseconds. */
    public static final long DEFAULT_INTERVAL_MS = 200L;

    private final Consumer<TerminalSize> listener;
    private final long intervalMs;
    private volatile boolean running = false;
    private Thread thread;
    private final AtomicReference<TerminalSize> lastSize = new AtomicReference<>();

    /**
     * Creates a poller with the {@link #DEFAULT_INTERVAL_MS default interval}.
     *
     * @param listener called on the poller thread whenever the terminal is resized
     */
    public ResizePoller(Consumer<TerminalSize> listener) {
        this(listener, DEFAULT_INTERVAL_MS);
    }

    /**
     * Creates a poller with a custom interval.
     *
     * @param listener   called whenever the terminal size changes
     * @param intervalMs poll interval in milliseconds (must be > 0)
     */
    public ResizePoller(Consumer<TerminalSize> listener, long intervalMs) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        if (intervalMs <= 0) throw new IllegalArgumentException("intervalMs must be > 0");
        this.listener   = listener;
        this.intervalMs = intervalMs;
        this.lastSize.set(TerminalSizeDetector.detect());
    }

    /**
     * Starts the polling daemon thread.
     * Calling {@link #start()} on an already-running poller is a no-op.
     */
    public synchronized void start() {
        if (running) return;
        running = true;
        thread  = new Thread(this::pollLoop, "alivejTUI-resize-poller");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Stops the polling thread.
     * Returns immediately; the thread may finish its current sleep before stopping.
     */
    public synchronized void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    /**
     * Returns {@code true} if the poller is currently running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns the most recently observed terminal size.
     * Updated on every successful poll regardless of whether the size changed.
     */
    public TerminalSize getLastSize() {
        return lastSize.get();
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void pollLoop() {
        while (running) {
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (!running) break;

            TerminalSize current = TerminalSizeDetector.detect();
            if (!current.equals(lastSize.get())) {
                lastSize.set(current);
                try {
                    listener.accept(current);
                } catch (Exception ignored) {
                    // listener must not crash the poller
                }
            }
        }
    }
}
