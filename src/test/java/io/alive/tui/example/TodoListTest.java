package io.alive.tui.example;

import io.alive.tui.core.AliveJTUI;
import io.alive.tui.core.Node;
import io.alive.tui.event.EventBus;
import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.backend.TerminalBackend;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class TodoListTest {

    static class ScriptedBackend implements TerminalBackend {
        final Queue<KeyEvent> keys;
        final List<String> rendered = new ArrayList<>();

        ScriptedBackend(KeyEvent... events) {
            this.keys = new ArrayDeque<>(List.of(events));
        }

        @Override public void init() {}
        @Override public void shutdown() {}
        @Override public int getWidth()  { return 80; }
        @Override public int getHeight() { return 24; }
        @Override public void putChar(int col, int row, char c, Style style) {
            rendered.add(col + "," + row + "=" + c);
        }
        @Override public void flush() {}
        @Override public void hideCursor() {}
        @Override public void showCursor() {}
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() {}
        @Override public void setResizeListener(Runnable r) {}
        @Override public KeyEvent readKey() {
            return keys.isEmpty() ? KeyEvent.of(KeyType.EOF) : keys.poll();
        }
    }

    private TodoList todoList;
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        todoList = new TodoList();
        eventBus = new EventBus();
        todoList.mount(() -> {}, eventBus);
    }

    @Test
    void initialInputIsEmpty() {
        assertEquals("", todoList.getInput());
    }

    @Test
    void initialItemsIsEmpty() {
        assertTrue(todoList.getItems().isEmpty());
    }

    @Test
    void characterKeyAppendsToInput() {
        eventBus.dispatch(KeyEvent.ofCharacter('H'));
        eventBus.dispatch(KeyEvent.ofCharacter('i'));
        assertEquals("Hi", todoList.getInput());
    }

    @Test
    void backspaceRemovesLastChar() {
        eventBus.dispatch(KeyEvent.ofCharacter('H'));
        eventBus.dispatch(KeyEvent.ofCharacter('i'));
        eventBus.dispatch(KeyEvent.of(KeyType.BACKSPACE));
        assertEquals("H", todoList.getInput());
    }

    @Test
    void backspaceOnEmptyInputDoesNotThrow() {
        assertDoesNotThrow(() -> eventBus.dispatch(KeyEvent.of(KeyType.BACKSPACE)));
        assertEquals("", todoList.getInput());
    }

    @Test
    void enterAddsItemAndClearsInput() {
        eventBus.dispatch(KeyEvent.ofCharacter('B'));
        eventBus.dispatch(KeyEvent.ofCharacter('u'));
        eventBus.dispatch(KeyEvent.ofCharacter('y'));
        eventBus.dispatch(KeyEvent.of(KeyType.ENTER));
        assertEquals(1, todoList.getItems().size());
        assertEquals("Buy", todoList.getItems().get(0));
        assertEquals("", todoList.getInput());
    }

    @Test
    void enterOnBlankInputDoesNotAddItem() {
        eventBus.dispatch(KeyEvent.ofCharacter(' '));
        eventBus.dispatch(KeyEvent.of(KeyType.ENTER));
        assertTrue(todoList.getItems().isEmpty());
    }

    @Test
    void arrowKeysNavigateList() {
        eventBus.dispatch(KeyEvent.ofCharacter('A'));
        eventBus.dispatch(KeyEvent.of(KeyType.ENTER));
        eventBus.dispatch(KeyEvent.ofCharacter('B'));
        eventBus.dispatch(KeyEvent.of(KeyType.ENTER));

        assertEquals(0, todoList.getItemList().getSelected());
        eventBus.dispatch(KeyEvent.of(KeyType.ARROW_DOWN));
        assertEquals(1, todoList.getItemList().getSelected());
        eventBus.dispatch(KeyEvent.of(KeyType.ARROW_UP));
        assertEquals(0, todoList.getItemList().getSelected());
    }

    @Test
    void arrowUpDoesNotGoAboveZero() {
        eventBus.dispatch(KeyEvent.of(KeyType.ARROW_UP));
        assertEquals(0, todoList.getItemList().getSelected());
    }

    @Test
    void renderReturnNonNull() {
        Node tree = todoList.render();
        assertNotNull(tree);
    }

    @Test
    void integrationTest_addItemViaAliveJTUI() {
        ScriptedBackend backend = new ScriptedBackend(
            KeyEvent.ofCharacter('T'),
            KeyEvent.ofCharacter('e'),
            KeyEvent.ofCharacter('a'),
            KeyEvent.of(KeyType.ENTER),
            KeyEvent.of(KeyType.ESCAPE)
        );
        TodoList root = new TodoList();
        AliveJTUI.run(root, backend);
        assertEquals(1, root.getItems().size());
        assertEquals("Tea", root.getItems().get(0));
    }
}
