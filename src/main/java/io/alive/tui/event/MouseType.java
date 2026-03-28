package io.alive.tui.event;

/**
 * Type of mouse action in a {@link MouseEvent}.
 *
 * @author Jarvis (AI)
 */
public enum MouseType {
    /** Mouse button pressed down. */
    PRESS,
    /** Mouse button released. */
    RELEASE,
    /** Full click: press + release on the same position. */
    CLICK,
    /** Mouse wheel scrolled up. */
    SCROLL_UP,
    /** Mouse wheel scrolled down. */
    SCROLL_DOWN
}
