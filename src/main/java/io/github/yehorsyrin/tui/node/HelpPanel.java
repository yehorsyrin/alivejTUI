package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.event.EventBus;
import io.github.yehorsyrin.tui.event.KeyType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory for {@link HelpPanelNode}.
 *
 * <pre>{@code
 * // Simple construction
 * HelpPanel.of(
 *     new KeyBinding("↑/↓", "navigate"),
 *     new KeyBinding("ESC", "quit")
 * )
 *
 * // Derived from an EventBus
 * HelpPanel.forBus(bus, Map.of(KeyType.ESCAPE, "quit", KeyType.ENTER, "confirm"))
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class HelpPanel {

    private HelpPanel() {}

    /**
     * Creates a {@link HelpPanelNode} from a varargs array of {@link KeyBinding} entries.
     *
     * @param bindings the keybindings to display
     * @return a new {@code HelpPanelNode}
     */
    public static HelpPanelNode of(KeyBinding... bindings) {
        return new HelpPanelNode(bindings == null ? List.of() : Arrays.asList(bindings));
    }

    /**
     * Creates a {@link HelpPanelNode} from a list of {@link KeyBinding} entries.
     *
     * @param bindings the keybindings to display
     * @return a new {@code HelpPanelNode}
     */
    public static HelpPanelNode of(List<KeyBinding> bindings) {
        return new HelpPanelNode(bindings);
    }

    /**
     * Creates a {@link HelpPanelNode} by inspecting the keys registered in the given
     * {@link EventBus} and mapping them to human-readable descriptions.
     *
     * <p>Only keys present in {@code descriptions} are included; keys registered on the bus
     * but absent from the map are silently skipped.
     *
     * @param bus          the event bus to introspect
     * @param descriptions mapping from {@link KeyType} to description string
     * @return a new {@code HelpPanelNode}
     */
    public static HelpPanelNode forBus(EventBus bus, Map<KeyType, String> descriptions) {
        if (bus == null || descriptions == null) {
            return new HelpPanelNode(List.of());
        }
        Set<KeyType> registered = bus.getRegisteredKeys();
        List<KeyBinding> bindings = registered.stream()
                .filter(descriptions::containsKey)
                .map(k -> new KeyBinding(k.name(), descriptions.get(k)))
                .collect(Collectors.toList());
        return new HelpPanelNode(bindings);
    }
}
