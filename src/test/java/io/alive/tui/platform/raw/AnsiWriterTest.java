package io.alive.tui.platform.raw;

import io.alive.tui.style.Color;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jarvis (AI)
 */
class AnsiWriterTest {

    private ByteArrayOutputStream sink;
    private AnsiWriter writer;

    @BeforeEach
    void setUp() {
        sink = new ByteArrayOutputStream();
        writer = new AnsiWriter(sink);
    }

    // ── Buffering ────────────────────────────────────────────────────────────

    @Test
    void nothingWrittenToStreamBeforeFlush() {
        writer.hideCursor();
        assertEquals(0, sink.size(), "stream must be empty before flush()");
    }

    @Test
    void flushSendsBufferToStream() {
        writer.hideCursor();
        writer.flush();
        assertTrue(sink.size() > 0);
    }

    @Test
    void flushClearsInternalBuffer() {
        writer.hideCursor();
        writer.flush();
        int after = sink.size();
        writer.flush(); // second flush — nothing new buffered
        assertEquals(after, sink.size());
    }

    @Test
    void emptyFlushDoesNothing() {
        assertDoesNotThrow(() -> writer.flush());
        assertEquals(0, sink.size());
    }

    // ── Cursor / screen sequences ─────────────────────────────────────────────

    @Test
    void hideCursorEmitsCorrectSequence() {
        writer.hideCursor();
        assertEquals(AnsiWriter.CURSOR_HIDE, bufStr());
    }

    @Test
    void showCursorEmitsCorrectSequence() {
        writer.showCursor();
        assertEquals(AnsiWriter.CURSOR_SHOW, bufStr());
    }

    @Test
    void alternateScreenOnEmitsCorrectSequence() {
        writer.alternateScreenOn();
        assertEquals(AnsiWriter.ALTERNATE_SCREEN_ON, bufStr());
    }

    @Test
    void alternateScreenOffEmitsCorrectSequence() {
        writer.alternateScreenOff();
        assertEquals(AnsiWriter.ALTERNATE_SCREEN_OFF, bufStr());
    }

    @Test
    void clearScreenEmitsCorrectSequence() {
        writer.clearScreen();
        assertEquals(AnsiWriter.CLEAR_SCREEN, bufStr());
    }

    @Test
    void moveCursorConvertsToOneBased() {
        writer.moveCursor(0, 0);
        assertEquals("\033[1;1H", bufStr());
    }

    @Test
    void moveCursorNonZeroPosition() {
        writer.moveCursor(4, 9);
        assertEquals("\033[10;5H", bufStr()); // row+1=10, col+1=5
    }

    // ── putChar — plain ───────────────────────────────────────────────────────

    @Test
    void putCharPlainContainsMoveAndChar() {
        writer.putChar(2, 3, 'X', null);
        String out = bufStr();
        assertTrue(out.contains("\033[4;3H"), "must contain cursor move to row=4,col=3");
        assertTrue(out.contains("X"), "must contain the character");
    }

    @Test
    void putCharAlwaysEmitsSgrResetAround() {
        writer.putChar(0, 0, 'A', null);
        String out = bufStr();
        // reset before style, character, reset after
        assertTrue(out.contains(AnsiWriter.SGR_RESET));
    }

    // ── putChar — styled ──────────────────────────────────────────────────────

    @Test
    void putCharBoldContainsSgrBold() {
        Style bold = Style.DEFAULT.withBold(true);
        writer.putChar(0, 0, 'B', bold);
        assertTrue(bufStr().contains("\033[1m") || bufStr().contains("\033[1;"));
    }

    @Test
    void putCharItalicContainsSgrItalic() {
        Style italic = Style.DEFAULT.withItalic(true);
        writer.putChar(0, 0, 'I', italic);
        assertTrue(bufStr().contains("3m") || bufStr().contains("3;"));
    }

