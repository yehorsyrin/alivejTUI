package io.github.yehorsyrin.tui.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.github.yehorsyrin.tui.event.KeyHandler;

class EventBusTest {

    private EventBus bus;

    @BeforeEach
    void setUp() { bus = new EventBus(); }

    @Test
    void dispatchFiresRegisteredHandler() {
        int[] count = {0};
        bus.register(KeyType.ENTER, () -> count[0]++);
        bus.dispatch(KeyEvent.of(KeyType.ENTER));
        assertEquals(1, count[0]);
    }

    @Test
    void dispatchFiresMultipleHandlers() {
        int[] count = {0};
        bus.register(KeyType.ENTER, () -> count[0]++);
        bus.register(KeyType.ENTER, () -> count[0]++);
        bus.dispatch(KeyEvent.of(KeyType.ENTER));
        assertEquals(2, count[0]);
    }

    @Test
    void dispatchDoesNotFireUnrelatedKey() {
        int[] count = {0};
        bus.register(KeyType.ENTER, () -> count[0]++);
        bus.dispatch(KeyEvent.of(KeyType.ESCAPE));
        assertEquals(0, count[0]);
    }

    @Test
    void unregisterRemovesHandler() {
        int[] count = {0};
        Runnable h = () -> count[0]++;
        bus.register(KeyType.ENTER, h);
        bus.unregister(KeyType.ENTER, h);
        bus.dispatch(KeyEvent.of(KeyType.ENTER));
        assertEquals(0, count[0]);
    }

    @Test
    void characterHandlerFired() {
        char[] captured = {'\0'};
        bus.registerCharacter(c -> captured[0] = c);
        bus.dispatch(KeyEvent.ofCharacter('x'));
        assertEquals('x', captured[0]);
    }

    @Test
    void characterHandlerUnregistered() {
        char[] captured = {'\0'};
        EventBus.CharacterHandler h = c -> captured[0] = c;
        bus.registerCharacter(h);
        bus.unregisterCharacter(h);
        bus.dispatch(KeyEvent.ofCharacter('x'));
        assertEquals('\0', captured[0]);
    }

    @Test
    void nonCharacterEventDoesNotFireCharacterHandlers() {
        char[] captured = {'\0'};
        bus.registerCharacter(c -> captured[0] = c);
        bus.dispatch(KeyEvent.of(KeyType.ENTER));
        assertEquals('\0', captured[0]);
    }

    @Test
    void clearRemovesAll() {
        int[] count = {0};
        bus.register(KeyType.ENTER, () -> count[0]++);
        bus.registerCharacter(c -> count[0]++);
        bus.clear();
        bus.dispatch(KeyEvent.of(KeyType.ENTER));
        bus.dispatch(KeyEvent.ofCharacter('a'));
        assertEquals(0, count[0]);
    }

    @Test
    void nullHandlerIgnored() {
        assertDoesNotThrow(() -> bus.register(KeyType.ENTER, (Runnable) null));
        assertDoesNotThrow(() -> bus.register(KeyType.ENTER, (KeyHandler) null));
        assertDoesNotThrow(() -> bus.registerCharacter(null));
        assertEquals(0, bus.handlerCount(KeyType.ENTER));
    }

    @Test
    void nullKeyIgnored() {
        assertDoesNotThrow(() -> bus.register(null, () -> {}));
    }

    @Test
    void nullEventIgnored() {
        assertDoesNotThrow(() -> bus.dispatch(null));
    }

    @Test
    void handlerCountAccurate() {
        bus.register(KeyType.ARROW_UP, () -> {});
        bus.register(KeyType.ARROW_UP, () -> {});
        assertEquals(2, bus.handlerCount(KeyType.ARROW_UP));
        assertEquals(0, bus.handlerCount(KeyType.ARROW_DOWN));
    }

    @Test
    void doubleRegisterSameHandlerDoesNotDoubleFire() {
        int[] count = {0};
        Runnable h = () -> count[0]++;
        bus.register(KeyType.ENTER, h);
        bus.register(KeyType.ENTER, h);  // duplicate — should be ignored
        bus.dispatch(KeyEvent.of(KeyType.ENTER));
        assertEquals(1, count[0]);
    }

    @Test
    void doubleRegisterSameCharacterHandlerDoesNotDoubleFire() {
        int[] count = {0};
        EventBus.CharacterHandler h = c -> count[0]++;
        bus.registerCharacter(h);
        bus.registerCharacter(h);  // duplicate — should be ignored
        bus.dispatch(KeyEvent.ofCharacter('a'));
        assertEquals(1, count[0]);
    }

    @Test
    void consumingHandlerStopsPropagation() {
        int[] count = {0};
        // first handler consumes the event
        bus.register(KeyType.ENTER, (KeyHandler) () -> { count[0]++; return true; });
        // second handler should NOT fire
        bus.register(KeyType.ENTER, (KeyHandler) () -> { count[0]++; return false; });
        bus.dispatch(KeyEvent.of(KeyType.ENTER));
        assertEquals(1, count[0]);
    }

    @Test
    void nonConsumingKeyHandlerContinuesPropagation() {
        int[] count = {0};
        bus.register(KeyType.ENTER, (KeyHandler) () -> { count[0]++; return false; });
        bus.register(KeyType.ENTER, (KeyHandler) () -> { count[0]++; return false; });
        bus.dispatch(KeyEvent.of(KeyType.ENTER));
        assertEquals(2, count[0]);
    }

    @Test
    void keyHandlerUnregisteredByReference() {
        int[] count = {0};
        KeyHandler h = () -> { count[0]++; return false; };
        bus.register(KeyType.ENTER, h);
        bus.unregister(KeyType.ENTER, h);
        bus.dispatch(KeyEvent.of(KeyType.ENTER));
        assertEquals(0, count[0]);
    }
}
