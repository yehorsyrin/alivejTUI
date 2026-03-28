package io.alive.tui.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for mouse handler support in {@link EventBus}.
 *
 * @author Jarvis (AI)
 */
class EventBusMouseTest {

    private EventBus bus;

    @BeforeEach
    void setUp() {
        bus = new EventBus();
    }

    @Test
    void registerMouse_handlerReceivesEvent() {
        List<MouseEvent> received = new ArrayList<>();
        bus.registerMouse(e -> { received.add(e); return false; });

        MouseEvent evt = MouseEvent.click(5, 5);
        bus.dispatchMouse(evt);

        assertEquals(1, received.size());
        assertEquals(evt, received.get(0));
    }

    @Test
    void registerMouse_duplicate_ignoredOnlyCalledOnce() {
        List<MouseEvent> received = new ArrayList<>();
        MouseHandler h = e -> { received.add(e); return false; };

        bus.registerMouse(h);
        bus.registerMouse(h);  // duplicate

        bus.dispatchMouse(MouseEvent.click(0, 0));
        assertEquals(1, received.size());
    }

    @Test
    void unregisterMouse_handlerNotCalledAfterRemoval() {
        List<MouseEvent> received = new ArrayList<>();
        MouseHandler h = e -> { received.add(e); return false; };

        bus.registerMouse(h);
        bus.unregisterMouse(h);
        bus.dispatchMouse(MouseEvent.click(0, 0));

        assertTrue(received.isEmpty());
    }

    @Test
    void dispatchMouse_consumingHandler_stopsChain() {
        List<String> order = new ArrayList<>();
        bus.registerMouse(e -> { order.add("first");  return true;  }); // consumes
        bus.registerMouse(e -> { order.add("second"); return false; }); // should not run

        bus.dispatchMouse(MouseEvent.click(0, 0));

        assertEquals(List.of("first"), order);
    }

    @Test
    void dispatchMouse_nonConsumingHandler_chainContinues() {
        List<String> order = new ArrayList<>();
        bus.registerMouse(e -> { order.add("first");  return false; });
        bus.registerMouse(e -> { order.add("second"); return false; });

        bus.dispatchMouse(MouseEvent.click(0, 0));

        assertEquals(List.of("first", "second"), order);
    }

    @Test
    void dispatch_keyEventWithMouse_routesToMouseHandlers() {
        List<MouseEvent> received = new ArrayList<>();
        bus.registerMouse(e -> { received.add(e); return false; });

        MouseEvent me = MouseEvent.scrollUp(3, 3);
        bus.dispatch(KeyEvent.ofMouse(me));

        assertEquals(1, received.size());
        assertEquals(me, received.get(0));
    }

    @Test
    void dispatchMouse_null_doesNothing() {
        // Should not throw
        bus.registerMouse(e -> false);
        bus.dispatchMouse(null);
    }

    @Test
    void clear_removesMouseHandlers() {
        bus.registerMouse(e -> false);
        assertEquals(1, bus.mouseHandlerCount());

        bus.clear();
        assertEquals(0, bus.mouseHandlerCount());
    }

    @Test
    void mouseHandlerCount_empty() {
        assertEquals(0, bus.mouseHandlerCount());
    }
}
