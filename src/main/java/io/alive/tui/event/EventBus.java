package io.alive.tui.event;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Central event bus for keyboard events.
 * Handlers are registered per {@link KeyType} and dispatched on the event loop thread.
 *
 * <p>All operations are single-threaded (event loop).
 *
 * @author Jarvis (AI)
 */
public class EventBus {

    private final Map<KeyType, Set<Runnable>> handlers = new EnumMap<>(KeyType.class);
    private final Set<CharacterHandler> characterHandlers = new LinkedHashSet<>();

    /**
     * Register a handler for a specific non-character key.
     *
     * <p>Duplicate registrations of the same handler reference are silently ignored —
     * the handler will fire at most once per event.
     */
    public void register(KeyType key, Runnable handler) {
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
     * Unregister a specific handler for a key.
     */
    public void unregister(KeyType key, Runnable handler) {
        if (key == null || handler == null) return;
        Set<Runnable> set = handlers.get(key);
        if (set != null) set.remove(handler);
    }

    /**
     * Unregister a character handler.
     */
    public void unregisterCharacter(CharacterHandler handler) {
        characterHandlers.remove(handler);
    }

    /**
     * Dispatch an event to all registered handlers.
     */
    public void dispatch(KeyEvent event) {
        if (event == null) return;

        if (event.type() == KeyType.CHARACTER) {
            for (CharacterHandler h : new ArrayList<>(characterHandlers)) {
                h.handle(event.character());
            }
        } else {
            Set<Runnable> set = handlers.get(event.type());
            if (set != null) {
                for (Runnable h : new ArrayList<>(set)) {
                    h.run();
                }
            }
        }
    }

    /**
     * Remove all registered handlers.
     */
    public void clear() {
        handlers.clear();
        characterHandlers.clear();
    }

    public int handlerCount(KeyType key) {
        Set<Runnable> set = handlers.get(key);
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
