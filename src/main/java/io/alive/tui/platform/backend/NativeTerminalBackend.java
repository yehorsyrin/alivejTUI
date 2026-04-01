package io.alive.tui.platform.backend;

import io.alive.tui.backend.TerminalBackend;
import io.alive.tui.backend.TerminalInitException;
import io.alive.tui.backend.TerminalRenderException;
import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.platform.input.AnsiKeyDecoder;
import io.alive.tui.platform.raw.AnsiWriter;
import io.alive.tui.platform.raw.PosixRawMode;
import io.alive.tui.platform.raw.ShutdownHook;
import io.alive.tui.platform.raw.WindowsRawMode;
import io.alive.tui.platform.raw.WindowsVtOutput;
import io.alive.tui.platform.signal.ResizePoller;
import io.alive.tui.platform.size.TerminalSize;
import io.alive.tui.platform.size.TerminalSizeDetector;
import io.alive.tui.style.Style;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * Native terminal backend — direct POSIX/Windows I/O without Lanterna.
 *
 * <p>Composes all low-level platform components:
 * <ul>
 *   <li>{@link WindowsVtOutput} / {@link WindowsRawMode} — Windows raw input + VT output.</li>
 *   <li>{@link PosixRawMode} — POSIX raw input.</li>
 *   <li>{@link AnsiWriter} — buffered ANSI escape sequence output.</li>
 *   <li>{@link AnsiKeyDecoder} — ANSI key sequence parser.</li>
 *   <li>{@link TerminalSizeDetector} — terminal size detection.</li>
 *   <li>{@link ResizePoller} — resize event delivery.</li>
 *   <li>{@link ShutdownHook} — terminal restore on JVM exit.</li>
 * </ul>
 *
 * <p>Use the factory methods {@link #createAuto()}, {@link #createForTesting(InputStream, OutputStream, int, int)}
 * rather than the constructor directly.
 *
 * @author Jarvis (AI)
 */
public final class NativeTerminalBackend implements TerminalBackend {

    // ── I/O ──────────────────────────────────────────────────────────────────

    private final InputStream  in;
    private final AnsiWriter   writer;

    // ── Size ──────────────────────────────────────────────────────────────────

    private volatile int width;
    private volatile int height;

    // ── Resize ────────────────────────────────────────────────────────────────

    private final ResizePoller resizePoller;
    private volatile Runnable  resizeListener;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    private volatile boolean initialised = false;
    private ShutdownHook shutdownHook;

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Creates a backend that reads from {@code System.in} and writes to
     * {@code System.out}, detecting OS automatically.
     */
    public static NativeTerminalBackend createAuto() {
        return new NativeTerminalBackend(System.in, System.out, 0, 0);
    }

    /**
     * Creates a backend with injected streams and fixed dimensions — intended
     * for unit tests where no real terminal is available.
     *
     * @param in    input stream (may be a {@link java.io.PipedInputStream})
     * @param out   output stream (may be a {@link java.io.ByteArrayOutputStream})
     * @param width  fixed width (0 = detect at init)
     * @param height fixed height (0 = detect at init)
     */
    public static NativeTerminalBackend createForTesting(InputStream in, OutputStream out,
                                                          int width, int height) {
        return new NativeTerminalBackend(in, out, width, height);
    }

    private NativeTerminalBackend(InputStream in, OutputStream out, int width, int height) {
        this.in     = in;
        this.writer = new AnsiWriter(out);
        this.width  = width;
        this.height = height;

        this.resizePoller = new ResizePoller(this::onResize);
    }

    // ── TerminalBackend ────────────────────────────────────────────────────────

    @Override
    public void init() {
        if (initialised) return;

        // 1. Enable platform-specific raw mode and VT output
        boolean onWindows = isWindows();
        if (onWindows) {
            WindowsVtOutput.enable();
            WindowsRawMode.enable();
        } else {
            PosixRawMode.enable();
        }

        // 2. Detect initial terminal size
        if (width == 0 || height == 0) {
            TerminalSize size = TerminalSizeDetector.detect();
            width  = size.cols();
            height = size.rows();
        }

        // 3. Prepare screen: alternate buffer, hide cursor, clear
        writer.alternateScreenOn();
        writer.hideCursor();
        writer.clearScreen();
        writer.flush();

        // 4. Register shutdown hook (restores terminal if JVM exits abnormally)
        shutdownHook = ShutdownHook.register(this::restoreTerminal);

        // 5. Start resize polling
        resizePoller.start();

        initialised = true;
    }

    @Override
    public void shutdown() {
        if (!initialised) return;
        initialised = false;

        resizePoller.stop();

        if (shutdownHook != null) {
            shutdownHook.cancel();
            shutdownHook = null;
        }

        restoreTerminal();
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
        writer.putChar(col, row, c, style);
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (UncheckedIOException e) {
            throw new TerminalRenderException("flush failed", e);
        }
    }

    @Override
    public void hideCursor() {
        writer.hideCursor();
    }

    @Override
    public void showCursor() {
        writer.showCursor();
    }

    @Override
    public void setCursor(int col, int row) {
        writer.moveCursor(col, row);
    }

    @Override
    public void clear() {
        writer.clearScreen();
    }

    @Override
    public KeyEvent readKey() throws InterruptedException {
        try {
            return readKeyInternal();
        } catch (IOException e) {
            throw new TerminalRenderException("readKey failed", e);
        }
    }

    @Override
    public KeyEvent readKey(long timeoutMs) throws InterruptedException {
        // Simple implementation: attempt non-blocking read with busy-wait.
        // For a proper implementation, use a separate reader thread + queue.
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (in.available() > 0) {
                    return readKey();
                }
            } catch (IOException e) {
                throw new TerminalRenderException("readKey(timeout) failed", e);
            }
            Thread.sleep(10);
        }
        return null;
    }

    @Override
    public void setResizeListener(Runnable onResize) {
        this.resizeListener = onResize;
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private KeyEvent readKeyInternal() throws IOException, InterruptedException {
        int first = in.read();
        if (first == -1) return KeyEvent.of(KeyType.EOF);

        byte[] seq;

        if (first == 0x1B) {
            // ESC — try to read more bytes for CSI/SS3 sequences
            seq = readEscapeSequence(first);
        } else {
            seq = new byte[]{(byte) first};
        }

        return AnsiKeyDecoder.decode(seq);
    }

    /**
     * Reads a complete escape sequence starting with the already-read ESC byte.
     * Uses {@code available()} to detect multi-byte sequences without blocking.
     */
    private byte[] readEscapeSequence(int esc) throws IOException {
        // Give the OS a brief moment to deliver the rest of the sequence
        // before checking available() — avoids splitting bracketed sequences.
        try { Thread.sleep(5); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int avail = in.available();
        if (avail == 0) {
            return new byte[]{(byte) esc}; // lone ESC
        }

        // Read up to 16 bytes (enough for all known sequences)
        byte[] rest = new byte[Math.min(avail, 16)];
        int read = in.read(rest, 0, rest.length);
        if (read <= 0) return new byte[]{(byte) esc};

        byte[] seq = new byte[1 + read];
        seq[0] = (byte) esc;
        System.arraycopy(rest, 0, seq, 1, read);
        return seq;
    }

    private void onResize(TerminalSize newSize) {
        width  = newSize.cols();
        height = newSize.rows();
        Runnable listener = resizeListener;
        if (listener != null) {
            try {
                listener.run();
            } catch (Exception ignored) {
                // listener must not crash the poller
            }
        }
    }

    private void restoreTerminal() {
        try {
            writer.showCursor();
            writer.alternateScreenOff();
            writer.flush();
        } catch (Exception ignored) {
            // best effort — we're shutting down
        }
        if (isWindows()) {
            WindowsVtOutput.disable();
            WindowsRawMode.disable();
        } else {
            PosixRawMode.disable();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }
}
