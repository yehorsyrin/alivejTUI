package io.alive.tui.nativeio.backend;

import io.alive.tui.backend.TerminalBackend;
import io.alive.tui.backend.TerminalRenderException;
import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.nativeio.input.AnsiKeyDecoder;
import io.alive.tui.nativeio.raw.WindowsRawMode;
import io.alive.tui.nativeio.raw.WindowsVtOutput;
import io.alive.tui.nativeio.size.TerminalSize;
import io.alive.tui.nativeio.size.TerminalSizeDetector;
import io.alive.tui.style.Style;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Native terminal backend — replaces {@code LanternaBackend}.
 *
 * <p>Uses only JDK standard I/O + JNA Win32 bindings.
 * No Lanterna dependency required.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@link #init()}: enable VT output (Windows), raw mode, alternate screen, hide cursor</li>
 *   <li>Normal rendering / input loop</li>
 *   <li>{@link #shutdown()}: restore terminal, main screen, show cursor</li>
 * </ol>
 *
 * <p>Use {@link #create()} for automatic platform detection, or
 * {@link #createWindows()} / {@link #createPosix()} explicitly.
 *
 * @author Jarvis (AI)
 */
public final class NativeBackend implements TerminalBackend {

    private final OutputStream out;
    private final InputStream  in;
    private final AnsiWriter   writer;
    private final AnsiKeyDecoder decoder;

    private int width  = 80;
    private int height = 24;

    private Runnable resizeListener;
    private TerminalSize lastKnownSize;

    /**
     * Creates a NativeBackend using the given streams.
     *
     * @param out terminal output stream
     * @param in  terminal input stream (should be in raw mode after {@link #init()})
     */
    public NativeBackend(OutputStream out, InputStream in) {
        if (out == null) throw new IllegalArgumentException("out must not be null");
        if (in  == null) throw new IllegalArgumentException("in must not be null");
        this.out     = out;
        this.in      = in;
        this.writer  = new AnsiWriter(out);
        this.decoder = AnsiKeyDecoder.create();
    }

    /**
     * Auto-detects the platform and returns the appropriate backend instance
     * using {@link System#out} and {@link System#in}.
     */
    public static NativeBackend create() {
        return new NativeBackend(System.out, System.in);
    }

    /** Windows-specific factory (same as {@link #create()} but explicit). */
    public static NativeBackend createWindows() {
        return new NativeBackend(System.out, System.in);
    }

    /** POSIX-specific factory (same as {@link #create()} on non-Windows). */
    public static NativeBackend createPosix() {
        return new NativeBackend(System.out, System.in);
    }

    // --- TerminalBackend ---

    @Override
    public void init() {
        try {
            // 1. Enable VT processing on Windows stdout (no-op on POSIX)
            WindowsVtOutput.enable();

            // 2. Switch stdin to raw mode
            WindowsRawMode.enable();

            // 3. Switch to alternate screen buffer
            writer.alternateScreenOn();

            // 4. Hide cursor
            writer.hideCursor();

            // 5. Detect terminal size
            TerminalSize sz = TerminalSizeDetector.detect();
            width  = sz.cols();
            height = sz.rows();
            lastKnownSize = sz;

            writer.flush();
        } catch (IOException e) {
            throw new TerminalRenderException("NativeBackend.init() failed", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            writer.showCursor();
            writer.alternateScreenOff();
            writer.resetStyle();
            writer.flush();
        } catch (IOException ignored) {
        } finally {
            WindowsRawMode.disable();
            WindowsVtOutput.disable();
        }
    }

    @Override
    public int getWidth()  { return width;  }

    @Override
    public int getHeight() { return height; }

    @Override
    public void putChar(int col, int row, char c, Style style) {
        try {
            writer.putChar(col, row, c, style);
        } catch (IOException e) {
            throw new TerminalRenderException("putChar failed", e);
        }
    }

    @Override
    public void flush() {
        try {
            writer.flush();
            checkResize();
        } catch (IOException e) {
            throw new TerminalRenderException("flush failed", e);
        }
    }

    @Override
    public void hideCursor() {
        try { writer.hideCursor(); } catch (IOException e) {
            throw new TerminalRenderException("hideCursor failed", e);
        }
    }

    @Override
    public void showCursor() {
        try { writer.showCursor(); } catch (IOException e) {
            throw new TerminalRenderException("showCursor failed", e);
        }
    }

    @Override
    public void setCursor(int col, int row) {
        try { writer.moveCursor(col, row); writer.showCursor(); } catch (IOException e) {
            throw new TerminalRenderException("setCursor failed", e);
        }
    }

    @Override
    public void clear() {
        try { writer.clearScreen(); writer.flush(); } catch (IOException e) {
            throw new TerminalRenderException("clear failed", e);
        }
    }

    @Override
    public KeyEvent readKey() throws InterruptedException {
        try {
            byte[] seq = readSequence();
            return decoder.decode(seq);
        } catch (IOException e) {
            throw new TerminalRenderException("readKey failed", e);
        }
    }

    @Override
    public KeyEvent readKey(long timeoutMs) throws InterruptedException {
        try {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                if (in.available() > 0) {
                    return readKey();
                }
                Thread.sleep(5);
            }
            return null;
        } catch (IOException e) {
            throw new TerminalRenderException("readKey(timeout) failed", e);
        }
    }

    @Override
    public void setResizeListener(Runnable onResize) {
        this.resizeListener = onResize;
    }

    // --- Input reading ---

    /**
     * Reads a complete key sequence from the input stream.
     * For ESC sequences, continues reading bytes until the sequence is complete.
     */
    private byte[] readSequence() throws IOException, InterruptedException {
        int first = in.read();
        if (first == -1) return new byte[]{(byte) KeyType.EOF.ordinal()};

        if (first != 0x1B) {
            // Plain byte
            return new byte[]{(byte) first};
        }

        // ESC sequence — read ahead with a short timeout
        byte[] buf = new byte[16];
        buf[0] = 0x1B;
        int len = 1;

        long deadline = System.currentTimeMillis() + 50;
        while (len < buf.length && System.currentTimeMillis() < deadline) {
            if (in.available() <= 0) {
                Thread.sleep(2);
                continue;
            }
            int b = in.read();
            if (b == -1) break;
            buf[len++] = (byte) b;

            // Check if sequence is complete:
            // Final byte of CSI is in range 0x40–0x7E (@ to ~)
            byte last = buf[len - 1];
            if (len >= 3 && last >= 0x40 && last <= 0x7E) break;
            // SS3 is always 3 bytes
            if (len == 3 && buf[1] == 'O') break;
        }

        byte[] result = new byte[len];
        System.arraycopy(buf, 0, result, 0, len);
        return result;
    }

    // --- Resize detection ---

    private void checkResize() {
        TerminalSize current = TerminalSizeDetector.detect();
        if (lastKnownSize != null && current.equals(lastKnownSize)) return;
        lastKnownSize = current;
        width  = current.cols();
        height = current.rows();
        if (resizeListener != null) resizeListener.run();
    }
}
