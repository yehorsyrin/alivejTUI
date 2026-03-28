package io.alive.tui.core;

import io.alive.tui.backend.LanternaBackend;
import io.alive.tui.backend.TerminalBackend;
import io.alive.tui.event.EventBus;
import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.render.Renderer;
import io.alive.tui.style.Theme;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entry point for AliveJTUI applications.
 *
 * <pre>{@code
 * public static void main(String[] args) {
 *     AliveJTUI.run(new MyApp());
 * }
 * }</pre>
 *
 * <p>The event loop is single-threaded. ESC (or Ctrl+C via shutdown hook) exits cleanly.
 *
 * @author Jarvis (AI)
 */
public class AliveJTUI {

    private AliveJTUI() {}

    // --- Theme API ---

    private static Theme activeTheme = Theme.DARK;

    /**
     * Sets the active theme.  Components read it via {@link #getTheme()}.
     *
     * @param theme the theme to activate; {@code null} resets to {@link Theme#DARK}
     */
    public static void setTheme(Theme theme) {
        activeTheme = theme != null ? theme : Theme.DARK;
    }

    /** Returns the currently active theme (default: {@link Theme#DARK}). */
    public static Theme getTheme() { return activeTheme; }

    // --- Overlay API (single-instance, single-threaded) ---

    private static Renderer      activeRenderer;
    private static Runnable      activeRerenderCallback;

    // --- Timer API ---

    private static TimerManager  activeTimerManager;

    // --- Async State API (always available, daemon threads) ---

