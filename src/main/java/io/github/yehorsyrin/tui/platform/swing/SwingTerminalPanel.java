package io.github.yehorsyrin.tui.platform.swing;

import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.platform.input.AnsiKeyDecoder;
import io.github.yehorsyrin.tui.style.Color;
import io.github.yehorsyrin.tui.style.Style;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

/**
 * Swing {@link JPanel} that renders a character grid as a terminal emulator.
 *
 * <p>Each cell contains a character and a {@link Style}. The panel maintains
 * a back-buffer of cells and repaints only when {@link #repaint()} is called
 * (triggered by {@link io.github.yehorsyrin.tui.platform.swing.SwingBackend#flush()}).
 *
 * <p>Key events are translated to {@link KeyEvent} objects via
 * {@link AnsiKeyDecoder} and forwarded to a registered listener.
 *
 * @author Jarvis (AI)
 */
public final class SwingTerminalPanel extends JPanel {

    // ── Font / metrics ────────────────────────────────────────────────────────

    private static final String FONT_NAME  = "Monospaced";
    private static final int    FONT_SIZE  = 14;
    private static final Font   CELL_FONT  = new Font(FONT_NAME, Font.PLAIN, FONT_SIZE);

    /** Default terminal dimensions in characters. */
    public static final int DEFAULT_COLS = 80;
    public static final int DEFAULT_ROWS = 24;

    // ── Cell dimensions (computed from font metrics) ───────────────────────────

    private final int cellWidth;
    private final int cellHeight;

    // ── Grid state ────────────────────────────────────────────────────────────

    private int cols;
    private int rows;

    /** Characters — indexed [row * cols + col]. */
    private char[]  chars;
    /** Styles — indexed [row * cols + col]. */
    private Style[] styles;

    // ── Cursor ────────────────────────────────────────────────────────────────

    private int     cursorCol     = 0;
    private int     cursorRow     = 0;
    private boolean cursorVisible = false;

    // ── Default terminal colours (updated via setDefaultColors) ──────────────

    private java.awt.Color defaultForeground = java.awt.Color.WHITE;
    private java.awt.Color defaultBackground = java.awt.Color.BLACK;

    // ── Key listener ──────────────────────────────────────────────────────────

    private Consumer<KeyEvent> keyListener;

    // ── Constructor ───────────────────────────────────────────────────────────

    public SwingTerminalPanel() {
        this(DEFAULT_COLS, DEFAULT_ROWS);
    }

