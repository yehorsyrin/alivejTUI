package io.github.yehorsyrin.tui.example;

import io.github.yehorsyrin.tui.core.Component;
import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.event.EventBus;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.node.*;
import io.github.yehorsyrin.tui.style.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Todo list root component.
 * Demonstrates: state, child components, key handling, layout, and styling.
 *
 * @author Jarvis (AI)
 */
public class TodoList extends Component {

    private String input = "";
    private final List<String> items = new ArrayList<>();
    private final ItemList itemList = new ItemList(items);
    private boolean keysWired = false;

    @Override
    public void mount(Runnable onStateChange, EventBus eventBus) {
        super.mount(onStateChange, eventBus);

        if (!keysWired) {
            keysWired = true;

            // Wire child list's key handlers on the same bus
            itemList.wireKeys(eventBus, onStateChange);

            // Character input: append to current input string
            eventBus.registerCharacter(c -> setState(() -> input += c));

            // Backspace: remove last character
            onKey(KeyType.BACKSPACE, () -> setState(() -> {
                if (!input.isEmpty()) input = input.substring(0, input.length() - 1);
            }));

            // Enter: add item and clear input
            onKey(KeyType.ENTER, () -> setState(() -> {
                if (!input.isBlank()) {
                    items.add(input.trim());
                    input = "";
                }
            }));
        }
    }

    @Override
    public Node render() {
        return VBox.of(
            Text.of(" AliveJTUI Todo").bold().color(Color.CYAN),
            Divider.horizontal(),
            itemList.render(),
            Divider.horizontal(),
            HBox.of(
                Text.of(" > ").color(Color.YELLOW),
                Text.of(input.isEmpty() ? "(type and press Enter to add)" : input)
                    .color(input.isEmpty() ? Color.BRIGHT_BLACK : Color.WHITE)
            ),
            Text.of(" [↑↓] navigate  [Enter] add  [Backspace] delete  [ESC] quit")
                .dim()
        );
    }

    public String getInput() { return input; }
    public List<String> getItems() { return items; }
    public ItemList getItemList() { return itemList; }
}
