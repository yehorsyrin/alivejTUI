package io.alive.tui.core;

import io.alive.tui.core.Node;
import io.alive.tui.node.NotificationNode;
import io.alive.tui.node.NotificationType;
import io.alive.tui.node.VBox;
import io.alive.tui.node.VBoxNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages a stack of temporary toast notifications and auto-dismisses them
 * after their configured duration using {@link AliveJTUI#schedule}.
 *
 * <pre>{@code
 * // In your component:
 * private final NotificationManager notifications = new NotificationManager(this::markDirty);
 *
 * // Show a toast:
 * notifications.show("Saved!", 3000);
 * notifications.show("File not found", 5000, NotificationType.ERROR);
 *
 * // In render():
 * Node overlay = notifications.buildOverlay();
 * if (overlay != null) AliveJTUI.pushOverlay(overlay);
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public class NotificationManager {

    private final List<NotificationNode> active  = new ArrayList<>();
    private final Runnable               onUpdate;

    /**
     * Creates a notification manager.
     *
     * @param onUpdate called on the event loop thread when the notification list changes
     *                 (a notification is added or auto-dismissed); may be {@code null}
     */
    public NotificationManager(Runnable onUpdate) {
        this.onUpdate = onUpdate;
    }

    /**
     * Shows an {@link NotificationType#INFO} notification for {@code durationMs} milliseconds.
     *
     * @param message    the text to display
     * @param durationMs how long (in ms) to display the notification before auto-dismiss
     */
    public void show(String message, long durationMs) {
        show(message, durationMs, NotificationType.INFO);
    }

    /**
     * Shows a notification for {@code durationMs} milliseconds.
     *
     * @param message    the text to display
     * @param durationMs how long (in ms) to display before auto-dismiss
     * @param type       the notification type ({@link NotificationType#INFO} by default)
     */
    public void show(String message, long durationMs, NotificationType type) {
        NotificationNode n = new NotificationNode(message, type);
        active.add(n);
        fireUpdate();
        AliveJTUI.schedule(durationMs, () -> {
            active.remove(n);
            fireUpdate();
        });
    }

    /**
     * Immediately dismisses all notifications.
     */
    public void clear() {
        active.clear();
        fireUpdate();
    }

    /**
     * Returns {@code true} if there are no active notifications.
     */
    public boolean isEmpty() { return active.isEmpty(); }

    /**
     * Returns the number of active notifications.
     */
    public int size() { return active.size(); }

    /**
     * Returns an unmodifiable snapshot of the currently active notifications,
     * in the order they were added (oldest first).
     */
    public List<NotificationNode> getActiveNotifications() {
        return Collections.unmodifiableList(new ArrayList<>(active));
    }

    /**
     * Builds a vertical stack of {@link NotificationNode} objects suitable for use
     * as an overlay, or returns {@code null} if there are no active notifications.
     *
     * <p>The caller is responsible for positioning and pushing the overlay:
     * <pre>{@code
     * Node overlay = notifications.buildOverlay();
     * if (overlay != null) AliveJTUI.pushOverlay(overlay);
     * }</pre>
     *
     * @return a {@link io.alive.tui.node.VBoxNode} of notifications, or {@code null} if empty
     */
    public Node buildOverlay() {
        if (active.isEmpty()) return null;
        List<NotificationNode> snap = getActiveNotifications();
        if (snap.size() == 1) return snap.get(0);
        return VBox.of(snap.toArray(new Node[0]));
    }

    private void fireUpdate() {
        if (onUpdate != null) onUpdate.run();
    }
}
