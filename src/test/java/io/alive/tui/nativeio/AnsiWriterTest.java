package io.alive.tui.nativeio;

import io.alive.tui.nativeio.backend.AnsiWriter;
import io.alive.tui.style.Color;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AnsiWriter}.
 *
 * @author Jarvis (AI)
 */
class AnsiWriterTest {

    private ByteArrayOutputStream buf;
    private AnsiWriter writer;

    @BeforeEach
    void setUp() {
        buf = new ByteArrayOutputStream();
        writer = new AnsiWriter(buf);
    }

    private String output() {
        return buf.toString(StandardCharsets.UTF_8);
    }

    // --- Constructor ---

    @Test
    void constructor_nullOut_throws() {
        assertThrows(IllegalArgumentException.class, () -> new AnsiWriter(null));
    }

    // --- Screen lifecycle ---

    @Test
    void alternateScreenOn_writesCorrectSequence() throws IOException {
        writer.alternateScreenOn();
        assertEquals("\033[?1049h", output());
    }

    @Test
    void alternateScreenOff_writesCorrectSequence() throws IOException {
        writer.alternateScreenOff();
        assertEquals("\033[?1049l", output());
    }

    @Test
    void hideCursor_writesCorrectSequence() throws IOException {
        writer.hideCursor();
        assertEquals("\033[?25l", output());
    }

    @Test
    void showCursor_writesCorrectSequence() throws IOException {
        writer.showCursor();
        assertEquals("\033[?25h", output());
    }

    @Test
    void clearScreen_writesEraseAndHome() throws IOException {
        writer.clearScreen();
        assertEquals("\033[2J\033[1;1H", output());
    }

    @Test
    void resetStyle_writesResetSequence() throws IOException {
        writer.resetStyle();
        assertEquals("\033[0m", output());
    }

    // --- Cursor ---

    @Test
    void moveCursor_convertsTo1Based() throws IOException {
        writer.moveCursor(0, 0);
        assertEquals("\033[1;1H", output());
    }

    @Test
    void moveCursor_col3Row5() throws IOException {
        writer.moveCursor(3, 5);
        assertEquals("\033[6;4H", output());
    }

    // --- putChar ---

    @Test
    void putChar_defaultStyle_containsChar() throws IOException {
        writer.putChar(0, 0, 'X', Style.DEFAULT);
        String out = output();
        assertTrue(out.contains("X"), "Expected 'X' in output");
        assertTrue(out.contains("\033[0m"), "Expected reset after char");
    }

    @Test
    void putChar_boldStyle_containsBoldCode() throws IOException {
        writer.putChar(0, 0, 'A', Style.DEFAULT.withBold(true));
        assertTrue(output().contains(";1m") || output().contains(";1;"));
    }

    // --- SGR builder ---

    @Test
    void buildSgr_nullStyle_returnsReset() {
        assertEquals("\033[0m", writer.buildSgr(null));
    }

    @Test
    void buildSgr_defaultStyle_returnsReset() {
        assertEquals("\033[0m", writer.buildSgr(Style.DEFAULT));
    }

    @Test
    void buildSgr_boldItalic() {
        String sgr = writer.buildSgr(Style.DEFAULT.withBold(true).withItalic(true));
        assertTrue(sgr.contains(";1"), "bold code ;1");
        assertTrue(sgr.contains(";3"), "italic code ;3");
    }

    @Test
    void buildSgr_dim() {
        assertTrue(writer.buildSgr(Style.DEFAULT.withDim(true)).contains(";2"));
    }

    @Test
    void buildSgr_underline() {
        assertTrue(writer.buildSgr(Style.DEFAULT.withUnderline(true)).contains(";4"));
    }

    @Test
    void buildSgr_strikethrough() {
        assertTrue(writer.buildSgr(Style.DEFAULT.withStrikethrough(true)).contains(";9"));
    }

    @Test
    void buildSgr_ansi16Foreground() {
        String sgr = writer.buildSgr(Style.DEFAULT.withForeground(Color.RED));
        assertTrue(sgr.contains("31"), "ANSI-16 red fg = 31");
    }

    @Test
    void buildSgr_ansi16Background() {
        String sgr = writer.buildSgr(Style.DEFAULT.withBackground(Color.BLUE));
        assertTrue(sgr.contains("44"), "ANSI-16 blue bg = 44");
    }

    @Test
    void buildSgr_rgbColor() {
        String sgr = writer.buildSgr(Style.DEFAULT.withForeground(Color.rgb(100, 150, 200)));
        assertTrue(sgr.contains("38;2;100;150;200"));
    }

    @Test
    void buildSgr_ansi256Color() {
        String sgr = writer.buildSgr(Style.DEFAULT.withForeground(Color.ansi256(200)));
        assertTrue(sgr.contains("38;5;200"));
    }

    @Test
    void buildSgr_alwaysEndsWithM() {
        String sgr = writer.buildSgr(Style.DEFAULT.withBold(true));
        assertTrue(sgr.endsWith("m"));
    }
}
