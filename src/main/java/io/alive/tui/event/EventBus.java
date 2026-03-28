package io.alive.tui.event;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Central event bus for keyboard events.
 * Handlers are registered per {@link KeyType} and dispatched on the event loop thread.
 *
 * <p>All operations are single-threaded (event loop).
 *
 * <p>Handlers are invoked in registration order. A {@link KeyHandler} that returns {@code true}
 * consumes the event — subsequent handlers in the chain will not be called.
 * Use {@link #register(KeyType, Runnable)} for non-consuming handlers (always returns {@code false}).
 *
 * @author Jarvis (AI)
 */
public class EventBus {

    /** Primary handler storage keyed by KeyType. */
    private final Map<KeyType, Set<KeyHandler>> handlers = new EnumMap<>(KeyType.class);

    /**
     * Tracks Runnable → KeyHandler wrappers so that registering the same Runnable twice
     * de-duplicates, and unregister-by-Runnable works correctly.
     */
    private final Map<KeyType, Map<Runnable, KeyHandler>> runnableWrappers = new EnumMap<>(KeyType.class);

    private final Set<CharacterHandler> characterHandlers = new LinkedHashSet<>();

    /**
     * Register a non-consuming handler for a specific non-character key.
     *
     * <p>Duplicate registrations of the same handler reference are silently ignored.
     * The handler never consumes the event — all subsequent handlers also fire.
     */
    public void register(KeyType key, Runnable handler) {
        if (key == null || handler == null) return;
        Map<Runnable, KeyHandler> wrappers = runnableWrappers.computeIfAbsent(key, k -> new LinkedHashMap<>());
        if (wrappers.containsKey(handler)) return;  // already registered — ignore duplicate
        KeyHandler wrapper = () -> { handler.run(); return false; };
        wrappers.put(handler, wrapper);
        handlers.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(wrapper);
    }

    /**
     * Register a consumable handler for a specific non-character key.
     *
     * <p>Duplicate registrations of the same handler reference are silently ignored.
     * Return {@code true} from the handler to stop propagation.
     */
    public void register(KeyType key, KeyHandler handler) {
        if (key == null || handler == null) return;
        handlers.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(handler);
    }

    /**
     * Register a handler for character keys (any printable character).
     *
     * <p>Duplicate registrations of the same handler reference are silently ignored.
     */
    public void registerCharacter(CharacterHandler handler) {
        if (handler != null) characterHandlers.add(handler);
    }

    /**
     * Unregister a non-consuming handler previously registered via {@link #register(KeyType, Runnable)}.
     */
    public void unregister(KeyType key, Runnable handler) {
        if (key == null || handler == null) return;
        Map<Runnable, KeyHandler> wrappers = runnableWrappers.get(key);
        if (wrappers == null) return;
        KeyHandler wrapper = wrappers.remove(handler);
        if (wrapper != null) {
            Set<KeyHandler> set = handlers.get(key);
            if (set != null) set.remove(wrapper);
        }
    }

    /**
     * Unregister a consumable handler previously registered via {@link #register(KeyType, KeyHandler)}.
     */
    public void unregister(KeyType key, KeyHandler handler) {
        if (key == null || handler == null) return;
        Set<KeyHandler> set = handlers.get(key);
        if (set != null) set.remove(handler);
    }

    /**
     * Unregister a character handler.
     */
    public void unregisterCharacter(CharacterHandler handler) {
        characterHandlers.remove(handler);
    }

    /**
     * Dispatch an event to registered handlers.
     *
     * <p>For non-character keys, handlers are called in registration order.
     * Dispatch stops at the first handler that returns {@code true} (consumed).
     */
    public void dispatch(KeyEvent event) {
        if (event == null) return;

        if (event.type() == KeyType.CHARACTER) {
            for (CharacterHandler h : new ArrayList<>(characterHandlers)) {
                h.handle(event.character());
            }
        } else {
            Set<KeyHandler> set = handlers.get(event.type());
            if (set != null) {
                for (KeyHandler h : new ArrayList<>(set)) {
                    if (h.handle()) break;  // consumed — stop propagation
                }
            }
        }
    }

    /**
     * Remove all registered handlers.
     */
    public void clear() {
        handlers.clear();
        runnableWrappers.clear();
        characterHandlers.clear();
    }

    public int handlerCount(KeyType key) {
        Set<KeyHandler> set = handlers.get(key);
        return set == null ? 0 : set.size();
    }

    public int characterHandlerCount() {
        return characterHandlers.size();
    }

    @FunctionalInterface
    public interface CharacterHandler {
        void handle(char c);
    }
}
