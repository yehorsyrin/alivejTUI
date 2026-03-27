package io.alive.tui.event;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Central event bus for keyboard events.
 * Handlers are registered per {@link KeyType} and dispatched on the event loop thread.
 *
 * <p>All operations are single-threaded (event loop).
 *
 * @author Jarvis (AI)
 */
public class EventBus {

    private final Map<KeyType, List<Runnable>> handlers = new EnumMap<>(KeyType.class);
    private final List<CharacterHandler> characterHandlers = new ArrayList<>();

    /**
     * Register a handler for a specific non-character key.
     */
    public void register(KeyType key, Runnable handler) {
        if (key == null || handler == null) return;
        handlers.computeIfAbsent(key, k -> new ArrayList<>()).add(handler);
    }

    /**
     * Register a handler for character keys (any printable character).
     */
    public void registerCharacter(CharacterHandler handler) {
        if (handler != null) characterHandlers.add(handler);
    }

    /**
     * Unregister a specific handler for a key.
     */
    public void unregister(KeyType key, Runnable handler) {
        if (key == null || handler == null) return;
        List<Runnable> list = handlers.get(key);
        if (list != null) list.remove(handler);
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
            List<Runnable> list = handlers.get(event.type());
            if (list != null) {
                for (Runnable h : new ArrayList<>(list)) {
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
        List<Runnable> list = handlers.get(key);
        return list == null ? 0 : list.size();
    }

    public int characterHandlerCount() {
        return characterHandlers.size();
    }

    @FunctionalInterface
    public interface CharacterHandler {
        void handle(char c);
    }
}
