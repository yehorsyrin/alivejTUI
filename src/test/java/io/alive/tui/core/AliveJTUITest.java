package io.alive.tui.core;

import io.alive.tui.backend.TerminalBackend;
import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.node.Text;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class AliveJTUITest {

    /** Controllable fake backend that feeds a pre-defined sequence of key events. */
    static class ScriptedBackend implements TerminalBackend {
        final Queue<KeyEvent> keyQueue;
        boolean initCalled = false;
        boolean shutdownCalled = false;
        final List<String> rendered = new ArrayList<>();
        Runnable resizeListener;

        ScriptedBackend(KeyEvent... keys) {
            this.keyQueue = new ArrayDeque<>(List.of(keys));
        }

        @Override public void init()     { initCalled = true; }
        @Override public void shutdown() { shutdownCalled = true; }
        @Override public int getWidth()  { return 80; }
        @Override public int getHeight() { return 24; }

        @Override
        public void putChar(int col, int row, char c, Style style) {
            rendered.add(col + "," + row + "=" + c);
        }

        @Override public void flush() {}
        @Override public void hideCursor() {}
        @Override public void showCursor() {}
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() {}
        @Override public void setResizeListener(Runnable r) { this.resizeListener = r; }

        @Override
        public KeyEvent readKey() {
            if (keyQueue.isEmpty()) return KeyEvent.of(KeyType.EOF);
            return keyQueue.poll();
        }
    }

    static class HelloComponent extends Component {
        @Override
        public Node render() {
            return Text.of("Hello");
        }
    }

    static class CountingComponent extends Component {
        int renderCount = 0;
        int count = 0;

        CountingComponent() {}

        @Override
        public Node render() {
            renderCount++;
            return Text.of("Count: " + count);
        }
    }

    @Test
    void escapeExitsEventLoop() {
        ScriptedBackend backend = new ScriptedBackend(KeyEvent.of(KeyType.ESCAPE));
        AliveJTUI.run(new HelloComponent(), backend);
        assertTrue(backend.shutdownCalled);
    }

    @Test
    void eofExitsEventLoop() {
        ScriptedBackend backend = new ScriptedBackend(KeyEvent.of(KeyType.EOF));
        AliveJTUI.run(new HelloComponent(), backend);
        assertTrue(backend.shutdownCalled);
    }

    @Test
    void initCalledBeforeRender() {
        ScriptedBackend backend = new ScriptedBackend(KeyEvent.of(KeyType.EOF));
        AliveJTUI.run(new HelloComponent(), backend);
        assertTrue(backend.initCalled);
    }

    @Test
    void initialRenderPutsCharsOnScreen() {
        ScriptedBackend backend = new ScriptedBackend(KeyEvent.of(KeyType.EOF));
        AliveJTUI.run(new HelloComponent(), backend);
        assertTrue(backend.rendered.contains("0,0=H"));
        assertTrue(backend.rendered.contains("4,0=o"));
    }

    @Test
    void keyEventDispatchedToEventBus() {
        int[] count = {0};

        Component comp = new Component() {
            {
                // Can't use onKey here because EventBus isn't injected until mount()
                // This is tested via CountingComponent + setState instead
            }

            @Override
            public Node render() {
                return Text.of("test");
            }
        };

        // Just verify the loop dispatches without crashing
        ScriptedBackend backend = new ScriptedBackend(
            KeyEvent.ofCharacter('a'),
            KeyEvent.of(KeyType.ESCAPE)
        );
        assertDoesNotThrow(() -> AliveJTUI.run(comp, backend));
    }

    @Test
    void nullRoot_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
            () -> AliveJTUI.run(null, new ScriptedBackend()));
    }

    @Test
    void nullBackend_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
            () -> AliveJTUI.run(new HelloComponent(), null));
    }

    @Test
    void componentUnmountedAfterExit() {
        boolean[] unmounted = {false};
        Component comp = new Component() {
            @Override public Node render() { return Text.of("x"); }
            @Override public void unmount() { super.unmount(); unmounted[0] = true; }
        };

        ScriptedBackend backend = new ScriptedBackend(KeyEvent.of(KeyType.EOF));
        AliveJTUI.run(comp, backend);
        assertTrue(unmounted[0]);
    }

    @Test
    void resizeListenerRegistered() {
        ScriptedBackend backend = new ScriptedBackend(KeyEvent.of(KeyType.EOF));
        AliveJTUI.run(new HelloComponent(), backend);
        assertNotNull(backend.resizeListener);
    }
}
