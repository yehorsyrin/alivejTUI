package io.github.yehorsyrin.tui.event;

/**
 * A consumable handler for non-character keyboard events.
 *
 * <p>Return {@code true} to consume the event and stop further propagation.
 * Return {@code false} (or use {@link EventBus#register(KeyType, Runnable)}) to let
 * subsequent handlers in the chain also receive the event.
 *
 * @author Jarvis (AI)
 */
@FunctionalInterface
public interface KeyHandler {

    /**
     * Handles the key event.
     *
     * @return {@code true} if the event is consumed and propagation should stop;
     *         {@code false} to continue propagation
     */
    boolean handle();
}
