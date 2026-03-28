package io.alive.tui.event;

/**
 * Handler for mouse events dispatched by {@link EventBus}.
 *
 * <p>Return {@code true} to consume the event and stop further propagation,
 * or {@code false} to let subsequent handlers process it.
 *
 * @author Jarvis (AI)
 */
@FunctionalInterface
public interface MouseHandler {
    /**
     * Handles a mouse event.
     *
     * @param event the mouse event
     * @return {@code true} if the event was consumed (stops propagation)
     */
    boolean handle(MouseEvent event);
}
