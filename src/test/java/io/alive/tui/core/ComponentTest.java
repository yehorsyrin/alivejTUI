package io.alive.tui.core;

import io.alive.tui.event.EventBus;
import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.node.Text;
import io.alive.tui.node.TextNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ComponentTest {

    static class CounterComponent extends Component {
        int count = 0;

        @Override
        public Node render() {
            return Text.of("Count: " + count);
        }
    }

    static class KeyAwareComponent extends Component {
        int upPresses = 0;

        KeyAwareComponent(EventBus bus) {
            mount(() -> {}, bus);
            onKey(KeyType.ARROW_UP, () -> upPresses++);
        }

        @Override
        public Node render() {
            return Text.of("Up: " + upPresses);
        }
    }

    @Test
    void setStateTriggesOnStateChange() {
        CounterComponent comp = new CounterComponent();
        int[] renders = {0};
        comp.mount(() -> renders[0]++, new EventBus());
        comp.setState(() -> comp.count++);
        assertEquals(1, comp.count);
        assertEquals(1, renders[0]);
    }

    @Test
    void setStateWithoutMountDoesNotThrow() {
        CounterComponent comp = new CounterComponent();
        assertDoesNotThrow(() -> comp.setState(() -> comp.count++));
        assertEquals(1, comp.count);
    }

    @Test
    void renderAndCacheSavesTree() {
        CounterComponent comp = new CounterComponent();
        assertNull(comp.getPreviousTree());
        Node tree = comp.renderAndCache();
        assertNotNull(tree);
        assertSame(tree, comp.getPreviousTree());
    }

    @Test
    void onKeyHandlerFiredByBus() {
        EventBus bus = new EventBus();
        KeyAwareComponent comp = new KeyAwareComponent(bus);
        bus.dispatch(KeyEvent.of(KeyType.ARROW_UP));
        assertEquals(1, comp.upPresses);
    }

    @Test
    void unmountRemovesKeyHandlers() {
        EventBus bus = new EventBus();
        KeyAwareComponent comp = new KeyAwareComponent(bus);
        comp.unmount();
        bus.dispatch(KeyEvent.of(KeyType.ARROW_UP));
        assertEquals(0, comp.upPresses); // handler was removed
    }

    @Test
    void unmountClearsStateChangeCallback() {
        CounterComponent comp = new CounterComponent();
        int[] renders = {0};
        comp.mount(() -> renders[0]++, new EventBus());
        comp.unmount();
        comp.setState(() -> comp.count++);
        assertEquals(0, renders[0]); // no re-render after unmount
    }
}
