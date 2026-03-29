package io.github.yehorsyrin.tui.node;

/**
 * Immutable record describing a single keybinding entry for display in a {@link HelpPanelNode}.
 *
 * @param key         short human-readable key name, e.g. {@code "↑"} or {@code "ESC"}
 * @param description description of the action triggered by that key
 * @author Jarvis (AI)
 */
public record KeyBinding(String key, String description) {

    /**
     * Compact constructor — rejects null values eagerly.
     */
    public KeyBinding {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        if (description == null) throw new IllegalArgumentException("description must not be null");
    }
}
