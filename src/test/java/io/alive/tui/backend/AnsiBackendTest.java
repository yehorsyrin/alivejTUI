package io.alive.tui.backend;

import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.style.Color;
import io.alive.tui.style.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AnsiBackend} — tests the SGR builder and input decoder
 * without needing a real terminal.
 *
 * @author Jarvis (AI)
 */
class AnsiBackendTest {

    private ByteArrayOutputStream outBuf;
    private AnsiBackend backend;

    @BeforeEach
    void setUp() {
        outBuf  = new ByteArrayOutputStream();
        // Empty input stream — tests that need input push bytes via constructor
        backend = new AnsiBackend(new PrintStream(outBuf), new ByteArrayInputStream(new byte[0]));
    }

    // --- SGR builder ---

    @Test
    void buildSgr_defaultStyle_resetsOnly() {
        String sgr = backend.buildSgr(Style.DEFAULT);
        assertTrue(sgr.startsWith("\033["));
        assertTrue(sgr.endsWith("m"));
        assertTrue(sgr.contains("0"));   // reset code
    }

    @Test
    void buildSgr_null_resetsOnly() {
        String sgr = backend.buildSgr(null);
        assertEquals("\033[0m", sgr);
    }

    @Test
    void buildSgr_bold_containsBoldCode() {
        String sgr = backend.buildSgr(Style.DEFAULT.withBold(true));
        assertTrue(sgr.contains(";1"));
    }

    @Test
    void buildSgr_italic_containsItalicCode() {
        String sgr = backend.buildSgr(Style.DEFAULT.withItalic(true));
        assertTrue(sgr.contains(";3"));
    }

    @Test
    void buildSgr_underline_containsUnderlineCode() {
        String sgr = backend.buildSgr(Style.DEFAULT.withUnderline(true));
        assertTrue(sgr.contains(";4"));
    }

    @Test
    void buildSgr_strikethrough_containsCode9() {
        String sgr = backend.buildSgr(Style.DEFAULT.withStrikethrough(true));
        assertTrue(sgr.contains(";9"));
    }

    @Test
    void buildSgr_ansi16Foreground_containsFgCode() {
        Style s = Style.DEFAULT.withForeground(Color.RED);  // ANSI index 1
        String sgr = backend.buildSgr(s);
        assertTrue(sgr.contains("31"), "Expected 31 (red fg) in: " + sgr);
    }

    @Test
    void buildSgr_brightForeground_containsBrightFgCode() {
        Style s = Style.DEFAULT.withForeground(Color.BRIGHT_RED);  // ANSI index 9
        String sgr = backend.buildSgr(s);
        assertTrue(sgr.contains("91"), "Expected 91 (bright red fg) in: " + sgr);
    }

    @Test
    void buildSgr_256Foreground_containsExtendedCode() {
        Style s = Style.DEFAULT.withForeground(Color.ansi256(200));
        String sgr = backend.buildSgr(s);
        assertTrue(sgr.contains("38;5;200"), sgr);
    }

    @Test
    void buildSgr_rgbForeground_containsRgbCode() {
        Style s = Style.DEFAULT.withForeground(Color.rgb(255, 128, 0));
        String sgr = backend.buildSgr(s);
        assertTrue(sgr.contains("38;2;255;128;0"), sgr);
    }

    @Test
    void buildSgr_ansi16Background_containsBgCode() {
        Style s = Style.DEFAULT.withBackground(Color.BLUE);  // ANSI index 4
        String sgr = backend.buildSgr(s);
        assertTrue(sgr.contains("44"), "Expected 44 (blue bg) in: " + sgr);
    }

    // --- Input decoding (via readKey) ---

    private AnsiBackend backendWithInput(byte... bytes) {
        return new AnsiBackend(new PrintStream(new ByteArrayOutputStream()),
                               new ByteArrayInputStream(bytes));
    }

    @Test
    void readKey_printableChar() throws InterruptedException {
        AnsiBackend b = backendWithInput((byte) 'A');
        KeyEvent e = b.readKey();
        assertEquals(KeyType.CHARACTER, e.type());
        assertEquals('A', e.character());
    }

    @Test
    void readKey_enter() throws InterruptedException {
        AnsiBackend b = backendWithInput((byte) 13);
        assertEquals(KeyType.ENTER, b.readKey().type());
    }

    @Test
    void readKey_backspace() throws InterruptedException {
        AnsiBackend b = backendWithInput((byte) 127);
        assertEquals(KeyType.BACKSPACE, b.readKey().type());
    }

    @Test
    void readKey_tab() throws InterruptedException {
        AnsiBackend b = backendWithInput((byte) 9);
        assertEquals(KeyType.TAB, b.readKey().type());
    }

    @Test
    void readKey_escape_bare() throws InterruptedException {
        // Single ESC with nothing following → ESCAPE
        AnsiBackend b = backendWithInput((byte) 27);
        assertEquals(KeyType.ESCAPE, b.readKey().type());
    }

    @Test
    void readKey_eof_returnsEof() throws InterruptedException {
        AnsiBackend b = backendWithInput();  // empty input
        // -1 from stream → EOF
        assertEquals(KeyType.EOF, b.readKey().type());
    }

    @Test
    void readKey_arrowUp() throws InterruptedException {
        AnsiBackend b = backendWithInput((byte) 27, (byte) '[', (byte) 'A');
        assertEquals(KeyType.ARROW_UP, b.readKey().type());
    }

    @Test
    void readKey_arrowDown() throws InterruptedException {
        AnsiBackend b = backendWithInput((byte) 27, (byte) '[', (byte) 'B');
        assertEquals(KeyType.ARROW_DOWN, b.readKey().type());
    }

    @Test
    void readKey_arrowRight() throws InterruptedException {
        AnsiBackend b = backendWithInput((byte) 27, (byte) '[', (byte) 'C');
        assertEquals(KeyType.ARROW_RIGHT, b.readKey().type());
    }

    @Test
    void readKey_arrowLeft() throws InterruptedException {
        AnsiBackend b = backendWithInput((byte) 27, (byte) '[', (byte) 'D');
        assertEquals(KeyType.ARROW_LEFT, b.readKey().type());
    }

    // --- Lifecycle ---

    @Test
    void init_writesAlternateScreenSequence() {
        backend.init();
        String output = outBuf.toString();
        assertTrue(output.contains("\033[?1049h"), "Expected alternate screen in: " + output);
    }

    @Test
    void shutdown_restoresMainScreen() {
        backend.shutdown();
        String output = outBuf.toString();
        assertTrue(output.contains("\033[?1049l"), "Expected restore screen in: " + output);
    }

    @Test
    void getDimensions_defaultValues() {
        assertEquals(80, backend.getWidth());
        assertEquals(24, backend.getHeight());
    }

    @Test
    void constructor_nullOut_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new AnsiBackend(null, System.in));
    }

    @Test
    void constructor_nullIn_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new AnsiBackend(System.out, null));
    }
}
