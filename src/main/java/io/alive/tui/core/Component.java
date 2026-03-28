package io.alive.tui.core;

import io.alive.tui.event.EventBus;
import io.alive.tui.event.KeyHandler;
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

    private record RunnableRegistration(KeyType key, Runnable handler) {}
    private record KeyHandlerRegistration(KeyType key, KeyHandler handler) {}
}
