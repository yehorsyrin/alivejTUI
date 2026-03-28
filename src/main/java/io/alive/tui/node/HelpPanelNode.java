package io.alive.tui.node;

import io.alive.tui.core.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A read-only panel that displays a list of {@link KeyBinding} entries.
 *
 * <p>Each binding is rendered on its own line using the format:
 * <pre>{@code [KEY]  description}</pre>
 *
 * <p>Width is determined by the longest formatted line (capped at the available width during
 * layout). Height equals the number of bindings (minimum 1 when empty).
 *
 * <p>Use the {@link HelpPanel} factory to construct instances.
 *
 * @author Jarvis (AI)
 */
public class HelpPanelNode extends Node {

    private final List<KeyBinding> bindings;

    /**
     * Creates a {@code HelpPanelNode} with the given bindings.
     *
     * @param bindings ordered list of keybindings; {@code null} is treated as an empty list
     */
    public HelpPanelNode(List<KeyBinding> bindings) {
        this.bindings = bindings != null ? Collections.unmodifiableList(new ArrayList<>(bindings))
                                        : Collections.emptyList();
    }

    /** Returns an unmodifiable view of the registered keybindings. */
    public List<KeyBinding> getBindings() {
        return bindings;
    }

    /**
     * Formats a single {@link KeyBinding} as it will appear on screen.
     *
     * @param binding the binding to format
     * @return formatted string of the form {@code [KEY]  description}
     */
    public static String format(KeyBinding binding) {
        return "[" + binding.key() + "]  " + binding.description();
    }

    /**
     * Returns the length of the longest formatted binding line, or {@code 0} when there are
     * no bindings.
     */
    public int maxLineLength() {
        return bindings.stream()
                       .mapToInt(b -> format(b).length())
                       .max()
                       .orElse(0);
    }
}