    public SwingTerminalPanel(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;

        // Compute monospace cell dimensions
        Canvas canvas = new Canvas();
        FontMetrics fm = canvas.getFontMetrics(CELL_FONT);
        cellWidth  = fm.charWidth('M');
        cellHeight = fm.getHeight();

        chars  = new char [cols * rows];
        styles = new Style[cols * rows];
        fill(' ', null);

        setBackground(java.awt.Color.BLACK);
        setFont(CELL_FONT);
        setFocusable(true);
        // Prevent Swing from consuming Tab/Shift+Tab for its own focus traversal;
        // our keyPressed handler translates them to TUI KeyEvents instead.
        setFocusTraversalKeysEnabled(false);
        setPreferredSize(new Dimension(cols * cellWidth, rows * cellHeight));

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                KeyEvent ke = translateKeyEvent(e);
                if (ke != null && keyListener != null) keyListener.accept(ke);
            }

            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                if (c != java.awt.event.KeyEvent.CHAR_UNDEFINED
                        && c >= 0x20 && c != 0x7F) {
                    KeyEvent ke = KeyEvent.ofCharacter(c);
                    if (keyListener != null) keyListener.accept(ke);
                }
            }
        });
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Writes a character at the given grid position.
     * Thread-safe: updates are synchronised on {@code this}.
     */
    public synchronized void putChar(int col, int row, char c, Style style) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) return;
        int idx = row * cols + col;
        chars[idx]  = c;
        styles[idx] = style;
    }

    /** Clears the grid (fills with spaces). */
    public synchronized void clear() {
        fill(' ', null);
    }

    /** Resizes the grid. Existing content is discarded. */
    public synchronized void resize(int newCols, int newRows) {
        this.cols   = newCols;
        this.rows   = newRows;
        this.chars  = new char [newCols * newRows];
        this.styles = new Style[newCols * newRows];
        fill(' ', null);
        setPreferredSize(new Dimension(newCols * cellWidth, newRows * cellHeight));
    }

    /** Shows or hides the cursor block. */
    public void setCursorVisible(boolean visible) {
        this.cursorVisible = visible;
    }

    /** Moves the cursor to the given grid position. */
    public void setCursorPosition(int col, int row) {
        this.cursorCol = col;
        this.cursorRow = row;
    }

    /** Registers a listener that receives decoded {@link KeyEvent}s. */
    public void setKeyListener(Consumer<KeyEvent> listener) {
        this.keyListener = listener;
    }

    /**
     * Updates the default foreground and background colours used for cells that
     * carry no explicit colour in their {@link io.github.yehorsyrin.tui.style.Style}.
     * Pass {@code null} for either argument to keep the current value.
     */
    public synchronized void setDefaultColors(java.awt.Color fg, java.awt.Color bg) {
        if (fg != null) defaultForeground = fg;
        if (bg != null) {
            defaultBackground = bg;
            setBackground(bg);
        }
        repaint();
    }

    /** Returns the cell width in pixels. */
    public int getCellWidth()  { return cellWidth; }

    /** Returns the cell height in pixels. */
    public int getCellHeight() { return cellHeight; }

    // ── Painting ──────────────────────────────────────────────────────────────

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(CELL_FONT);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        FontMetrics fm = g2.getFontMetrics();
        int ascent = fm.getAscent();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int idx = row * cols + col;
                char  c = chars[idx];
                Style s = styles[idx];

                int x = col * cellWidth;
                int y = row * cellHeight;

                // Background
                java.awt.Color bg = backgroundOf(s);
                g2.setColor(bg);
                g2.fillRect(x, y, cellWidth, cellHeight);

                // Cursor block (drawn over background, under character)
                if (cursorVisible && col == cursorCol && row == cursorRow) {
                    g2.setColor(java.awt.Color.WHITE);
                    g2.fillRect(x, y, cellWidth, cellHeight);
                }

                // Character
                if (c != ' ' && c != '\0') {
                    g2.setColor(foregroundOf(s));
                    applyTextStyle(g2, s);
                    g2.drawString(String.valueOf(c), x, y + ascent);
                    g2.setFont(CELL_FONT); // reset after bold/italic variant
                }
            }
        }
    }

    // ── Key translation ───────────────────────────────────────────────────────

    private static KeyEvent translateKeyEvent(java.awt.event.KeyEvent e) {
        boolean ctrl  = e.isControlDown();
        boolean alt   = e.isAltDown();
        boolean shift = e.isShiftDown();

        return switch (e.getKeyCode()) {
            case java.awt.event.KeyEvent.VK_ENTER     -> KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.ENTER,     ctrl, alt, shift);
            case java.awt.event.KeyEvent.VK_BACK_SPACE-> KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.BACKSPACE, ctrl, alt, shift);
            case java.awt.event.KeyEvent.VK_DELETE    -> KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.DELETE,    ctrl, alt, shift);
            case java.awt.event.KeyEvent.VK_ESCAPE    -> KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.ESCAPE);
            case java.awt.event.KeyEvent.VK_TAB       -> shift
                    ? KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.SHIFT_TAB)
                    : KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.TAB);
            case java.awt.event.KeyEvent.VK_UP        -> KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.ARROW_UP,    ctrl, alt, shift);
            case java.awt.event.KeyEvent.VK_DOWN      -> KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.ARROW_DOWN,  ctrl, alt, shift);
            case java.awt.event.KeyEvent.VK_LEFT      -> KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.ARROW_LEFT,  ctrl, alt, shift);
            case java.awt.event.KeyEvent.VK_RIGHT     -> KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.ARROW_RIGHT, ctrl, alt, shift);
            case java.awt.event.KeyEvent.VK_HOME      -> KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.HOME,       ctrl, alt, shift);
            case java.awt.event.KeyEvent.VK_END       -> KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.END,        ctrl, alt, shift);
            case java.awt.event.KeyEvent.VK_PAGE_UP   -> KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.PAGE_UP,    ctrl, alt, shift);
            case java.awt.event.KeyEvent.VK_PAGE_DOWN -> KeyEvent.of(io.github.yehorsyrin.tui.event.KeyType.PAGE_DOWN,  ctrl, alt, shift);
            default -> null; // handled by keyTyped for printable chars
        };
    }

    // ── Style → AWT color helpers ─────────────────────────────────────────────

    private java.awt.Color foregroundOf(Style s) {
        if (s == null || s.getForeground() == null) return defaultForeground;
        return toAwtColor(s.getForeground());
    }

    private java.awt.Color backgroundOf(Style s) {
        if (s == null || s.getBackground() == null) return defaultBackground;
        return toAwtColor(s.getBackground());
    }

    private static java.awt.Color toAwtColor(Color c) {
        return switch (c.getType()) {
            case ANSI_16  -> ansi16ToAwt(c.getAnsiIndex());
            case ANSI_256 -> ansi256ToAwt(c.getAnsiIndex());
            case RGB      -> new java.awt.Color(c.getR(), c.getG(), c.getB());
        };
    }

    private static void applyTextStyle(Graphics2D g2, Style s) {
        if (s == null) return;
        int style = Font.PLAIN;
        if (s.isBold())   style |= Font.BOLD;
        if (s.isItalic()) style |= Font.ITALIC;
        if (style != Font.PLAIN) g2.setFont(CELL_FONT.deriveFont(style));
    }

    /** Standard 16-colour ANSI palette (xterm default). */
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

    /**
     * Converts an ANSI 256-colour index to an AWT colour.
     * Indices 0-15: standard 16 colours; 16-231: 6×6×6 colour cube;
     * 232-255: grayscale ramp.
     */
    private static java.awt.Color ansi256ToAwt(int idx) {
        if (idx < 16)  return ansi16ToAwt(idx);
        if (idx < 232) {
            int n = idx - 16;
            int b = n % 6;
            int g = (n / 6) % 6;
            int r = n / 36;
            return new java.awt.Color(r == 0 ? 0 : 55 + r * 40,
                                       g == 0 ? 0 : 55 + g * 40,
                                       b == 0 ? 0 : 55 + b * 40);
        }
        // Grayscale 232-255
        int v = 8 + (idx - 232) * 10;
        return new java.awt.Color(v, v, v);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void fill(char c, Style s) {
        java.util.Arrays.fill(chars,  c);
        java.util.Arrays.fill(styles, s);
    }
}
