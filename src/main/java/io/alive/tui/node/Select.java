package io.alive.tui.node;

import java.util.Arrays;
import java.util.List;

/**
 * Factory for {@link SelectNode}.
 *
 * <pre>{@code
 * SelectNode sel = Select.of("Red", "Green", "Blue");
 * sel.setFocused(true);
 * // In event handlers:
 * bus.register(KeyType.Enter, () -> { sel.toggle(); markDirty(); });
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Select {

    private Select() {}

    /**
     * Creates a select node with the given options (first option selected by default).
     *
     * @param options at least one option label
     */
    public static SelectNode of(String... options) {
        return new SelectNode(Arrays.asList(options), 0, null);
    }

    /**
     * Creates a select node from a list of options.
     *
     * @param options option labels
     */
    public static SelectNode of(List<String> options) {
        return new SelectNode(options, 0, null);
    }

    /**
     * Creates a select node with a pre-selected index and change callback.
     *
     * @param options       option labels
     * @param selectedIndex initial selected index
     * @param onChange      called when selection changes; may be null
     */
    public static SelectNode of(List<String> options, int selectedIndex, Runnable onChange) {
        return new SelectNode(options, selectedIndex, onChange);
    }
}
