package io.alive.tui.backend;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.MouseAction;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorColorConfiguration;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorDeviceConfiguration;

import java.awt.Font;

import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.event.MouseEvent;
import io.alive.tui.event.MouseType;
import io.alive.tui.style.Color;
import io.alive.tui.style.Style;

import java.io.IOException;
import java.util.EnumSet;

/**
 * Lanterna-based terminal backend implementation.
 *
 * <p>Known limitations of Lanterna 3.1.2:
 * <ul>
 *   <li>{@code SGR.FAINT} is not available — dim style is silently ignored.</li>
 *   <li>{@code showCursor()} cannot set cursor visibility directly; it is a no-op.
 *       Cursor visibility is controlled by {@link #setCursor(int, int)} (visible)
 *       and {@link #hideCursor()} (hidden via null position).</li>
 * </ul>
 *
 * @author Jarvis (AI)
 */
public class LanternaBackend implements TerminalBackend {

    private Screen screen;
    private Runnable resizeListener;

    @Override
    public void init() {
        try {
            SwingTerminalFontConfiguration fontConfig = SwingTerminalFontConfiguration.newInstance(
                    new Font("Consolas", Font.PLAIN, 16),
                    new Font("Courier New", Font.PLAIN, 16),
                    new Font("Monospaced", Font.PLAIN, 16)
            );
            SwingTerminalFrame terminal = new SwingTerminalFrame("AliveJTUI",
                    new TerminalSize(120, 35),
                    TerminalEmulatorDeviceConfiguration.getDefault(),
                    fontConfig,
                    TerminalEmulatorColorConfiguration.getDefault(),
                    TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode);
            terminal.setVisible(true);
            screen = new TerminalScreen(terminal);
            screen.startScreen();
            screen.setCursorPosition(null);
        } catch (IOException e) {
            throw new TerminalInitException("Failed to initialize Lanterna terminal", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (screen != null) {
                screen.stopScreen();
            }
        } catch (IOException e) {
            throw new TerminalInitException("Failed to shutdown Lanterna terminal", e);
        }
    }

    @Override
    public int getWidth() {
        requireInitialized();
        return screen.getTerminalSize().getColumns();
    }

    @Override
    public int getHeight() {
        requireInitialized();
        return screen.getTerminalSize().getRows();
    }

    @Override
    public void putChar(int col, int row, char c, Style style) {
        requireInitialized();
        TextColor fg = toTextColor(style.getForeground(), TextColor.ANSI.DEFAULT);
        TextColor bg = toTextColor(style.getBackground(), TextColor.ANSI.DEFAULT);
        EnumSet<SGR> sgrs = toSGR(style);
        TextCharacter tc = new TextCharacter(c, fg, bg, sgrs.toArray(new SGR[0]));
        screen.setCharacter(col, row, tc);
    }

    @Override
    public void flush() {
        requireInitialized();
        try {
            TerminalSize newSize = screen.doResizeIfNecessary();
            if (newSize != null && resizeListener != null) {
                resizeListener.run();
            }
            screen.refresh();
        } catch (IOException e) {
            throw new TerminalRenderException("Failed to flush screen", e);
        }
    }

    @Override
    public void hideCursor() {
        requireInitialized();
        screen.setCursorPosition(null);
    }

    @Override
    public void showCursor() {
        // No-op in Lanterna 3.1.2: cursor visibility is controlled via setCursor/hideCursor.
        // See class-level JavaDoc for details.
    }

    @Override
    public void setCursor(int col, int row) {
        requireInitialized();
        screen.setCursorPosition(new TerminalPosition(col, row));
    }

    @Override
    public void clear() {
        requireInitialized();
        screen.clear();
    }

    @Override
    public KeyEvent readKey() throws InterruptedException {
        requireInitialized();
        try {
            KeyStroke ks = screen.readInput();
            if (ks == null) return KeyEvent.of(KeyType.EOF);
            return toKeyEvent(ks);
        } catch (IOException e) {
            throw new TerminalRenderException("Terminal I/O error while reading input", e);
        }
    }

    @Override
    public KeyEvent readKey(long timeoutMs) throws InterruptedException {
        requireInitialized();
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        try {
            while (true) {
                KeyStroke ks = screen.pollInput();
                if (ks != null) return toKeyEvent(ks);
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) return null;
                Thread.sleep(Math.min(10L, remaining / 1_000_000L + 1));
            }
        } catch (IOException e) {
            throw new TerminalRenderException("Terminal I/O error while reading input", e);
        }
    }

    @Override
    public void setResizeListener(Runnable onResize) {
        this.resizeListener = onResize;
    }

    // --- Internal helpers ---

    private void requireInitialized() {
        if (screen == null) {
            throw new IllegalStateException("LanternaBackend.init() must be called before use");
        }
    }

