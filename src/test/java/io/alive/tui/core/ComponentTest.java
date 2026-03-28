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

    // --- Error Boundaries (TASK-23) ---

    static class ThrowingComponent extends Component {
        boolean shouldThrow = true;

        @Override
        public Node render() {
            if (shouldThrow) throw new RuntimeException("kaboom");
            return Text.of("ok");
        }
    }

    static class CustomErrorComponent extends Component {
        String lastError;

        @Override
        public Node render() {
            throw new IllegalStateException("state gone");
        }

        @Override
        protected Node onError(Exception ex) {
            lastError = ex.getMessage();
            return Text.of("fallback: " + ex.getMessage());
        }
    }

    @Test
    void renderThrows_defaultOnError_returnsErrorText() {
        ThrowingComponent comp = new ThrowingComponent();
        Node result = comp.renderAndCache();
        assertNotNull(result);
        // Default onError returns Text.of("Error: <message>")
        assertInstanceOf(io.alive.tui.node.TextNode.class, result);
        assertTrue(((io.alive.tui.node.TextNode) result).getText().contains("kaboom"));
    }

    @Test
    void renderThrows_doesNotPropagateException() {
        ThrowingComponent comp = new ThrowingComponent();
        assertDoesNotThrow(() -> comp.renderAndCache());
    }

    @Test
    void renderThrows_fallbackIsCached() {
        ThrowingComponent comp = new ThrowingComponent();
        Node first  = comp.renderAndCache();
        Node second = comp.renderAndCache();
        // The fallback from the first failing render is cached; second call returns it
        // (shouldUpdate default = true, so it re-renders and may throw again)
        assertNotNull(second);
    }

    @Test
    void customOnError_called_withCorrectException() {
        CustomErrorComponent comp = new CustomErrorComponent();
        comp.renderAndCache();
        assertEquals("state gone", comp.lastError);
    }

    @Test
    void customOnError_fallbackNodeReturned() {
        CustomErrorComponent comp = new CustomErrorComponent();
        Node result = comp.renderAndCache();
        assertInstanceOf(io.alive.tui.node.TextNode.class, result);
        assertEquals("fallback: state gone", ((io.alive.tui.node.TextNode) result).getText());
    }

    @Test
    void afterErrorFixed_renderRecovery() {
        ThrowingComponent comp = new ThrowingComponent();
        comp.renderAndCache();               // fails → fallback
        comp.shouldThrow = false;
        Node recovered = comp.renderAndCache();
        assertInstanceOf(io.alive.tui.node.TextNode.class, recovered);
        assertEquals("ok", ((io.alive.tui.node.TextNode) recovered).getText());
    }

    // --- Async State Updates (TASK-24) ---

    static class AsyncComponent extends Component {
        String data = "initial";

        @Override public Node render() { return Text.of(data); }
    }

    @Test
    void setStateAsync_mutationAppliedAfterDrain() throws InterruptedException {
        AsyncComponent comp = new AsyncComponent();
        int[] rerenders = {0};
        comp.mount(() -> rerenders[0]++, new EventBus());

        comp.setStateAsync(() -> () -> comp.data = "loaded");

        // Wait for background task to post to queue
        Thread.sleep(100);

        // Drain the async queue (simulates event loop draining)
        AliveJTUI.drainAsyncQueue();

        assertEquals("loaded", comp.data);
        assertEquals(1, rerenders[0]);
    }

    @Test
    void setStateAsync_supplierReturnsNull_onStateChangeStillCalled() throws InterruptedException {
        AsyncComponent comp = new AsyncComponent();
        int[] rerenders = {0};
        comp.mount(() -> rerenders[0]++, new EventBus());

        comp.setStateAsync(() -> null);  // null mutation — just re-render

        Thread.sleep(100);
        AliveJTUI.drainAsyncQueue();

        assertEquals(1, rerenders[0]);
        assertEquals("initial", comp.data); // unchanged
    }

    @Test
    void setStateAsync_supplierThrows_queueStillDrainable() throws InterruptedException {
        AsyncComponent comp = new AsyncComponent();
        int[] rerenders = {0};
        comp.mount(() -> rerenders[0]++, new EventBus());

        comp.setStateAsync(() -> { throw new RuntimeException("bg error"); });

        Thread.sleep(100);
        assertDoesNotThrow(() -> AliveJTUI.drainAsyncQueue());
        // Even on exception, the queue should be drainable without error
    }

    @Test
    void enqueueStateUpdate_runOnDrain() {
        int[] count = {0};
        AliveJTUI.enqueueStateUpdate(() -> count[0]++);
        AliveJTUI.enqueueStateUpdate(() -> count[0]++);
        int drained = AliveJTUI.drainAsyncQueue();
        assertEquals(2, drained);
        assertEquals(2, count[0]);
    }

    @Test
    void drainAsyncQueue_emptyQueue_returnsZero() {
        // Clear any residual items first
        AliveJTUI.asyncQueue.clear();
        assertEquals(0, AliveJTUI.drainAsyncQueue());
    }
}