    @Test
    void putCharUnderlineContainsSgrUnderline() {
        Style underline = Style.DEFAULT.withUnderline(true);
        writer.putChar(0, 0, 'U', underline);
        assertTrue(bufStr().contains("4m") || bufStr().contains("4;"));
    }

    @Test
    void putCharDimContainsSgrDim() {
        Style dim = Style.DEFAULT.withDim(true);
        writer.putChar(0, 0, 'D', dim);
        assertTrue(bufStr().contains("2m") || bufStr().contains("2;"));
    }

    @Test
    void putCharStrikethroughContainsSgr() {
        Style strike = Style.DEFAULT.withStrikethrough(true);
        writer.putChar(0, 0, 'S', strike);
        assertTrue(bufStr().contains("9m") || bufStr().contains("9;"));
    }

    // ── Colours: ANSI_16 ─────────────────────────────────────────────────────

    @Test
    void fgAnsi16StandardColor() {
        Style s = Style.DEFAULT.withForeground(Color.RED); // index 1
        writer.putChar(0, 0, 'R', s);
        assertTrue(bufStr().contains("31m") || bufStr().contains("31;"),
                "standard red fg = 31");
    }

    @Test
    void fgAnsi16BrightColor() {
        Style s = Style.DEFAULT.withForeground(Color.BRIGHT_RED); // index 9
        writer.putChar(0, 0, 'R', s);
        assertTrue(bufStr().contains("91m") || bufStr().contains("91;"),
                "bright red fg = 91");
    }

    @Test
    void bgAnsi16StandardColor() {
        Style s = Style.DEFAULT.withBackground(Color.BLUE); // index 4
        writer.putChar(0, 0, 'B', s);
        assertTrue(bufStr().contains("44m") || bufStr().contains("44;"),
                "standard blue bg = 44");
    }

    @Test
    void bgAnsi16BrightColor() {
        Style s = Style.DEFAULT.withBackground(Color.BRIGHT_WHITE); // index 15
        writer.putChar(0, 0, 'W', s);
        assertTrue(bufStr().contains("107m") || bufStr().contains("107;"),
                "bright white bg = 107");
    }

    // ── Colours: ANSI_256 ────────────────────────────────────────────────────

    @Test
    void fgAnsi256Color() {
        Style s = Style.DEFAULT.withForeground(Color.ansi256(200));
        writer.putChar(0, 0, 'C', s);
        assertTrue(bufStr().contains("38;5;200"), "256-color fg");
    }

    @Test
    void bgAnsi256Color() {
        Style s = Style.DEFAULT.withBackground(Color.ansi256(100));
        writer.putChar(0, 0, 'C', s);
        assertTrue(bufStr().contains("48;5;100"), "256-color bg");
    }

    // ── Colours: RGB ─────────────────────────────────────────────────────────

    @Test
    void fgRgbColor() {
        Style s = Style.DEFAULT.withForeground(Color.rgb(10, 20, 30));
        writer.putChar(0, 0, 'X', s);
        assertTrue(bufStr().contains("38;2;10;20;30"), "truecolor fg");
    }

    @Test
    void bgRgbColor() {
        Style s = Style.DEFAULT.withBackground(Color.rgb(255, 128, 0));
        writer.putChar(0, 0, 'X', s);
        assertTrue(bufStr().contains("48;2;255;128;0"), "truecolor bg");
    }

    // ── writeRaw ─────────────────────────────────────────────────────────────

    @Test
    void writeRawAppendsToBuffer() {
        writer.writeRaw("\033[?2004h");
        assertEquals("\033[?2004h", bufStr());
    }

    // ── flush — stream content ────────────────────────────────────────────────

    @Test
    void flushedBytesAreUtf8() {
        writer.putChar(0, 0, '\u00e9', null); // é
        writer.flush();
        String flushed = sink.toString(StandardCharsets.UTF_8);
        assertTrue(flushed.contains("\u00e9"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String bufStr() {
        return writer.bufferContents();
    }
}
