package io.alive.tui.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link UndoManager}.
 *
 * @author Jarvis (AI)
 */
class UndoManagerTest {

    private UndoManager manager;
    private List<String> log;

    @BeforeEach
    void setUp() {
        manager = new UndoManager();
        log = new ArrayList<>();
    }

    // --- Construction ---

    @Test
    void defaultMaxHistory() {
        assertEquals(100, new UndoManager().getMaxHistory());
    }

    @Test
    void customMaxHistory() {
        assertEquals(5, new UndoManager(5).getMaxHistory());
    }

    @Test
    void constructor_invalidMaxHistory_throws() {
        assertThrows(IllegalArgumentException.class, () -> new UndoManager(0));
    }

    // --- Record ---

    @Test
    void record_nullUndo_throws() {
        assertThrows(IllegalArgumentException.class, () -> manager.record(null, () -> {}));
    }

    @Test
    void record_nullRedo_throws() {
        assertThrows(IllegalArgumentException.class, () -> manager.record(() -> {}, null));
    }

    @Test
    void record_increasesUndoSize() {
        manager.record(() -> {}, () -> {});
        assertEquals(1, manager.undoSize());
    }

    @Test
    void record_clearsRedoStack() {
        manager.record(() -> {}, () -> {});
        manager.undo();
        assertEquals(1, manager.redoSize());

        manager.record(() -> {}, () -> {});  // new record clears redo
        assertEquals(0, manager.redoSize());
    }

    // --- Undo ---

    @Test
    void undo_emptyStack_returnsFalse() {
        assertFalse(manager.undo());
    }

    @Test
    void undo_executesUndoAction() {
        manager.record(() -> log.add("undo!"), () -> log.add("redo!"));
        manager.undo();
        assertEquals(List.of("undo!"), log);
    }

    @Test
    void undo_movesEntryToRedoStack() {
        manager.record(() -> {}, () -> {});
        manager.undo();
        assertEquals(0, manager.undoSize());
        assertEquals(1, manager.redoSize());
    }

    @Test
    void undo_multipleOps_lifoOrder() {
        manager.record(() -> log.add("A"), () -> {});
        manager.record(() -> log.add("B"), () -> {});
        manager.undo();
        manager.undo();
        assertEquals(List.of("B", "A"), log);  // most recent first
    }

    // --- Redo ---

    @Test
    void redo_emptyStack_returnsFalse() {
        assertFalse(manager.redo());
    }

    @Test
    void redo_executesRedoAction() {
        manager.record(() -> log.add("undo!"), () -> log.add("redo!"));
        manager.undo();
        log.clear();
        manager.redo();
        assertEquals(List.of("redo!"), log);
    }

    @Test
    void redo_movesEntryBackToUndoStack() {
        manager.record(() -> {}, () -> {});
        manager.undo();
        manager.redo();
        assertEquals(1, manager.undoSize());
        assertEquals(0, manager.redoSize());
    }

    @Test
    void undo_redo_cycle() {
        final String[] state = { "initial" };
        manager.record(
                () -> state[0] = "initial",
                () -> state[0] = "changed"
        );
        state[0] = "changed";

        manager.undo();
        assertEquals("initial", state[0]);

        manager.redo();
        assertEquals("changed", state[0]);

        manager.undo();
        assertEquals("initial", state[0]);
    }

    // --- canUndo / canRedo ---

    @Test
    void canUndo_falseWhenEmpty() {
        assertFalse(manager.canUndo());
    }

    @Test
    void canUndo_trueAfterRecord() {
        manager.record(() -> {}, () -> {});
        assertTrue(manager.canUndo());
    }

    @Test
    void canRedo_trueAfterUndo() {
        manager.record(() -> {}, () -> {});
        manager.undo();
        assertTrue(manager.canRedo());
    }

    // --- Max history ---

    @Test
    void maxHistory_oldestDropped() {
        UndoManager m = new UndoManager(3);
        for (int i = 0; i < 5; i++) {
            final int val = i;
            m.record(() -> log.add("u" + val), () -> {});
        }
        assertEquals(3, m.undoSize());
    }

    // --- Clear ---

    @Test
    void clear_emptysBothStacks() {
        manager.record(() -> {}, () -> {});
        manager.undo();
        manager.clear();
        assertEquals(0, manager.undoSize());
        assertEquals(0, manager.redoSize());
    }
}
