package io.alive.tui.core;

import io.alive.tui.event.EventBus;
import io.alive.tui.event.KeyHandler;
import io.alive.tui.event.KeyType;
import io.alive.tui.core.Focusable;
import io.alive.tui.core.FocusManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Base class for all user-defined UI components.
 *
 * <p>Subclass and implement {@link #render()} to describe your UI.
 * Call {@link #setState(Runnable)} to mutate state and trigger a re-render.
 * Call {@link #onKey(KeyType, Runnable)} inside the constructor to register key handlers.
 *
 * <p>All methods run on the single event loop thread — no synchronization needed.
 *
 * @author Jarvis (AI)
 */
public abstract class Component {

    private Runnable onStateChange;
    private Node previousTree;
    private EventBus eventBus;
    private FocusManager focusManager;

    /** Tracks Runnable handlers registered by this component so they can be removed on unmount. */
    private final List<RunnableRegistration> runnableRegistrations = new ArrayList<>();

    /** Tracks KeyHandler handlers registered by this component so they can be removed on unmount. */
    private final List<KeyHandlerRegistration> keyHandlerRegistrations = new ArrayList<>();

    /**
     * Triggers a state mutation and schedules a re-render.
     *
     * <p>Usage: {@code setState(() -> this.count++)}
     */
    protected void setState(Runnable mutation) {
        mutation.run();
        if (onStateChange != null) {
            onStateChange.run();
        }
    }

    /**
     * Runs {@code task} asynchronously in a background thread.
     * The {@link Runnable} returned by the supplier is then queued and applied
     * on the event loop thread, triggering a re-render.
     *
     * <p>Usage:
     * <pre>{@code
     * setStateAsync(() -> {
     *     String result = slowNetworkFetch();   // runs in background
     *     return () -> this.data = result;      // applied on event loop thread
     * });
     * }</pre>
     *
     * <p>If the supplier returns {@code null}, the re-render is still triggered.
     * If the supplier throws, the exception is silently ignored.
     *
     * @param task supplier that computes the state change in a background thread
     */
    protected void setStateAsync(Supplier<Runnable> task) {
        final Runnable captured = this.onStateChange;
        AliveJTUI.submitAsync(() -> {
            Runnable mutation = null;
            try {
                mutation = task.get();
            } catch (Exception ignored) {
                // Background errors are swallowed; components should handle them internally
            }
            final Runnable m = mutation;
            AliveJTUI.enqueueStateUpdate(() -> {
                if (m != null) m.run();
                if (captured != null) captured.run();
            });
        });
    }

    /**
     * Registers a focusable node with the application-wide {@link FocusManager}.
     *
     * <p>Call from {@link #render()} or {@link #mount(Runnable, EventBus)} to make a node
     * participate in TAB-based focus cycling. Duplicate registrations are silently ignored.
     *
     * @param node the focusable node; {@code null} is ignored
     */
    protected void registerFocusable(Focusable node) {
        if (focusManager != null) focusManager.register(node);
    }

    /**
     * Returns the application-wide {@link FocusManager}, or {@code null} if not mounted.
     */
    protected FocusManager getFocusManager() {
        return focusManager;
    }

    /**
     * Registers a non-consuming handler for the given key type.
     * The handler is automatically removed when this component is {@link #unmount() unmounted}.
     *
     * <p>Call from the constructor or {@link #render()} (though constructor is preferred
     * to avoid duplicate registrations).
     */
    protected void onKey(KeyType key, Runnable handler) {
        if (eventBus == null || key == null || handler == null) return;
        eventBus.register(key, handler);
        runnableRegistrations.add(new RunnableRegistration(key, handler));
    }

    /**
     * Registers a consumable handler for the given key type.
     * Return {@code true} from the handler to consume the event and stop propagation.
     * The handler is automatically removed when this component is {@link #unmount() unmounted}.
     */
    protected void onKey(KeyType key, KeyHandler handler) {
        if (eventBus == null || key == null || handler == null) return;
        eventBus.register(key, handler);
        keyHandlerRegistrations.add(new KeyHandlerRegistration(key, handler));
    }

    /**
     * Declare the UI for the current state.
     * Return a {@link Node} tree that represents the component's current visual.
     */
    public abstract Node render();

    /**
     * Called by the framework when the component is mounted into the UI tree.
     *
     * <p>Subclasses may override this to register key handlers or perform other setup.
     * Always call {@code super.mount(onStateChange, eventBus)} first.
     *
     * @param onStateChange callback to invoke when {@link #setState} is called
     * @param eventBus      the application-wide event bus
     */
    public void mount(Runnable onStateChange, EventBus eventBus) {
        this.onStateChange = onStateChange;
        this.eventBus = eventBus;
    }

    /**
     * Called by the framework when the component is mounted with a {@link FocusManager}.
     * Delegates to {@link #mount(Runnable, EventBus)} so that subclass overrides are honoured.
     *
     * @param onStateChange callback to invoke when {@link #setState} is called
     * @param eventBus      the application-wide event bus
     * @param focusManager  the application-wide focus manager (may be {@code null})
     */
    public final void mount(Runnable onStateChange, EventBus eventBus, FocusManager focusManager) {
        this.focusManager = focusManager;
        mount(onStateChange, eventBus);
    }

    /**
     * Called by the framework when the component is removed from the UI tree.
     * Unregisters all key handlers.
     */
    public void unmount() {
        for (RunnableRegistration reg : runnableRegistrations) {
            if (eventBus != null) eventBus.unregister(reg.key(), reg.handler());
        }
        runnableRegistrations.clear();
        for (KeyHandlerRegistration reg : keyHandlerRegistrations) {
            if (eventBus != null) eventBus.unregister(reg.key(), reg.handler());
        }
        keyHandlerRegistrations.clear();
        this.onStateChange = null;
        this.eventBus = null;
        this.focusManager = null;
    }

    /**
     * Called when {@link #render()} throws an exception.
     * Override to provide a fallback node instead of crashing the event loop.
     *
     * <p>The default implementation returns a single-line error text node so the
     * application degrades gracefully rather than exiting.
     *
     * @param ex the exception thrown by {@link #render()}
     * @return a fallback node to display in place of the failed render
     */
    protected Node onError(Exception ex) {
        return io.alive.tui.node.Text.of("Error: " + ex.getMessage());
    }

    /**
     * Override to skip re-renders when state has not meaningfully changed.
     * Called by {@link #renderAndCache()} before invoking {@link #render()}.
     *
     * <p>The default implementation always returns {@code true} (always re-render).
     * Subclasses may return {@code false} to reuse the previous tree and skip
     * the render + diff cycle entirely — useful for expensive subtrees.
     *
     * <p>Note: this method is NOT called on the first render (when no previous tree exists).
     *
     * @return {@code true} to render fresh, {@code false} to reuse the previous tree
     */
    protected boolean shouldUpdate() {
        return true;
    }

    /**
     * Renders the component, caches the result, and returns it.
     * If {@link #shouldUpdate()} returns {@code false} and a cached tree exists,
     * the cached tree is returned without calling {@link #render()}.
     *
     * <p>If {@link #render()} throws an exception, {@link #onError(Exception)} is called
     * and its fallback node is cached and returned instead.
     */
    public Node renderAndCache() {
        if (previousTree != null && !shouldUpdate()) return previousTree;
        try {
            previousTree = render();
        } catch (Exception ex) {
            previousTree = onError(ex);
        }
        return previousTree;
    }

    public Node getPreviousTree() {
        return previousTree;
    }

    private record RunnableRegistration(KeyType key, Runnable handler) {}
    private record KeyHandlerRegistration(KeyType key, KeyHandler handler) {}
}
