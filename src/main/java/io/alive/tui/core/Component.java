package io.alive.tui.core;

import io.alive.tui.event.EventBus;
import io.alive.tui.event.KeyType;

import java.util.ArrayList;
import java.util.List;

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

    /** Tracks key handlers registered by this component so they can be removed on unmount. */
    private final List<KeyRegistration> keyRegistrations = new ArrayList<>();

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
     * Registers a handler for the given key type.
     * The handler is automatically removed when this component is {@link #unmount() unmounted}.
     *
     * <p>Call from the constructor or {@link #render()} (though constructor is preferred
     * to avoid duplicate registrations).
     */
    protected void onKey(KeyType key, Runnable handler) {
        if (eventBus == null || key == null || handler == null) return;
        eventBus.register(key, handler);
        keyRegistrations.add(new KeyRegistration(key, handler));
    }

    /**
     * Declare the UI for the current state.
     * Return a {@link Node} tree that represents the component's current visual.
     */
    public abstract Node render();

    /**
     * Called by the framework when the component is mounted into the UI tree.
     *
     * @param onStateChange callback to invoke when {@link #setState} is called
     * @param eventBus      the application-wide event bus
     */
    public void mount(Runnable onStateChange, EventBus eventBus) {
        this.onStateChange = onStateChange;
        this.eventBus = eventBus;
    }

    /**
     * Called by the framework when the component is removed from the UI tree.
     * Unregisters all key handlers.
     */
    public void unmount() {
        for (KeyRegistration reg : keyRegistrations) {
            if (eventBus != null) eventBus.unregister(reg.key(), reg.handler());
        }
        keyRegistrations.clear();
        this.onStateChange = null;
        this.eventBus = null;
    }

    /**
     * Renders the component, caches the result, and returns it.
     * Used by the framework to obtain the virtual tree for diffing.
     */
    public Node renderAndCache() {
        previousTree = render();
        return previousTree;
    }

    public Node getPreviousTree() {
        return previousTree;
    }

    private record KeyRegistration(KeyType key, Runnable handler) {}
}
