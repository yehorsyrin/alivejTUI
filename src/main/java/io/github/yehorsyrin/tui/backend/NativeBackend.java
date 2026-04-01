package io.github.yehorsyrin.tui.backend;

import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.style.Style;

/**
 * Native terminal backend — direct POSIX/Windows I/O without Lanterna.
 *
 * <p>TODO: implement using raw terminal I/O (POSIX tcsetattr / Windows ENABLE_VIRTUAL_TERMINAL_PROCESSING)
 * <ul>
 *   <li>POSIX: tcsetattr to set raw mode, read from stdin, write ANSI sequences to stdout</li>
 *   <li>Windows: SetConsoleMode with ENABLE_VIRTUAL_TERMINAL_PROCESSING + ENABLE_VIRTUAL_TERMINAL_INPUT</li>
 *   <li>Resize: SIGWINCH on POSIX, polling on Windows</li>
 *   <li>Size detection: ioctl TIOCGWINSZ on POSIX, GetConsoleScreenBufferInfo on Windows</li>
 * </ul>
 *
 * @author Jarvis (AI)
 */
public class NativeBackend implements TerminalBackend {

    // TODO: platform-specific raw mode handles

    @Override
    public void init() {
        throw new UnsupportedOperationException("NativeBackend is not yet implemented");
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("NativeBackend is not yet implemented");
    }

    @Override
    public int getWidth() {
        throw new UnsupportedOperationException("NativeBackend is not yet implemented");
    }

    @Override
    public int getHeight() {
        throw new UnsupportedOperationException("NativeBackend is not yet implemented");
    }

    @Override
    public void putChar(int col, int row, char c, Style style) {
        throw new UnsupportedOperationException("NativeBackend is not yet implemented");
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException("NativeBackend is not yet implemented");
    }

    @Override
    public void hideCursor() {
        throw new UnsupportedOperationException("NativeBackend is not yet implemented");
    }

    @Override
    public void showCursor() {
        throw new UnsupportedOperationException("NativeBackend is not yet implemented");
    }

    @Override
    public void setCursor(int col, int row) {
        throw new UnsupportedOperationException("NativeBackend is not yet implemented");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("NativeBackend is not yet implemented");
    }

    @Override
    public KeyEvent readKey() throws InterruptedException {
        throw new UnsupportedOperationException("NativeBackend is not yet implemented");
    }

    @Override
    public void setResizeListener(Runnable onResize) {
        throw new UnsupportedOperationException("NativeBackend is not yet implemented");
    }
}
