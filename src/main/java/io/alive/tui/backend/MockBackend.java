package io.alive.tui.backend;

import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.style.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * A fully-functional in-memory {@link TerminalBackend} for testing.
 *
 * <p>Captures all {@link #putChar} calls in a cell grid and in an ordered call log.
 * Key events can be pre-loaded via {@link #pushKey(KeyEvent)} for scripted playback.
 *
 * <pre>{@code
 * MockBackend backend = new MockBackend(80, 24);
 * backend.pushKey(KeyEvent.ofCharacter('q'));
 *
 * AliveJTUI.run(new MyApp(), backend);
 *
 * assertEquals('H', backend.getCharAt(0, 0));
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public class MockBackend implements TerminalBackend {

    /** A single recorded putChar call. */
    public record CellRecord(int col, int row, char character, Style style) {}

    private final int width;
    private final int height;

    private final char[][]  charGrid;
    private final Style[][] styleGrid;
    private final List<CellRecord> callLog = new ArrayList<>();
    private final Queue<KeyEvent>  keyQueue = new LinkedList<>();

    private boolean initialized = false;
    private int cursorCol = -1;
    private int cursorRow = -1;

    private Runnable resizeListener;

    /**
     * Creates a mock backend with the given dimensions.
     *
     * @param width  terminal width in columns; must be ≥ 1
     * @param height terminal height in rows; must be ≥ 1
     */
    public MockBackend(int width, int height) {
        if (width  < 1) throw new IllegalArgumentException("width must be ≥ 1");
        if (height < 1) throw new IllegalArgumentException("height must be ≥ 1");
        this.width      = width;
        this.height     = height;
        this.charGrid   = new char[height][width];
        this.styleGrid  = new Style[height][width];
    }

    // --- TerminalBackend ---

    @Override
    public void init() {
        initialized = true;
    }

    @Override
    public void shutdown() {
        initialized = false;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void putChar(int col, int row, char c, Style style) {
        callLog.add(new CellRecord(col, row, c, style));
        if (col >= 0 && col < width && row >= 0 && row < height) {
            charGrid[row][col]  = c;
            styleGrid[row][col] = style;
        }
    }

    @Override
    public void flush() {}

    @Override
    public void hideCursor() {
        cursorCol = -1;
        cursorRow = -1;
    }

    @Override
    public void showCursor() {}

    @Override
    public void setCursor(int col, int row) {
        this.cursorCol = col;
        this.cursorRow = row;
    }

    @Override
    public void clear() {
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                charGrid[r][c]  = '\0';
                styleGrid[r][c] = null;
            }
        }
        callLog.clear();
    }

    @Override
    public KeyEvent readKey() throws InterruptedException {
        KeyEvent k = keyQueue.poll();
        return k != null ? k : KeyEvent.of(KeyType.EOF);
    }

    @Override
    public KeyEvent readKey(long timeoutMs) throws InterruptedException {
        return keyQueue.poll();  // immediate poll — no blocking
    }

    @Override
    public void setResizeListener(Runnable onResize) {
        this.resizeListener = onResize;
    }

    // --- Test helpers ---

    /**
     * Enqueues a key event to be returned by the next {@link #readKey()} call.
     *
     * @param event the event to enqueue; must not be {@code null}
     */
    public void pushKey(KeyEvent event) {
        if (event == null) throw new IllegalArgumentException("event must not be null");
        keyQueue.add(event);
    }

    /**
     * Returns the character at the given cell position, or {@code '\0'} if the cell is empty.
     *
     * @param col column (0-based)
     * @param row row (0-based)
     */
    public char getCharAt(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) return '\0';
        return charGrid[row][col];
    }

    /**
     * Returns the {@link Style} at the given cell position, or {@code null}.
     *
     * @param col column (0-based)
     * @param row row (0-based)
     */
    public Style getStyleAt(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) return null;
        return styleGrid[row][col];
    }

    /**
     * Returns a string representation of the given row (all columns concatenated).
     *
     * @param row row index (0-based)
     */
    public String getRow(int row) {
        if (row < 0 || row >= height) return "";
        return new String(charGrid[row]);
    }

    /**
     * Returns an unmodifiable view of all recorded {@link CellRecord}s in call order.
     */
    public List<CellRecord> getCallLog() {
        return Collections.unmodifiableList(callLog);
    }

    /** Returns the current cursor column, or -1 if the cursor is hidden. */
    public int getCursorCol() { return cursorCol; }

    /** Returns the current cursor row, or -1 if the cursor is hidden. */
    public int getCursorRow() { return cursorRow; }

    /** Returns {@code true} if {@link #init()} has been called and {@link #shutdown()} has not. */
    public boolean isInitialized() { return initialized; }

    /**
     * Fires the resize listener (simulates a terminal resize event).
     * No-op if no listener has been registered.
     */
    public void simulateResize() {
        if (resizeListener != null) resizeListener.run();
    }
}
