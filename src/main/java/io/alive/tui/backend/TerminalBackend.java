package io.alive.tui.backend;

import io.alive.tui.event.KeyEvent;
import io.alive.tui.style.Style;

/**
 * Abstraction over the underlying terminal library.
 * All rendering and input operations go through this interface.
 *
 * <h2>Error handling contract</h2>
 * <ul>
 *   <li>{@link #init()} and {@link #shutdown()} throw {@link TerminalInitException} on failure.</li>
 *   <li>{@link #putChar}, {@link #flush}, {@link #clear} and cursor methods throw
 *       {@link TerminalRenderException} on failure. These are generally fatal — the event loop
 *       should catch them, call {@link #shutdown()}, and exit.</li>
 *   <li>{@link #readKey()} returns {@link io.alive.tui.event.KeyType#EOF} only when the terminal
 *       signals an orderly end of input. Any I/O error is thrown as {@link TerminalRenderException}
 *       so the event loop can distinguish a clean EOF from a broken terminal.</li>
 * </ul>
 *
 * @author Jarvis (AI)
 */
public interface TerminalBackend {

    /**
     * Initializes the terminal (raw mode, alternate screen buffer).
     *
     * @throws TerminalInitException if initialization fails
     */
    void init();

    /**
     * Restores the terminal to its original state.
     *
     * @throws TerminalInitException if shutdown fails
     */
    void shutdown();

    /**
     * Returns the current terminal width in columns.
     * Must only be called after {@link #init()}.
     */
    int getWidth();

    /**
     * Returns the current terminal height in rows.
     * Must only be called after {@link #init()}.
     */
    int getHeight();

    /**
     * Puts a single character at the given position with the given style.
     *
     * @throws TerminalRenderException on I/O failure
     */
    void putChar(int col, int row, char c, Style style);

    /**
     * Flushes all pending changes to the terminal screen.
     * Also checks for terminal resize and fires the resize listener if registered.
     *
     * @throws TerminalRenderException on I/O failure
     */
    void flush();

    /**
     * Hides the terminal cursor.
     */
    void hideCursor();

    /**
     * Shows the terminal cursor at the position set by {@link #setCursor}.
     * Implementations that cannot show the cursor explicitly may treat this as a no-op.
     */
    void showCursor();

    /**
     * Moves the cursor to the given position.
     * Has no visible effect if the cursor is hidden.
     */
    void setCursor(int col, int row);

    /**
     * Clears the entire screen.
     *
     * @throws TerminalRenderException on I/O failure
     */
    void clear();

    /**
     * Blocks until a key is pressed and returns the corresponding event.
     * Returns {@link io.alive.tui.event.KeyType#EOF} only on orderly end-of-input.
     *
     * @throws InterruptedException   if the thread is interrupted while waiting
     * @throws TerminalRenderException on unexpected I/O failure
     */
    KeyEvent readKey() throws InterruptedException;

    /**
     * Registers a callback invoked when the terminal is resized.
     * Called from within {@link #flush()}, so it runs on the event loop thread.
     */
    void setResizeListener(Runnable onResize);
}
