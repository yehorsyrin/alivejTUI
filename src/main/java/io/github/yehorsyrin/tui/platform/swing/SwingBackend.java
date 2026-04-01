package io.github.yehorsyrin.tui.platform.swing;

import io.github.yehorsyrin.tui.backend.TerminalBackend;
import io.github.yehorsyrin.tui.backend.TerminalInitException;
import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.style.Style;

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
    private volatile Runnable resizeListener;
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
                    Runnable l = resizeListener;
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
        this.resizeListener = onResize;
    }
}
