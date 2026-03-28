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

    private static Renderer   activeRenderer;
    private static Runnable   activeRerenderCallback;

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

        // Expose overlay API
        activeRenderer = renderer;
        activeRerenderCallback = () -> renderer.render(root.renderAndCache());

        // Mount root component
        root.mount(activeRerenderCallback, eventBus, focusManager);

        // Initial render
        renderer.render(root.renderAndCache());

        try {
            eventLoop(backend, eventBus);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            activeRenderer          = null;
            activeRerenderCallback  = null;
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

    private static void eventLoop(TerminalBackend backend, EventBus eventBus)
        throws InterruptedException {
        while (true) {
            KeyEvent event = backend.readKey();

            if (event.type() == KeyType.ESCAPE || event.type() == KeyType.EOF) {
                break;
            }

            eventBus.dispatch(event);
        }
    }
}
