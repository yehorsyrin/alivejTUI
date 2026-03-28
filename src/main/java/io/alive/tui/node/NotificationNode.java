package io.alive.tui.node;

import io.alive.tui.core.Node;
import io.alive.tui.style.Color;
import io.alive.tui.style.Style;

/**
 * A single toast / notification message rendered as {@code [icon] message}.
 *
 * <p>Notifications are typically managed by {@link NotificationManager} and
 * auto-dismissed after a configurable duration.
 *
 * @author Jarvis (AI)
 */
public class NotificationNode extends Node {

    private static final char ICON_INFO    = 'i';
    private static final char ICON_SUCCESS = '✓';
    private static final char ICON_WARNING = '!';
    private static final char ICON_ERROR   = '✗';

    private final String           message;
    private final NotificationType type;

    /**
     * Creates a notification node.
     *
     * @param message the text to display
     * @param type    the notification type; determines icon and style
     */
    public NotificationNode(String message, NotificationType type) {
        this.message = message != null ? message : "";
        this.type    = type    != null ? type    : NotificationType.INFO;
    }

    /** Returns the notification message text. */
    public String getMessage() { return message; }

    /** Returns the notification type. */
    public NotificationType getType() { return type; }

    /** Returns the icon character for this notification's type. */
    public char getIcon() {
        return switch (type) {
            case INFO    -> ICON_INFO;
            case SUCCESS -> ICON_SUCCESS;
            case WARNING -> ICON_WARNING;
            case ERROR   -> ICON_ERROR;
        };
    }

    /**
     * Returns the rendered text for this notification: {@code [icon] message}.
     * Used by {@link io.alive.tui.diff.TreeFlattener}.
     */
    public String renderText() {
        return "[" + getIcon() + "] " + message;
    }

    /**
     * Returns the display style for this notification type.
     * Success → green, Warning → yellow, Error → red, Info → default bold.
     */
    public Style getStyle() {
        return switch (type) {
            case INFO    -> Style.DEFAULT.withBold(true);
            case SUCCESS -> Style.DEFAULT.withForeground(Color.GREEN).withBold(true);
            case WARNING -> Style.DEFAULT.withForeground(Color.YELLOW).withBold(true);
            case ERROR   -> Style.DEFAULT.withForeground(Color.RED).withBold(true);
        };
    }
}
