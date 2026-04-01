package io.github.yehorsyrin.tui.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks reversible state mutations for undo/redo support.
 *
 * <p>Each recorded operation consists of an <em>undo</em> action (reverses the change)
 * and a <em>redo</em> action (re-applies it). Call {@link #record(Runnable, Runnable)}
 * immediately after each state mutation.
 *
 * <pre>{@code
 * String before = state;
 * state = newValue;
 * undoManager.record(
 *     () -> { state = before;   rerender(); },   // undo
 *     () -> { state = newValue; rerender(); }    // redo
 * );
 * }</pre>
 *
 * <p>The maximum history depth is configurable at construction time.
 * When the limit is exceeded, the oldest undo entry is discarded.
 *
 * @author Jarvis (AI)
 */
public class UndoManager {

    private static final int DEFAULT_MAX_HISTORY = 100;

    /** Holds a reversible operation as an (undo, redo) pair. */
    private record Entry(Runnable undo, Runnable redo) {}

    private final int           maxHistory;
    private final Deque<Entry>  undoStack = new ArrayDeque<>();
    private final Deque<Entry>  redoStack = new ArrayDeque<>();

    /** Creates an UndoManager with {@value #DEFAULT_MAX_HISTORY} levels of history. */
    public UndoManager() {
        this(DEFAULT_MAX_HISTORY);
    }

    /**
     * Creates an UndoManager with the given history depth limit.
     *
     * @param maxHistory maximum number of undoable operations to remember; must be ≥ 1
     */
    public UndoManager(int maxHistory) {
        if (maxHistory < 1) throw new IllegalArgumentException("maxHistory must be ≥ 1");
        this.maxHistory = maxHistory;
    }

    /**
     * Records a reversible mutation.
     *
     * <p>Any pending redo history is discarded when a new mutation is recorded.
     *
     * @param undo action that reverses the mutation; must not be {@code null}
     * @param redo action that re-applies the mutation; must not be {@code null}
     */
    public void record(Runnable undo, Runnable redo) {
        if (undo == null) throw new IllegalArgumentException("undo must not be null");
        if (redo == null) throw new IllegalArgumentException("redo must not be null");
        redoStack.clear();  // new change discards redo history
        undoStack.push(new Entry(undo, redo));
        if (undoStack.size() > maxHistory) {
            ((ArrayDeque<Entry>) undoStack).removeLast();
        }
    }

    /**
     * Undoes the most recent recorded mutation by executing its undo action and pushing
     * the entry onto the redo stack so it can be re-applied.
     *
     * @return {@code true} if an undo was performed; {@code false} if the undo stack is empty
     */
    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        Entry entry = undoStack.pop();
        entry.undo().run();
        redoStack.push(entry);
        return true;
    }

    /**
     * Re-applies the most recently undone mutation and pushes it back onto the undo stack.
     *
     * @return {@code true} if a redo was performed; {@code false} if the redo stack is empty
     */
    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        Entry entry = redoStack.pop();
        entry.redo().run();
        undoStack.push(entry);
        return true;
    }

    /** Returns {@code true} if there is at least one operation to undo. */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /** Returns {@code true} if there is at least one operation to redo. */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /** Returns the number of operations currently in the undo stack. */
    public int undoSize() {
        return undoStack.size();
    }

    /** Returns the number of operations currently in the redo stack. */
    public int redoSize() {
        return redoStack.size();
    }

    /** Clears both the undo and redo stacks. */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    /** Returns the configured maximum history depth. */
    public int getMaxHistory() {
        return maxHistory;
    }
}
