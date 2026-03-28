package io.alive.tui.backend;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.style.Color;
import io.alive.tui.style.Style;

import com.googlecode.lanterna.input.KeyStroke;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LanternaBackend's internal conversion helpers.
 * These methods are package-private to allow direct testing without a real terminal.
 */
class LanternaBackendConversionTest {

    private LanternaBackend backend;

    @BeforeEach
    void setUp() {
        backend = new LanternaBackend();
    }

    // --- toTextColor ---

    @Test
    void toTextColor_nullColorReturnsFallback() {
        TextColor result = backend.toTextColor(null, TextColor.ANSI.RED);
        assertEquals(TextColor.ANSI.RED, result);
    }

    @Test
    void toTextColor_rgbColor() {
        Color c = Color.rgb(10, 20, 30);
        TextColor result = backend.toTextColor(c, TextColor.ANSI.DEFAULT);
        assertInstanceOf(TextColor.RGB.class, result);
        TextColor.RGB rgb = (TextColor.RGB) result;
        assertEquals(10, rgb.getRed());
        assertEquals(20, rgb.getGreen());
        assertEquals(30, rgb.getBlue());
    }

    @Test
    void toTextColor_ansi256Color() {
        Color c = Color.ansi256(100);
        TextColor result = backend.toTextColor(c, TextColor.ANSI.DEFAULT);
        assertInstanceOf(TextColor.Indexed.class, result);
    }

    @Test
    void toTextColor_ansi16Color() {
        Color c = Color.GREEN;
        TextColor result = backend.toTextColor(c, TextColor.ANSI.DEFAULT);
        assertEquals(TextColor.ANSI.GREEN, result);
    }

    // --- indexToAnsi ---

    @ParameterizedTest
    @CsvSource({
        "0,  BLACK",
        "1,  RED",
        "2,  GREEN",
        "3,  YELLOW",
        "4,  BLUE",
        "5,  MAGENTA",
        "6,  CYAN",
        "7,  WHITE",
        "8,  BLACK_BRIGHT",
        "9,  RED_BRIGHT",
        "10, GREEN_BRIGHT",
        "11, YELLOW_BRIGHT",
        "12, BLUE_BRIGHT",
        "13, MAGENTA_BRIGHT",
        "14, CYAN_BRIGHT",
        "15, WHITE_BRIGHT"
    })
    void indexToAnsi_allSixteenColors(int index, String expectedName) {
        TextColor.ANSI result = backend.indexToAnsi(index);
        assertEquals(expectedName, result.name());
    }

    @Test
    void indexToAnsi_unknownIndexReturnsDefault() {
        assertEquals(TextColor.ANSI.DEFAULT, backend.indexToAnsi(99));
        assertEquals(TextColor.ANSI.DEFAULT, backend.indexToAnsi(-1));
    }

    // --- toSGR ---

    @Test
    void toSGR_noDecorations_emptySet() {
        EnumSet<SGR> sgrs = backend.toSGR(Style.DEFAULT);
        assertTrue(sgrs.isEmpty());
    }

    @Test
    void toSGR_bold() {
        Style s = Style.DEFAULT.withBold(true);
        assertTrue(backend.toSGR(s).contains(SGR.BOLD));
    }

    @Test
    void toSGR_italic() {
        Style s = Style.DEFAULT.withItalic(true);
        assertTrue(backend.toSGR(s).contains(SGR.ITALIC));
    }

    @Test
    void toSGR_underline() {
        Style s = Style.DEFAULT.withUnderline(true);
        assertTrue(backend.toSGR(s).contains(SGR.UNDERLINE));
    }

    @Test
    void toSGR_strikethrough() {
        Style s = Style.DEFAULT.withStrikethrough(true);
        assertTrue(backend.toSGR(s).contains(SGR.CROSSED_OUT));
    }

    @Test
    void toSGR_dim_silentlyIgnored() {
        // Lanterna 3.1.2 has no SGR.FAINT; dim should not produce any SGR
        Style s = Style.DEFAULT.withDim(true);
        EnumSet<SGR> sgrs = backend.toSGR(s);
        assertTrue(sgrs.isEmpty());
    }

    @Test
    void toSGR_multipleDecorations() {
        Style s = Style.DEFAULT.withBold(true).withUnderline(true).withItalic(true);
        EnumSet<SGR> sgrs = backend.toSGR(s);
        assertTrue(sgrs.contains(SGR.BOLD));
        assertTrue(sgrs.contains(SGR.UNDERLINE));
        assertTrue(sgrs.contains(SGR.ITALIC));
        assertEquals(3, sgrs.size());
    }

    // --- toKeyEvent ---

    @Test
    void toKeyEvent_characterKey() {
        KeyStroke ks = new KeyStroke('a', false, false);
        KeyEvent result = backend.toKeyEvent(ks);
        assertEquals(KeyType.CHARACTER, result.type());
        assertEquals('a', result.character());
    }

