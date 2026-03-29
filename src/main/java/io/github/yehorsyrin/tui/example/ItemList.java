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
 * Scrollable list of todo items with keyboard navigation (↑↓).
 *
 * <p>Must be mounted before use; key handlers are registered on the shared EventBus.
 *
 * @author Jarvis (AI)
 */
public class ItemList extends Component {

    private final List<String> items;
    private int selected;

    public ItemList(List<String> items) {
        this.items    = items;
        this.selected = 0;
    }

    /** Called by the parent component after it receives its own EventBus from mount(). */
    public void wireKeys(EventBus eventBus, Runnable onStateChange) {
        mount(onStateChange, eventBus);
        onKey(KeyType.ARROW_UP,   () -> setState(() -> selected = Math.max(0, selected - 1)));
        onKey(KeyType.ARROW_DOWN, () -> setState(() -> selected = Math.min(
            Math.max(0, items.size() - 1), selected + 1)));
    }

    @Override
    public Node render() {
        if (items.isEmpty()) {
            return Text.of("  (no items yet)").dim();
        }

        List<Node> rows = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            String prefix = i == selected ? "> " : "  ";
            TextNode row = Text.of(prefix + items.get(i));
            if (i == selected) row.color(Color.GREEN).bold();
            rows.add(row);
        }
        return VBox.of(rows.toArray(new Node[0]));
    }

    public int getSelected() { return selected; }
}
