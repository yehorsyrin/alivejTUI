package io.github.yehorsyrin.tui.platform.raw;

import io.github.yehorsyrin.tui.style.Color;
import io.github.yehorsyrin.tui.style.Style;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes ANSI/VT escape sequences to an {@link OutputStream}.
 *
 * <p>All write operations are buffered internally and flushed only on {@link #flush()}.
 * This avoids excessive system calls during a render frame.
 *
 * <p>Colours are rendered using the appropriate ANSI encoding depending on
 * {@link Color.ColorType}: 16-colour SGR codes for ANSI_16, {@code 38;5;n} for
 * ANSI_256, and {@code 38;2;r;g;b} for RGB true-colour.
 * A full SGR reset is emitted before each styled cell to avoid bleed.
 *
 * @author Jarvis (AI)
 */
public final class AnsiWriter {

    // ── Escape sequence constants ────────────────────────────────────────────

    /** Switch to the alternate screen buffer (preserves the user's scrollback). */
    public static final String ALTERNATE_SCREEN_ON  = "\033[?1049h";
    /** Return to the normal screen buffer. */
    public static final String ALTERNATE_SCREEN_OFF = "\033[?1049l";

    /** Hide the cursor. */
    public static final String CURSOR_HIDE = "\033[?25l";
    /** Show the cursor. */
    public static final String CURSOR_SHOW = "\033[?25h";

    /** Erase the entire display and move cursor to home (1,1). */
    public static final String CLEAR_SCREEN = "\033[2J\033[1;1H";

    /** SGR reset — clears all attributes, colours, etc. */
    public static final String SGR_RESET = "\033[0m";

    // ── SGR attribute codes ──────────────────────────────────────────────────

    private static final int SGR_BOLD      = 1;
    private static final int SGR_DIM       = 2;
    private static final int SGR_ITALIC    = 3;
    private static final int SGR_UNDERLINE = 4;
    private static final int SGR_STRIKE    = 9;

    // ── ANSI 16-colour offsets ───────────────────────────────────────────────

    /** Foreground: standard colours 0-7 → 30-37, bright 8-15 → 90-97. */
    private static final int FG_STANDARD_OFFSET = 30;
    private static final int FG_BRIGHT_OFFSET   = 90;
    /** Background: standard colours 0-7 → 40-47, bright 8-15 → 100-107. */
    private static final int BG_STANDARD_OFFSET = 40;
    private static final int BG_BRIGHT_OFFSET   = 100;

    // ── Instance ─────────────────────────────────────────────────────────────

    private final OutputStream out;
    /** Internal write buffer — built up per frame, flushed on flush(). */
    private final StringBuilder buf = new StringBuilder(4096);

    public AnsiWriter(OutputStream out) {
        this.out = out;
    }

    // ── Screen / cursor helpers ──────────────────────────────────────────────

    /** Switches to the alternate screen buffer. */
    public void alternateScreenOn()  { buf.append(ALTERNATE_SCREEN_ON); }

    /** Returns to the normal screen buffer. */
    public void alternateScreenOff() { buf.append(ALTERNATE_SCREEN_OFF); }

    /** Hides the text cursor. */
    public void hideCursor() { buf.append(CURSOR_HIDE); }

    /** Shows the text cursor. */
    public void showCursor() { buf.append(CURSOR_SHOW); }

    /**
     * Moves the cursor to the given 0-based column and row.
     * Converts to 1-based ANSI coordinates internally.
     */
    public void moveCursor(int col, int row) {
        buf.append("\033[").append(row + 1).append(';').append(col + 1).append('H');
    }

    /** Clears the screen and resets cursor to home. */
    public void clearScreen() { buf.append(CLEAR_SCREEN); }

    // ── Character rendering ──────────────────────────────────────────────────

    /**
     * Writes a single character at the given position using the supplied style.
     * Always resets SGR attributes before applying the new style to prevent bleed.
     *
     * @param col   0-based column
     * @param row   0-based row
     * @param c     character to draw
     * @param style visual style (may be {@code null} for plain text)
     */
    public void putChar(int col, int row, char c, Style style) {
        moveCursor(col, row);
        buf.append(SGR_RESET);
        if (style != null) {
            appendStyle(style);
        }
        buf.append(c);
        buf.append(SGR_RESET);
    }

    // ── Flush ────────────────────────────────────────────────────────────────

    /**
     * Flushes the internal buffer to the underlying {@link OutputStream}.
     *
     * @throws UncheckedIOException if writing fails
     */
    public void flush() {
        if (buf.isEmpty()) return;
        try {
            out.write(buf.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        buf.setLength(0);
    }

    // ── Raw write ────────────────────────────────────────────────────────────

    /**
     * Writes a raw escape sequence string directly into the buffer.
     * Useful for sequences not covered by the named helpers.
     */
    public void writeRaw(String sequence) {
        buf.append(sequence);
    }

    // ── Package-private helpers for testing ──────────────────────────────────

    /** Returns the current buffer length — confirms that output was produced. */
    int bufferLength() {
        return buf.length();
    }

    /** Returns the current buffer contents without flushing. */
    String bufferContents() {
        return buf.toString();
    }

    // ── Style rendering ──────────────────────────────────────────────────────

    private void appendStyle(Style style) {
        boolean hasSgr = style.isBold() || style.isDim() || style.isItalic()
                || style.isUnderline() || style.isStrikethrough();
        boolean hasFg  = style.getForeground() != null;
        boolean hasBg  = style.getBackground() != null;

        if (!hasSgr && !hasFg && !hasBg) return;

        buf.append("\033[");
        boolean first = true;

        if (style.isBold())          { buf.append(SGR_BOLD);      first = false; }
        if (style.isDim())           { if (!first) buf.append(';'); buf.append(SGR_DIM);       first = false; }
        if (style.isItalic())        { if (!first) buf.append(';'); buf.append(SGR_ITALIC);    first = false; }
        if (style.isUnderline())     { if (!first) buf.append(';'); buf.append(SGR_UNDERLINE); first = false; }
        if (style.isStrikethrough()) { if (!first) buf.append(';'); buf.append(SGR_STRIKE);    first = false; }

        if (hasFg) {
            if (!first) buf.append(';');
            appendColor(style.getForeground(), false);
            first = false;
        }
        if (hasBg) {
            if (!first) buf.append(';');
            appendColor(style.getBackground(), true);
        }

        buf.append('m');
    }

    /**
     * Appends the colour SGR parameter for the given colour.
     *
     * @param color    the colour to encode
     * @param isBackground {@code true} for background, {@code false} for foreground
     */
    private void appendColor(Color color, boolean isBackground) {
        switch (color.getType()) {
            case ANSI_16 -> {
                int idx = color.getAnsiIndex();
                if (isBackground) {
                    buf.append(idx < 8 ? BG_STANDARD_OFFSET + idx : BG_BRIGHT_OFFSET + (idx - 8));
                } else {
                    buf.append(idx < 8 ? FG_STANDARD_OFFSET + idx : FG_BRIGHT_OFFSET + (idx - 8));
                }
            }
            case ANSI_256 -> buf.append(isBackground ? 48 : 38)
                               .append(";5;")
                               .append(color.getAnsiIndex());
            case RGB      -> buf.append(isBackground ? 48 : 38)
                               .append(";2;")
                               .append(color.getR()).append(';')
                               .append(color.getG()).append(';')
                               .append(color.getB());
        }
    }
}