    @Test
    void toKeyEvent_enterKey() {
        KeyStroke ks = new KeyStroke(com.googlecode.lanterna.input.KeyType.Enter, false, false);
        assertEquals(KeyType.ENTER, backend.toKeyEvent(ks).type());
    }

    @Test
    void toKeyEvent_backspace() {
        KeyStroke ks = new KeyStroke(com.googlecode.lanterna.input.KeyType.Backspace, false, false);
        assertEquals(KeyType.BACKSPACE, backend.toKeyEvent(ks).type());
    }

    @Test
    void toKeyEvent_arrowKeys() {
        assertEquals(KeyType.ARROW_UP,    backend.toKeyEvent(new KeyStroke(com.googlecode.lanterna.input.KeyType.ArrowUp, false, false)).type());
        assertEquals(KeyType.ARROW_DOWN,  backend.toKeyEvent(new KeyStroke(com.googlecode.lanterna.input.KeyType.ArrowDown, false, false)).type());
        assertEquals(KeyType.ARROW_LEFT,  backend.toKeyEvent(new KeyStroke(com.googlecode.lanterna.input.KeyType.ArrowLeft, false, false)).type());
        assertEquals(KeyType.ARROW_RIGHT, backend.toKeyEvent(new KeyStroke(com.googlecode.lanterna.input.KeyType.ArrowRight, false, false)).type());
    }

    @Test
    void toKeyEvent_escape() {
        KeyStroke ks = new KeyStroke(com.googlecode.lanterna.input.KeyType.Escape, false, false);
        assertEquals(KeyType.ESCAPE, backend.toKeyEvent(ks).type());
    }

    @Test
    void toKeyEvent_tab() {
        KeyStroke ks = new KeyStroke(com.googlecode.lanterna.input.KeyType.Tab, false, false);
        assertEquals(KeyType.TAB, backend.toKeyEvent(ks).type());
    }

    @Test
    void toKeyEvent_shiftTab() {
        // 4-arg constructor: (KeyType, ctrlDown, altDown, shiftDown)
        KeyStroke ks = new KeyStroke(com.googlecode.lanterna.input.KeyType.Tab, false, false, true);
        assertEquals(KeyType.SHIFT_TAB, backend.toKeyEvent(ks).type());
    }

    @Test
    void toKeyEvent_eof() {
        KeyStroke ks = new KeyStroke(com.googlecode.lanterna.input.KeyType.EOF, false, false);
        assertEquals(KeyType.EOF, backend.toKeyEvent(ks).type());
    }

    // --- toKeyEvent modifier tests ---

    @Test
    void toKeyEvent_ctrlModifier() {
        KeyStroke ks = new KeyStroke('a', true, false);
        KeyEvent result = backend.toKeyEvent(ks);
        assertTrue(result.ctrl());
        assertFalse(result.alt());
        assertFalse(result.shift());
    }

    @Test
    void toKeyEvent_altModifier() {
        KeyStroke ks = new KeyStroke(com.googlecode.lanterna.input.KeyType.Enter, false, true);
        KeyEvent result = backend.toKeyEvent(ks);
        assertFalse(result.ctrl());
        assertTrue(result.alt());
        assertFalse(result.shift());
    }

    @Test
    void toKeyEvent_shiftModifier() {
        KeyStroke ks = new KeyStroke(com.googlecode.lanterna.input.KeyType.ArrowUp, false, false, true);
        KeyEvent result = backend.toKeyEvent(ks);
        assertFalse(result.ctrl());
        assertFalse(result.alt());
        assertTrue(result.shift());
    }

    @Test
    void toKeyEvent_noModifiers() {
        KeyStroke ks = new KeyStroke(com.googlecode.lanterna.input.KeyType.Escape, false, false);
        KeyEvent result = backend.toKeyEvent(ks);
        assertFalse(result.ctrl());
        assertFalse(result.alt());
        assertFalse(result.shift());
    }

    // --- requireInitialized (via public methods) ---

    @Test
    void getWidth_beforeInit_throwsIllegalState() {
        assertThrows(IllegalStateException.class, backend::getWidth);
    }

    @Test
    void getHeight_beforeInit_throwsIllegalState() {
        assertThrows(IllegalStateException.class, backend::getHeight);
    }

    @Test
    void putChar_beforeInit_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
            () -> backend.putChar(0, 0, 'x', Style.DEFAULT));
    }

    @Test
    void flush_beforeInit_throwsIllegalState() {
        assertThrows(IllegalStateException.class, backend::flush);
    }

    @Test
    void readKey_beforeInit_throwsIllegalState() {
        assertThrows(IllegalStateException.class, backend::readKey);
    }
}