    static final ConcurrentLinkedQueue<Runnable> asyncQueue     = new ConcurrentLinkedQueue<>();
    static final AtomicInteger                   asyncInFlight  = new AtomicInteger(0);
    private static final ExecutorService asyncExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "alivejTUI-async");
        t.setDaemon(true);
        return t;
    });

    /**
     * Pushes a node to be rendered as an overlay on top of the current root tree.
     * Triggers an immediate re-render. Must be called from within the event loop thread.
     *
     * @param node the overlay node (e.g. a dialog or popup)
     */
    public static void pushOverlay(Node node) {
        if (activeRenderer != null) {
            activeRenderer.pushOverlay(node);
            if (activeRerenderCallback != null) activeRerenderCallback.run();
        }
    }

    /**
     * Removes the current overlay and triggers an immediate re-render.
     * Must be called from within the event loop thread.
     */
    public static void popOverlay() {
        if (activeRenderer != null) {
            activeRenderer.clearOverlay();
            if (activeRerenderCallback != null) activeRerenderCallback.run();
        }
    }

    // --- Timer API ---

    /**
     * Schedules a one-shot callback to run after {@code delayMs} milliseconds.
     * Must be called from within the event loop thread (e.g. inside a key handler or setState).
     * A re-render is triggered automatically after the callback fires.
     *
     * @param delayMs delay in milliseconds
     * @param task    the callback to run
     */
    public static void schedule(long delayMs, Runnable task) {
        if (activeTimerManager != null) activeTimerManager.schedule(delayMs, task);
    }

    /**
     * Schedules a repeating callback that fires every {@code intervalMs} milliseconds.
     * Useful for animations (e.g. advancing a {@link io.alive.tui.node.SpinnerNode} frame).
     * A re-render is triggered automatically after each firing.
     *
     * @param intervalMs interval in milliseconds
     * @param task       the callback to run on each interval
     */
    public static void scheduleRepeating(long intervalMs, Runnable task) {
        if (activeTimerManager != null) activeTimerManager.scheduleRepeating(intervalMs, task);
    }

    /**
     * Cancels all scheduled occurrences of the given task (by reference equality).
     *
     * @param task the task to cancel
     */
    public static void cancelTimer(Runnable task) {
        if (activeTimerManager != null) activeTimerManager.cancel(task);
    }

    // --- Async State API (package-private, used by Component) ---

    /**
     * Submits a task to the background executor. Tracks in-flight count so the
     * event loop knows to poll the async queue with a short timeout.
     */
    static void submitAsync(Runnable task) {
        asyncInFlight.incrementAndGet();
        asyncExecutor.submit(() -> {
            try {
                task.run();
            } finally {
                asyncInFlight.decrementAndGet();
            }
        });
    }

    /**
     * Enqueues a state mutation to be applied on the event loop thread.
     * Thread-safe: may be called from any thread.
     */
    static void enqueueStateUpdate(Runnable mutation) {
        if (mutation != null) asyncQueue.add(mutation);
    }

    /**
     * Drains and runs all pending async state mutations.
     * Must be called from the event loop thread. Returns the count drained.
     */
    static int drainAsyncQueue() {
        int count = 0;
        Runnable m;
        while ((m = asyncQueue.poll()) != null) {
            m.run();
            count++;
        }
        return count;
    }

    static boolean hasAsyncPending() {
        return !asyncQueue.isEmpty() || asyncInFlight.get() > 0;
    }

    /**
     * Starts the application with the given root component using the default Lanterna backend.
     * Blocks until the user presses ESC or the terminal signals EOF.
     */
    public static void run(Component root) {
        run(root, new LanternaBackend());
    }

    /**
     * Starts the application with a custom {@link TerminalBackend}.
     * Useful for testing with a fake backend.
     *
     * @param root    the root component
     * @param backend the terminal backend to use
     */
    public static void run(Component root, TerminalBackend backend) {
        if (root == null)    throw new IllegalArgumentException("root component must not be null");
        if (backend == null) throw new IllegalArgumentException("backend must not be null");

        backend.init();

        EventBus eventBus = new EventBus();
        FocusManager focusManager = new FocusManager();
        Renderer renderer = new Renderer(backend);

        // Wire resize: on resize, do a full redraw
        backend.setResizeListener(renderer::onResize);

        // Register shutdown hook for Ctrl+C
        Thread shutdownHook = new Thread(() -> {
            try { backend.shutdown(); } catch (Exception ignored) {}
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // Wire TAB / Shift+TAB to focus cycling
        eventBus.register(KeyType.TAB,       focusManager::focusNext);
        eventBus.register(KeyType.SHIFT_TAB, focusManager::focusPrev);

        // Expose overlay and timer APIs
        activeRenderer         = renderer;
        activeRerenderCallback = () -> renderer.render(root.renderAndCache());
        activeTimerManager     = new TimerManager();

        // Mount root component
        root.mount(activeRerenderCallback, eventBus, focusManager);

        // Initial render
        renderer.render(root.renderAndCache());

        try {
            eventLoop(backend, eventBus, activeTimerManager, activeRerenderCallback);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            activeRenderer         = null;
            activeRerenderCallback = null;
            activeTimerManager.clear();
            activeTimerManager     = null;
            root.unmount();
            eventBus.clear();
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM already shutting down
            }
            backend.shutdown();
        }
    }

    private static void eventLoop(TerminalBackend backend, EventBus eventBus,
                                  TimerManager timers, Runnable onTick)
        throws InterruptedException {
        while (true) {
            KeyEvent event;
            boolean needPoll = timers.hasPendingTimers() || hasAsyncPending();
            if (needPoll) {
                long waitMs = timers.hasPendingTimers()
                    ? Math.max(1L, Math.min(50L, timers.msUntilNext()))
                    : 50L;  // async-only: poll every 50 ms
                event = backend.readKey(waitMs);  // returns null on timeout
            } else {
                event = backend.readKey();         // blocks until key arrives
            }

            // Fire any due timers; triggers re-render if any fired
            timers.tick(onTick);

            // Drain async state mutations posted by background tasks
            if (drainAsyncQueue() > 0 && onTick != null) onTick.run();

            if (event == null) continue;  // timeout — timers / async may have fired
            if (event.type() == KeyType.ESCAPE || event.type() == KeyType.EOF) break;
            eventBus.dispatch(event);
        }
    }
}
