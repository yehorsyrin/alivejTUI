package io.alive.tui.core;

import io.alive.tui.backend.TerminalBackend;
import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.node.Button;
import io.alive.tui.node.ButtonNode;
import io.alive.tui.node.VBox;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Tab / Shift+Tab focus navigation (TASK-12).
 *
 * @author Jarvis (AI)
 */
class TabNavigationTest {

    static class ScriptedBackend implements TerminalBackend {
        final Queue<KeyEvent> keyQueue;
        ScriptedBackend(KeyEvent... keys) { this.keyQueue = new ArrayDeque<>(List.of(keys)); }
        @Override public void init() {}
        @Override public void shutdown() {}
        @Override public int getWidth()  { return 80; }
        @Override public int getHeight() { return 24; }
        @Override public void putChar(int col, int row, char c, Style style) {}
        @Override public void flush() {}
        @Override public void hideCursor() {}
        @Override public void showCursor() {}
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() {}
        @Override public void setResizeListener(Runnable r) {}
        @Override public KeyEvent readKey() {
            if (keyQueue.isEmpty()) return KeyEvent.of(KeyType.EOF);
            return keyQueue.poll();
        }
    }

    /**
     * Component that registers 3 buttons and tracks which was focused when TAB fired.
     */
    static class ThreeButtonsComponent extends Component {
        ButtonNode btn1, btn2, btn3;
        final List<String> focusLog = new ArrayList<>();

        @Override
        public void mount(Runnable onStateChange, io.alive.tui.event.EventBus eventBus, FocusManager focusManager) {
            super.mount(onStateChange, eventBus, focusManager);
            btn1 = new ButtonNode("B1", null); btn1.setKey("btn1");
            btn2 = new ButtonNode("B2", null); btn2.setKey("btn2");
            btn3 = new ButtonNode("B3", null); btn3.setKey("btn3");
            registerFocusable(btn1);
            registerFocusable(btn2);
            registerFocusable(btn3);
        }

        @Override
        public Node render() {
            // log current focus state after each render
            if (btn1 != null) {
                if (btn1.isFocused()) focusLog.add("btn1");
                else if (btn2.isFocused()) focusLog.add("btn2");
                else if (btn3.isFocused()) focusLog.add("btn3");
                else focusLog.add("none");
            }
            return VBox.of(
                Button.of("B1", null),
                Button.of("B2", null),
                Button.of("B3", null)
            );
        }
    }

    @Test
    void tabAdvancesFocusForward() {
        ThreeButtonsComponent comp = new ThreeButtonsComponent();
        ScriptedBackend backend = new ScriptedBackend(
            KeyEvent.of(KeyType.TAB),   // focus btn1
            KeyEvent.of(KeyType.TAB),   // focus btn2
            KeyEvent.of(KeyType.TAB),   // focus btn3
            KeyEvent.of(KeyType.ESCAPE)
        );
        AliveJTUI.run(comp, backend);

        // After 3 TABs, btn3 should be focused
        assertTrue(comp.btn3.isFocused());
        assertFalse(comp.btn1.isFocused());
        assertFalse(comp.btn2.isFocused());
    }

    @Test
    void tabWrapAroundFromLastToFirst() {
        ThreeButtonsComponent comp = new ThreeButtonsComponent();
        ScriptedBackend backend = new ScriptedBackend(
            KeyEvent.of(KeyType.TAB),   // btn1
            KeyEvent.of(KeyType.TAB),   // btn2
            KeyEvent.of(KeyType.TAB),   // btn3
            KeyEvent.of(KeyType.TAB),   // wraps to btn1
            KeyEvent.of(KeyType.ESCAPE)
        );
        AliveJTUI.run(comp, backend);
        assertTrue(comp.btn1.isFocused());
    }

    @Test
    void shiftTabGoesBackward() {
        ThreeButtonsComponent comp = new ThreeButtonsComponent();
        ScriptedBackend backend = new ScriptedBackend(
            KeyEvent.of(KeyType.TAB),        // btn1
            KeyEvent.of(KeyType.TAB),        // btn2
            KeyEvent.of(KeyType.SHIFT_TAB),  // back to btn1
            KeyEvent.of(KeyType.ESCAPE)
        );
        AliveJTUI.run(comp, backend);
        assertTrue(comp.btn1.isFocused());
        assertFalse(comp.btn2.isFocused());
    }

    @Test
    void shiftTabWrapsFromFirstToLast() {
        ThreeButtonsComponent comp = new ThreeButtonsComponent();
        ScriptedBackend backend = new ScriptedBackend(
            KeyEvent.of(KeyType.TAB),        // btn1
            KeyEvent.of(KeyType.SHIFT_TAB),  // wraps to btn3
            KeyEvent.of(KeyType.ESCAPE)
        );
        AliveJTUI.run(comp, backend);
        assertTrue(comp.btn3.isFocused());
    }

    @Test
    void noFocusableNodes_tabDoesNotThrow() {
        Component comp = new Component() {
            @Override public Node render() { return io.alive.tui.node.Text.of("hello"); }
        };
        ScriptedBackend backend = new ScriptedBackend(
            KeyEvent.of(KeyType.TAB),
            KeyEvent.of(KeyType.SHIFT_TAB),
            KeyEvent.of(KeyType.ESCAPE)
        );
        assertDoesNotThrow(() -> AliveJTUI.run(comp, backend));
    }
}
