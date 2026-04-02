package io.github.yehorsyrin.tui.platform.backend;

import io.github.yehorsyrin.tui.backend.TerminalBackend;
import io.github.yehorsyrin.tui.backend.TerminalInitException;
import io.github.yehorsyrin.tui.backend.TerminalRenderException;
import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.platform.input.AnsiKeyDecoder;
import io.github.yehorsyrin.tui.platform.raw.AnsiWriter;
import io.github.yehorsyrin.tui.platform.raw.PosixRawMode;
import io.github.yehorsyrin.tui.platform.raw.ShutdownHook;
import io.github.yehorsyrin.tui.platform.raw.WindowsRawMode;
import io.github.yehorsyrin.tui.platform.raw.WindowsVtOutput;
import io.github.yehorsyrin.tui.platform.signal.ResizePoller;
import io.github.yehorsyrin.tui.platform.size.TerminalSize;
import io.github.yehorsyrin.tui.platform.size.TerminalSizeDetector;
import io.github.yehorsyrin.tui.style.Style;

import io.github.yehorsyrin.tui.style.Theme;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

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
    private final java.util.concurrent.atomic.AtomicReference<Runnable> resizeListener =
            new java.util.concurrent.atomic.AtomicReference<>();

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
        this.resizeListener.set(onResize);
    }

    @Override
    public void applyTheme(Theme theme) {
        writer.setDefaultBackground(theme != null ? theme.background() : null);
        // Buffer a clear so the next render frame fills empty terminal areas with
        // the new background color.  Not flushed here — sent atomically with the
        // putChar calls that follow in the same render pass, avoiding any flicker.
        writer.clearScreen();
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private KeyEvent readKeyInternal() throws IOException, InterruptedException {
        int first = in.read();
        if (first == -1) return KeyEvent.of(KeyType.EOF);

        byte[] seq;

        if (first == 0x1B) {
            // ESC — try to read more bytes for CSI/SS3 sequences
            seq = readEscapeSequence(first);
        } else if ((first & 0x80) != 0) {
            // UTF-8 multi-byte lead byte — read continuation bytes and decode
            return decodeUtf8(first);
        } else {
            seq = new byte[]{(byte) first};
        }

        return AnsiKeyDecoder.decode(seq);
    }

    /**
     * Reads a complete UTF-8 multi-byte sequence starting with {@code leadByte}
     * and returns the decoded character as a {@link KeyEvent}.
     * Handles 2-, 3-, and 4-byte sequences (Cyrillic, CJK, emoji, etc.).
     */
    private KeyEvent decodeUtf8(int leadByte) throws IOException {
        int extraBytes;
        if      ((leadByte & 0xE0) == 0xC0) extraBytes = 1; // 110xxxxx — U+0080..U+07FF
        else if ((leadByte & 0xF0) == 0xE0) extraBytes = 2; // 1110xxxx — U+0800..U+FFFF
        else if ((leadByte & 0xF8) == 0xF0) extraBytes = 3; // 11110xxx — U+10000..U+10FFFF
        else return AnsiKeyDecoder.decode(new byte[]{(byte) leadByte}); // lone continuation / invalid

        byte[] utf8 = new byte[1 + extraBytes];
        utf8[0] = (byte) leadByte;
        for (int i = 1; i <= extraBytes; i++) {
            int b = in.read();
            if (b == -1 || (b & 0xC0) != 0x80) {
                // Truncated or invalid sequence — skip silently
                return AnsiKeyDecoder.decode(new byte[]{(byte) leadByte});
            }
            utf8[i] = (byte) b;
        }

        String s = new String(utf8, StandardCharsets.UTF_8);
        if (s.isEmpty()) return AnsiKeyDecoder.decode(new byte[]{(byte) leadByte});
        return KeyEvent.ofCharacter(s.charAt(0));
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
        Runnable listener = resizeListener.get();
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
