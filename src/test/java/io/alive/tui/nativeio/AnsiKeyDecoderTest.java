package io.alive.tui.nativeio;

import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;
import io.alive.tui.nativeio.input.AnsiKeyDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AnsiKeyDecoder} — ~50 sequence vectors.
 *
 * @author Jarvis (AI)
 */
class AnsiKeyDecoderTest {

    private AnsiKeyDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = AnsiKeyDecoder.create();
    }

    // Helper: build byte array from ints
    private static byte[] b(int... bytes) {
        byte[] arr = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) arr[i] = (byte) bytes[i];
        return arr;
    }

    // --- Factory ---

    @Test
    void create_returnsInstance() {
        assertNotNull(AnsiKeyDecoder.create());
    }

    // --- Edge cases ---

    @Test
    void decode_null_returnsEof() {
        assertEquals(KeyType.EOF, decoder.decode(null).type());
    }

    @Test
    void decode_emptyArray_returnsEof() {
        assertEquals(KeyType.EOF, decoder.decode(new byte[0]).type());
    }

    // --- Plain ASCII ---

    static Stream<Arguments> plainAsciiVectors() {
        return Stream.of(
                Arguments.of(b(13),  KeyType.ENTER,     '\0'),
                Arguments.of(b(127), KeyType.BACKSPACE,  '\0'),
                Arguments.of(b(9),   KeyType.TAB,        '\0'),
                Arguments.of(b('a'), KeyType.CHARACTER,  'a'),
                Arguments.of(b('Z'), KeyType.CHARACTER,  'Z'),
                Arguments.of(b(' '), KeyType.CHARACTER,  ' '),
                Arguments.of(b('0'), KeyType.CHARACTER,  '0'),
                Arguments.of(b(1),   KeyType.EOF,        '\0'),   // Ctrl+A (below 32)
                Arguments.of(b(31),  KeyType.EOF,        '\0')    // US (below 32)
        );
    }

    @ParameterizedTest
    @MethodSource("plainAsciiVectors")
    void decode_plainAscii(byte[] seq, KeyType expectedType, char expectedChar) {
        KeyEvent ev = decoder.decode(seq);
        assertEquals(expectedType, ev.type());
        if (expectedChar != '\0') assertEquals(expectedChar, ev.character());
    }

    // --- ESC alone ---

    @Test
    void decode_escAlone_returnsEscape() {
        assertEquals(KeyType.ESCAPE, decoder.decode(b(0x1B)).type());
    }

    // --- CSI simple arrow keys ---

    static Stream<Arguments> csiSimpleVectors() {
        return Stream.of(
                Arguments.of(b(0x1B, '[', 'A'), KeyType.ARROW_UP),
                Arguments.of(b(0x1B, '[', 'B'), KeyType.ARROW_DOWN),
                Arguments.of(b(0x1B, '[', 'C'), KeyType.ARROW_RIGHT),
                Arguments.of(b(0x1B, '[', 'D'), KeyType.ARROW_LEFT),
                Arguments.of(b(0x1B, '[', 'H'), KeyType.HOME),
                Arguments.of(b(0x1B, '[', 'F'), KeyType.END),
                Arguments.of(b(0x1B, '[', 'Z'), KeyType.SHIFT_TAB)
        );
    }

    @ParameterizedTest
    @MethodSource("csiSimpleVectors")
    void decode_csiSimple(byte[] seq, KeyType expected) {
        assertEquals(expected, decoder.decode(seq).type());
    }

    // --- SS3 sequences ---

    static Stream<Arguments> ss3Vectors() {
        return Stream.of(
                Arguments.of(b(0x1B, 'O', 'A'), KeyType.ARROW_UP),
                Arguments.of(b(0x1B, 'O', 'B'), KeyType.ARROW_DOWN),
                Arguments.of(b(0x1B, 'O', 'C'), KeyType.ARROW_RIGHT),
                Arguments.of(b(0x1B, 'O', 'D'), KeyType.ARROW_LEFT),
                Arguments.of(b(0x1B, 'O', 'H'), KeyType.HOME),
                Arguments.of(b(0x1B, 'O', 'F'), KeyType.END)
        );
    }

    @ParameterizedTest
    @MethodSource("ss3Vectors")
    void decode_ss3(byte[] seq, KeyType expected) {
        assertEquals(expected, decoder.decode(seq).type());
    }

    // --- CSI tilde sequences ---

    static Stream<Arguments> csiTildeVectors() {
        return Stream.of(
                Arguments.of(b(0x1B, '[', '1', '~'), KeyType.HOME),
                Arguments.of(b(0x1B, '[', '3', '~'), KeyType.DELETE),
                Arguments.of(b(0x1B, '[', '4', '~'), KeyType.END),
                Arguments.of(b(0x1B, '[', '5', '~'), KeyType.PAGE_UP),
                Arguments.of(b(0x1B, '[', '6', '~'), KeyType.PAGE_DOWN),
                Arguments.of(b(0x1B, '[', '7', '~'), KeyType.HOME),
                Arguments.of(b(0x1B, '[', '8', '~'), KeyType.END)
        );
    }

    @ParameterizedTest
    @MethodSource("csiTildeVectors")
    void decode_csiTilde(byte[] seq, KeyType expected) {
        assertEquals(expected, decoder.decode(seq).type());
    }

    // --- Modifier sequences ---

    @Test
    void decode_shiftUp() {
        // ESC [ 1 ; 2 A  → Shift+Up
        KeyEvent ev = decoder.decode(b(0x1B, '[', '1', ';', '2', 'A'));
        assertEquals(KeyType.ARROW_UP, ev.type());
        assertTrue(ev.shift(), "Expected shift=true");
        assertFalse(ev.ctrl());
        assertFalse(ev.alt());
    }

    @Test
    void decode_ctrlUp() {
        // ESC [ 1 ; 5 A  → Ctrl+Up
        KeyEvent ev = decoder.decode(b(0x1B, '[', '1', ';', '5', 'A'));
        assertEquals(KeyType.ARROW_UP, ev.type());
        assertTrue(ev.ctrl(), "Expected ctrl=true");
        assertFalse(ev.shift());
    }

    @Test
    void decode_altUp() {
        // ESC [ 1 ; 3 A  → Alt+Up
        KeyEvent ev = decoder.decode(b(0x1B, '[', '1', ';', '3', 'A'));
        assertEquals(KeyType.ARROW_UP, ev.type());
        assertTrue(ev.alt(), "Expected alt=true");
    }

    @Test
    void decode_ctrlShiftUp() {
        // ESC [ 1 ; 6 A  → Ctrl+Shift+Up  (mod=6: shift+ctrl)
        KeyEvent ev = decoder.decode(b(0x1B, '[', '1', ';', '6', 'A'));
        assertEquals(KeyType.ARROW_UP, ev.type());
        assertTrue(ev.ctrl());
        assertTrue(ev.shift());
    }

    @Test
    void decode_shiftDelete() {
        // ESC [ 3 ; 2 ~  → Shift+Delete
        KeyEvent ev = decoder.decode(b(0x1B, '[', '3', ';', '2', '~'));
        assertEquals(KeyType.DELETE, ev.type());
        assertTrue(ev.shift());
    }

    @Test
    void decode_ctrlPageUp() {
        // ESC [ 5 ; 5 ~  → Ctrl+PageUp
        KeyEvent ev = decoder.decode(b(0x1B, '[', '5', ';', '5', '~'));
        assertEquals(KeyType.PAGE_UP, ev.type());
        assertTrue(ev.ctrl());
    }

    // --- Alt + character ---

    @Test
    void decode_altA() {
        // ESC 'a'  → Alt+a
        KeyEvent ev = decoder.decode(b(0x1B, 'a'));
        assertEquals(KeyType.CHARACTER, ev.type());
        assertEquals('a', ev.character());
        assertTrue(ev.alt(), "Expected alt=true");
    }

    // --- Bracketed paste ---

    @Test
    void decode_bracketedPasteBegin_returnsEof() {
        // ESC [ 2 0 0 ~
        KeyEvent ev = decoder.decode(b(0x1B, '[', '2', '0', '0', '~'));
        assertEquals(KeyType.EOF, ev.type());
    }

    @Test
    void decode_bracketedPasteEnd_returnsEof() {
        // ESC [ 2 0 1 ~
        KeyEvent ev = decoder.decode(b(0x1B, '[', '2', '0', '1', '~'));
        assertEquals(KeyType.EOF, ev.type());
    }

    // --- Unknown sequences → ESCAPE or EOF ---

    @Test
    void decode_unknownCsi_returnsEscape() {
        // ESC [ X  (unknown final byte)
        KeyEvent ev = decoder.decode(b(0x1B, '[', 'X'));
        // Should not throw; must be ESCAPE
        assertEquals(KeyType.ESCAPE, ev.type());
    }

    @Test
    void decode_unknownSs3_returnsEscape() {
        KeyEvent ev = decoder.decode(b(0x1B, 'O', 'Z'));
        assertEquals(KeyType.ESCAPE, ev.type());
    }

    // --- Modifier decoding correctness ---

    @Test
    void applyModifier_mod2_isShiftOnly() {
        KeyEvent ev = decoder.decode(b(0x1B, '[', '1', ';', '2', 'A'));
        assertTrue(ev.shift());
        assertFalse(ev.alt());
        assertFalse(ev.ctrl());
    }

    @Test
    void applyModifier_mod3_isAltOnly() {
        KeyEvent ev = decoder.decode(b(0x1B, '[', '1', ';', '3', 'A'));
        assertFalse(ev.shift());
        assertTrue(ev.alt());
        assertFalse(ev.ctrl());
    }

    @Test
    void applyModifier_mod5_isCtrlOnly() {
        KeyEvent ev = decoder.decode(b(0x1B, '[', '1', ';', '5', 'A'));
        assertFalse(ev.shift());
        assertFalse(ev.alt());
        assertTrue(ev.ctrl());
    }

    @Test
    void applyModifier_mod8_isCtrlAltShift() {
        KeyEvent ev = decoder.decode(b(0x1B, '[', '1', ';', '8', 'A'));
        assertTrue(ev.shift());
        assertTrue(ev.alt());
        assertTrue(ev.ctrl());
    }
}
