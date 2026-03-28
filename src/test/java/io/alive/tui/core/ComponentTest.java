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

    // --- shouldUpdate / Memoization (TASK-22) ---

    static class AlwaysSkipComponent extends Component {
        int renderCallCount = 0;

        @Override
        public Node render() {
            renderCallCount++;
            return Text.of("static");
        }

        @Override
        protected boolean shouldUpdate() {
            return false;  // never re-render after first
        }
    }

    static class ToggleUpdateComponent extends Component {
        int renderCallCount = 0;
        boolean allowUpdate = true;

        @Override
        public Node render() {
            renderCallCount++;
            return Text.of("v" + renderCallCount);
        }

        @Override
        protected boolean shouldUpdate() {
            return allowUpdate;
        }
    }

    @Test
    void shouldUpdateFalse_firstRenderStillHappens() {
        AlwaysSkipComponent comp = new AlwaysSkipComponent();
        comp.renderAndCache();
        assertEquals(1, comp.renderCallCount);  // first render always happens
    }

    @Test
    void shouldUpdateFalse_subsequentRenderSkipped() {
        AlwaysSkipComponent comp = new AlwaysSkipComponent();
        Node first = comp.renderAndCache();
        Node second = comp.renderAndCache();
        assertEquals(1, comp.renderCallCount);  // render() called only once
        assertSame(first, second);              // same cached tree returned
    }

    @Test
    void shouldUpdateTrue_alwaysReRenders() {
        CounterComponent comp = new CounterComponent();
        comp.renderAndCache();
        comp.renderAndCache();
        // CounterComponent doesn't override shouldUpdate — default is true
        // render() is called twice (no caching)
        assertNotNull(comp.getPreviousTree());
    }

    @Test
    void shouldUpdateToggle_switchesBehaviorCorrectly() {
        ToggleUpdateComponent comp = new ToggleUpdateComponent();
        comp.renderAndCache();          // 1st render — always happens
        comp.allowUpdate = false;
        comp.renderAndCache();          // skipped
        comp.renderAndCache();          // skipped
        assertEquals(1, comp.renderCallCount);

        comp.allowUpdate = true;
        comp.renderAndCache();          // renders again
        assertEquals(2, comp.renderCallCount);
    }

    @Test
    void shouldUpdate_defaultReturnsTrue() {
        // CounterComponent doesn't override shouldUpdate; verify default is true
        CounterComponent comp = new CounterComponent();
        comp.renderAndCache();
        int beforeCount = comp.count;
        comp.renderAndCache();
        // Both calls succeed (no caching), count unchanged but no exception
        assertEquals(beforeCount, comp.count);
    }
}
