package io.github.yehorsyrin.tui.platform.swing;

import io.github.yehorsyrin.tui.backend.TerminalBackend;
import io.github.yehorsyrin.tui.backend.TerminalInitException;
import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.style.Color;
import io.github.yehorsyrin.tui.style.Style;
import io.github.yehorsyrin.tui.style.Theme;

/**
 * Swing-based terminal backend — opens a {@code JFrame} with a monospace
 * character grid and acts as a self-contained terminal emulator.
 *
 * <p>Intended for GUI desktop environments where no external terminal
 * is available.  Use {@link io.github.yehorsyrin.tui.platform.backend.Backends#createSwing()}
 * to obtain an instance.
 *
 * <p><b>Note:</b> full implementation is provided in STASK-01/02.
 * This file is a compile-time placeholder; {@link #create()} will be
 * replaced with the real factory in the STASK-02 commit.
 *
 * @author Jarvis (AI)
 */
public final class SwingBackend implements TerminalBackend {

    // Populated by STASK-01 (SwingTerminalPanel) and STASK-02 (full impl)
    private SwingTerminalPanel panel;
    private javax.swing.JFrame frame;
    private volatile int width;
    private volatile int height;
    private final java.util.concurrent.atomic.AtomicReference<Runnable> resizeListener =
            new java.util.concurrent.atomic.AtomicReference<>();
    private final java.util.concurrent.LinkedBlockingQueue<KeyEvent> keyQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();

    private SwingBackend() {}

    /**
     * Creates and returns a new SwingBackend.
     * Called by {@link io.github.yehorsyrin.tui.platform.backend.Backends#createSwing()}.
     */
    public static SwingBackend create() {
        return new SwingBackend();
    }

    @Override
    public void init() {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            throw new TerminalInitException("SwingBackend requires a display (headless=true)",
                    new IllegalStateException("headless environment"));
        }
        panel = new SwingTerminalPanel();

        frame = new javax.swing.JFrame("AliveJTUI");
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();

        // Derive initial size from panel preferred size
        java.awt.Dimension pref = panel.getPreferredSize();
        int cellW = panel.getCellWidth();
        int cellH = panel.getCellHeight();
        width  = pref.width  / cellW;
        height = pref.height / cellH;

        // Forward key events from Swing EDT to our blocking queue
        panel.setKeyListener(keyQueue::offer);

        // Notify on resize
        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int newCols = frame.getContentPane().getWidth()  / panel.getCellWidth();
                int newRows = frame.getContentPane().getHeight() / panel.getCellHeight();
                if (newCols > 0 && newRows > 0 && (newCols != width || newRows != height)) {
                    width  = newCols;
                    height = newRows;
                    panel.resize(width, height);
                    Runnable l = resizeListener.get();
                    if (l != null) l.run();
                }
            }
        });

        // macOS: hide Dock icon when launched as a GUI app
        System.setProperty("apple.awt.UIElement", "true");

        frame.setVisible(true);
    }

    @Override
    public void shutdown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    @Override public int getWidth()  { return width; }
    @Override public int getHeight() { return height; }

    @Override
    public void putChar(int col, int row, char c, Style style) {
        if (panel != null) panel.putChar(col, row, c, style);
    }

    @Override
    public void flush() {
        if (panel != null) panel.repaint();
    }

    @Override
    public void hideCursor() {
        if (panel != null) panel.setCursorVisible(false);
    }

    @Override
    public void showCursor() {
        if (panel != null) panel.setCursorVisible(true);
    }

    @Override
    public void setCursor(int col, int row) {
        if (panel != null) panel.setCursorPosition(col, row);
    }

    @Override
    public void clear() {
        if (panel != null) panel.clear();
    }

    @Override
    public KeyEvent readKey() throws InterruptedException {
        return keyQueue.take();
    }

    @Override
    public KeyEvent readKey(long timeoutMs) throws InterruptedException {
        return keyQueue.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @Override
    public void setResizeListener(Runnable onResize) {
        this.resizeListener.set(onResize);
    }

    @Override
    public void applyTheme(Theme theme) {
        if (panel == null) return;
        Color tuiBg = theme.background();
        Color tuiFg = theme.foreground().getForeground();
        java.awt.Color awtBg = tuiBg != null ? toAwtColor(tuiBg) : java.awt.Color.BLACK;
        java.awt.Color awtFg = tuiFg != null ? toAwtColor(tuiFg) : java.awt.Color.WHITE;
        javax.swing.SwingUtilities.invokeLater(() -> panel.setDefaultColors(awtFg, awtBg));
    }

    private static java.awt.Color toAwtColor(Color c) {
        return switch (c.getType()) {
            case ANSI_16  -> ansi16ToAwt(c.getAnsiIndex());
            case ANSI_256 -> ansi256ToAwt(c.getAnsiIndex());
            case RGB      -> new java.awt.Color(c.getR(), c.getG(), c.getB());
        };
    }

    private static java.awt.Color ansi16ToAwt(int index) {
        return switch (index) {
            case  0 -> new java.awt.Color(0,   0,   0);
            case  1 -> new java.awt.Color(170, 0,   0);
            case  2 -> new java.awt.Color(0,   170, 0);
            case  3 -> new java.awt.Color(170, 170, 0);
            case  4 -> new java.awt.Color(0,   0,   170);
            case  5 -> new java.awt.Color(170, 0,   170);
            case  6 -> new java.awt.Color(0,   170, 170);
            case  7 -> new java.awt.Color(170, 170, 170);
            case  8 -> new java.awt.Color(85,  85,  85);
            case  9 -> new java.awt.Color(255, 85,  85);
            case 10 -> new java.awt.Color(85,  255, 85);
            case 11 -> new java.awt.Color(255, 255, 85);
            case 12 -> new java.awt.Color(85,  85,  255);
            case 13 -> new java.awt.Color(255, 85,  255);
            case 14 -> new java.awt.Color(85,  255, 255);
            case 15 -> new java.awt.Color(255, 255, 255);
            default -> java.awt.Color.WHITE;
        };
    }

    private static java.awt.Color ansi256ToAwt(int idx) {
        if (idx < 16) return ansi16ToAwt(idx);
        if (idx < 232) {
            int n = idx - 16;
            int b = n % 6;
            int g = (n / 6) % 6;
            int r = n / 36;
            return new java.awt.Color(r == 0 ? 0 : 55 + r * 40,
                                       g == 0 ? 0 : 55 + g * 40,
                                       b == 0 ? 0 : 55 + b * 40);
        }
        int v = 8 + (idx - 232) * 10;
        return new java.awt.Color(v, v, v);
    }
}
