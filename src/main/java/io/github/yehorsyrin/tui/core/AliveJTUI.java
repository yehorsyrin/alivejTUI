package io.github.yehorsyrin.tui.core;

import io.github.yehorsyrin.tui.backend.TerminalBackend;
import io.github.yehorsyrin.tui.platform.backend.Backends;
import io.github.yehorsyrin.tui.event.EventBus;
import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.node.ButtonNode;
import io.github.yehorsyrin.tui.render.Renderer;
import io.github.yehorsyrin.tui.style.Theme;

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
        if (activeBackend  != null) activeBackend.applyTheme(activeTheme);
        if (activeRenderer != null) activeRenderer.invalidate();
    }

    /** Returns the currently active theme (default: {@link Theme#DARK}). */
    public static Theme getTheme() { return activeTheme; }

    // --- Overlay API (single-instance, single-threaded) ---

    private static Renderer         activeRenderer;
    private static Runnable         activeRerenderCallback;
    private static TerminalBackend  activeBackend;

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
     * Takes effect on the next render pass. Must be called from within the event loop thread.
     *
     * @param node the overlay node (e.g. a dialog or popup)
     */
    public static void pushOverlay(Node node) {
        if (activeRenderer != null) {
            activeRenderer.pushOverlay(node);
        }
    }

    /**
     * Removes the current overlay.
     * Must be called from within the event loop thread.
     */
    public static void popOverlay() {
        if (activeRenderer != null) {
            activeRenderer.clearOverlay();
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
     * Useful for animations (e.g. advancing a {@link io.github.yehorsyrin.tui.node.SpinnerNode} frame).
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
     * Submits an {@link AsyncTask} for background execution.
     *
     * <p>The task's {@code work} callable runs on a background thread; the
     * {@code onSuccess} (or {@code onError}) callback is enqueued onto the event loop
     * thread and called during the next event loop tick.
     *
     * <p>May be called from any thread (not just the event loop).
     *
     * @param task the async task to execute
     * @param <T>  result type
     */
    public static <T> void runAsync(AsyncTask<T> task) {
        if (task == null) return;
        submitAsync(() -> {
            try {
                T result = task.getWork().call();
                enqueueStateUpdate(() -> task.getOnSuccess().accept(result));
            } catch (Exception ex) {
                if (task.getOnError() != null) {
                    enqueueStateUpdate(() -> task.getOnError().accept(ex));
                }
            }
        });
    }

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
     * Starts the application with the given root component.
     * Auto-selects the best backend: {@code SwingBackend} on GUI desktops,
     * {@code NativeTerminalBackend} in headless / server environments.
     * Blocks until the user presses ESC or the terminal signals EOF.
     */
    public static void run(Component root) {
        run(root, Backends.createAuto());
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

        // Expose overlay and timer APIs
        activeRenderer         = renderer;
        activeRerenderCallback = () -> renderer.render(root.renderAndCache());
        activeTimerManager     = new TimerManager();
        activeBackend          = backend;

        // Apply initial theme to backend (e.g. sets default fg/bg on SwingBackend)
        backend.applyTheme(activeTheme);

        // Mount root component BEFORE wiring system keys so that component
        // handlers registered in mount() fire first and can consume events.
        root.mount(activeRerenderCallback, eventBus, focusManager);

        // Wire TAB / Shift+TAB to focus cycling (non-consuming fallback —
        // components may register consuming handlers during mount() to override).
        eventBus.register(KeyType.TAB, () -> {
            focusManager.focusNext();
            Runnable cb = activeRerenderCallback;
            if (cb != null) cb.run();
        });
        eventBus.register(KeyType.SHIFT_TAB, () -> {
            focusManager.focusPrev();
            Runnable cb = activeRerenderCallback;
            if (cb != null) cb.run();
        });

        // Wire ENTER to trigger the currently-focused button's onClick
        eventBus.register(KeyType.ENTER, () -> {
            if (focusManager.getFocused() instanceof ButtonNode btn) {
                btn.click();
                Runnable cb = activeRerenderCallback;
                if (cb != null) cb.run();
            }
            return false; // don't consume — let other ENTER handlers run
        });

        // Initial render
        renderer.render(root.renderAndCache());

        try {
            eventLoop(backend, eventBus, activeTimerManager, activeRerenderCallback);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            activeRenderer         = null;
            activeRerenderCallback = null;
            activeBackend          = null;
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
