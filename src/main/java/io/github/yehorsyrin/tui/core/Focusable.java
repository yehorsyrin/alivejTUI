package io.github.yehorsyrin.tui.core;

/**
 * Marker interface for nodes that can receive keyboard focus.
 *
 * <p>Implement this interface on any {@link Node} subclass that should participate in
 * focus cycling managed by {@link FocusManager}.
 *
 * @author Jarvis (AI)
 */
public interface Focusable {

    /** Sets whether this node currently has keyboard focus. */
    void setFocused(boolean focused);

    /** Returns {@code true} if this node currently has keyboard focus. */
    boolean isFocused();

    /**
     * Returns a stable identifier used by {@link FocusManager#focusById(String)}.
     * Typically backed by {@link Node#getKey()}.
     */
    String getFocusId();
}