    TextColor toTextColor(Color color, TextColor fallback) {
        if (color == null) return fallback;
        return switch (color.getType()) {
            case ANSI_16  -> indexToAnsi(color.getAnsiIndex());
            case ANSI_256 -> new TextColor.Indexed(color.getAnsiIndex());
            case RGB      -> new TextColor.RGB(color.getR(), color.getG(), color.getB());
        };
    }

    TextColor.ANSI indexToAnsi(int index) {
        return switch (index) {
            case 0  -> TextColor.ANSI.BLACK;
            case 1  -> TextColor.ANSI.RED;
            case 2  -> TextColor.ANSI.GREEN;
            case 3  -> TextColor.ANSI.YELLOW;
            case 4  -> TextColor.ANSI.BLUE;
            case 5  -> TextColor.ANSI.MAGENTA;
            case 6  -> TextColor.ANSI.CYAN;
            case 7  -> TextColor.ANSI.WHITE;
            case 8  -> TextColor.ANSI.BLACK_BRIGHT;
            case 9  -> TextColor.ANSI.RED_BRIGHT;
            case 10 -> TextColor.ANSI.GREEN_BRIGHT;
            case 11 -> TextColor.ANSI.YELLOW_BRIGHT;
            case 12 -> TextColor.ANSI.BLUE_BRIGHT;
            case 13 -> TextColor.ANSI.MAGENTA_BRIGHT;
            case 14 -> TextColor.ANSI.CYAN_BRIGHT;
            case 15 -> TextColor.ANSI.WHITE_BRIGHT;
            default -> TextColor.ANSI.DEFAULT;
        };
    }

    EnumSet<SGR> toSGR(Style style) {
        EnumSet<SGR> sgrs = EnumSet.noneOf(SGR.class);
        if (style.isBold())          sgrs.add(SGR.BOLD);
        if (style.isItalic())        sgrs.add(SGR.ITALIC);
        if (style.isUnderline())     sgrs.add(SGR.UNDERLINE);
        // SGR.FAINT (dim) not available in Lanterna 3.1.2 — silently ignored
        if (style.isStrikethrough()) sgrs.add(SGR.CROSSED_OUT);
        return sgrs;
    }

    KeyEvent toKeyEvent(KeyStroke ks) {
        boolean ctrl  = ks.isCtrlDown();
        boolean alt   = ks.isAltDown();
        boolean shift = ks.isShiftDown();
        return switch (ks.getKeyType()) {
            case Character  -> KeyEvent.ofCharacter(ks.getCharacter(), ctrl, alt, shift);
            case Enter      -> KeyEvent.of(KeyType.ENTER,       ctrl, alt, shift);
            case Backspace  -> KeyEvent.of(KeyType.BACKSPACE,   ctrl, alt, shift);
            case Delete     -> KeyEvent.of(KeyType.DELETE,      ctrl, alt, shift);
            case ArrowUp    -> KeyEvent.of(KeyType.ARROW_UP,    ctrl, alt, shift);
            case ArrowDown  -> KeyEvent.of(KeyType.ARROW_DOWN,  ctrl, alt, shift);
            case ArrowLeft  -> KeyEvent.of(KeyType.ARROW_LEFT,  ctrl, alt, shift);
            case ArrowRight -> KeyEvent.of(KeyType.ARROW_RIGHT, ctrl, alt, shift);
            case Escape     -> KeyEvent.of(KeyType.ESCAPE,      ctrl, alt, shift);
            case Tab        -> shift
                                 ? KeyEvent.of(KeyType.SHIFT_TAB, ctrl, alt, true)
                                 : KeyEvent.of(KeyType.TAB,       ctrl, alt, false);
            case Home       -> KeyEvent.of(KeyType.HOME,        ctrl, alt, shift);
            case End        -> KeyEvent.of(KeyType.END,         ctrl, alt, shift);
            case PageUp     -> KeyEvent.of(KeyType.PAGE_UP,     ctrl, alt, shift);
            case PageDown   -> KeyEvent.of(KeyType.PAGE_DOWN,   ctrl, alt, shift);
            case EOF        -> KeyEvent.of(KeyType.EOF);
            case MouseEvent -> toMouseKeyEvent(ks);
            default         -> KeyEvent.of(KeyType.EOF);
        };
    }

    private KeyEvent toMouseKeyEvent(KeyStroke ks) {
        if (!(ks instanceof MouseAction ma)) return KeyEvent.of(KeyType.EOF);
        int col    = ma.getPosition().getColumn();
        int row    = ma.getPosition().getRow();
        int button = ma.getButton();
        MouseType mType = switch (ma.getActionType()) {
            case CLICK_DOWN    -> MouseType.PRESS;
            case CLICK_RELEASE -> MouseType.RELEASE;
            case SCROLL_UP     -> MouseType.SCROLL_UP;
            case SCROLL_DOWN   -> MouseType.SCROLL_DOWN;
            default            -> MouseType.PRESS;  // DRAG, MOVE
        };
        return KeyEvent.ofMouse(new MouseEvent(mType, col, row, button));
    }
}
