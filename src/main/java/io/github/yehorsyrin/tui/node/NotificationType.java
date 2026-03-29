package io.github.yehorsyrin.tui.node;

/**
 * Semantic type of a {@link NotificationNode}.
 *
 * @author Jarvis (AI)
 */
public enum NotificationType {

    /** Informational message. Icon: {@code i} */
    INFO,

    /** Successful operation. Icon: {@code ✓} */
    SUCCESS,

    /** Non-critical warning. Icon: {@code !} */
    WARNING,

    /** Error or failure. Icon: {@code ✗} */
    ERROR
}
