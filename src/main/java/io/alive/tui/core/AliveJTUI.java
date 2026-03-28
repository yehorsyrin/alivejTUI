package io.alive.tui.core;

import io.alive.tui.backend.LanternaBackend;
import io.alive.tui.backend.TerminalBackend;
import io.alive.tui.event.EventBus;
import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.render.Renderer;

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

    // --- Overlay API (single-instance, single-threaded) ---

    private static Renderer      activeRenderer;
    private static Runnable      activeRerenderCallback;

    // --- Timer API ---

    private static TimerManager  activeTimerManager;

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
            if (timers.hasPendingTimers()) {
                long waitMs = Math.max(1L, timers.msUntilNext());
                event = backend.readKey(waitMs);  // returns null on timeout
            } else {
                event = backend.readKey();         // blocks until key arrives
            }

            // Fire any due timers; triggers re-render if any fired
            timers.tick(onTick);

            if (event == null) continue;  // timeout — timer(s) may have fired
            if (event.type() == KeyType.ESCAPE || event.type() == KeyType.EOF) break;
            eventBus.dispatch(event);
        }
    }
}
