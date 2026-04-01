package io.github.yehorsyrin.tui.platform.input;

import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.event.KeyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test vectors for AnsiKeyDecoder.
 * Each test vector is a raw byte sequence → expected KeyEvent.
 *
 * @author Jarvis (AI)
 */
class AnsiKeyDecoderTest {

    // ── Null / empty ──────────────────────────────────────────────────────────

    @Test
    void nullSequenceReturnsEof() {
        assertEquals(KeyType.EOF, AnsiKeyDecoder.decode(null).type());
    }

    @Test
    void emptySequenceReturnsEof() {
        assertEquals(KeyType.EOF, AnsiKeyDecoder.decode(new byte[0]).type());
    }

    // ── Single-byte: control ──────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("controlBytes")
    void controlByte(String label, byte[] seq, KeyType expectedType) {
        assertEquals(expectedType, AnsiKeyDecoder.decode(seq).type(), label);
    }

    static Stream<Arguments> controlBytes() {
        return Stream.of(
            Arguments.of("CR (\\r) → ENTER",       b(0x0D), KeyType.ENTER),
            Arguments.of("LF (\\n) → ENTER",       b(0x0A), KeyType.ENTER),
            Arguments.of("DEL (0x7F) → BACKSPACE", b(0x7F), KeyType.BACKSPACE),
            Arguments.of("BS  (0x08) → BACKSPACE",  b(0x08), KeyType.BACKSPACE),
            Arguments.of("TAB (0x09) → TAB",        b(0x09), KeyType.TAB),
            Arguments.of("ESC (0x1B) → ESCAPE",     b(0x1B), KeyType.ESCAPE),
            Arguments.of("NUL (0x00) → EOF",        b(0x00), KeyType.EOF),
            Arguments.of("Ctrl+D (0x04) → EOF",     b(0x04), KeyType.EOF)
        );
    }

    // ── Single-byte: printable ────────────────────────────────────────────────

    @Test
    void printableAsciiLetterIsCharacter() {
        KeyEvent e = AnsiKeyDecoder.decode(b('a'));
        assertEquals(KeyType.CHARACTER, e.type());
        assertEquals('a', e.character());
        assertFalse(e.ctrl());
    }

    @Test
    void printableUpperCaseIsCharacter() {
        KeyEvent e = AnsiKeyDecoder.decode(b('Z'));
        assertEquals(KeyType.CHARACTER, e.type());
        assertEquals('Z', e.character());
    }

    @Test
    void digitIsCharacter() {
        KeyEvent e = AnsiKeyDecoder.decode(b('5'));
        assertEquals(KeyType.CHARACTER, e.type());
        assertEquals('5', e.character());
    }

    @Test
    void spaceIsCharacter() {
        KeyEvent e = AnsiKeyDecoder.decode(b(' '));
        assertEquals(KeyType.CHARACTER, e.type());
        assertEquals(' ', e.character());
    }

    // ── Ctrl+letter ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "Ctrl+{0}")
    @MethodSource("ctrlLetters")
    void ctrlLetter(String letter, byte raw, char expectedChar) {
        KeyEvent e = AnsiKeyDecoder.decode(b(raw));
        assertEquals(KeyType.CHARACTER, e.type(), "Ctrl+" + letter + " type");
        assertEquals(expectedChar, e.character(), "Ctrl+" + letter + " char");
        assertTrue(e.ctrl(), "Ctrl+" + letter + " ctrl flag");
        assertFalse(e.alt());
    }

    static Stream<Arguments> ctrlLetters() {
        return Stream.of(
            Arguments.of("A", (byte) 0x01, 'a'),
            Arguments.of("B", (byte) 0x02, 'b'),
            Arguments.of("C", (byte) 0x03, 'c'),
            Arguments.of("L", (byte) 0x0C, 'l'),
            Arguments.of("Z", (byte) 0x1A, 'z')
        );
    }

    // ── CSI: arrows (no modifier) ─────────────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("csiArrows")
    void csiArrow(String label, byte[] seq, KeyType expectedType) {
        KeyEvent e = AnsiKeyDecoder.decode(seq);
        assertEquals(expectedType, e.type(), label);
        assertFalse(e.ctrl());
        assertFalse(e.alt());
        assertFalse(e.shift());
    }

    static Stream<Arguments> csiArrows() {
        return Stream.of(
            Arguments.of("ESC[A → ARROW_UP",    esc('[', 'A'), KeyType.ARROW_UP),
            Arguments.of("ESC[B → ARROW_DOWN",  esc('[', 'B'), KeyType.ARROW_DOWN),
            Arguments.of("ESC[C → ARROW_RIGHT", esc('[', 'C'), KeyType.ARROW_RIGHT),
            Arguments.of("ESC[D → ARROW_LEFT",  esc('[', 'D'), KeyType.ARROW_LEFT),
            Arguments.of("ESC[H → HOME",        esc('[', 'H'), KeyType.HOME),
            Arguments.of("ESC[F → END",         esc('[', 'F'), KeyType.END)
        );
    }

    // ── CSI: SS3 arrows ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("ss3Sequences")
    void ss3Arrow(String label, byte[] seq, KeyType expectedType) {
        assertEquals(expectedType, AnsiKeyDecoder.decode(seq).type(), label);
    }

    static Stream<Arguments> ss3Sequences() {
        return Stream.of(
            Arguments.of("ESC OA → ARROW_UP",    esc('O', 'A'), KeyType.ARROW_UP),
            Arguments.of("ESC OB → ARROW_DOWN",  esc('O', 'B'), KeyType.ARROW_DOWN),
            Arguments.of("ESC OC → ARROW_RIGHT", esc('O', 'C'), KeyType.ARROW_RIGHT),
            Arguments.of("ESC OD → ARROW_LEFT",  esc('O', 'D'), KeyType.ARROW_LEFT),
            Arguments.of("ESC OH → HOME",        esc('O', 'H'), KeyType.HOME),
            Arguments.of("ESC OF → END",         esc('O', 'F'), KeyType.END)
        );
    }

    // ── CSI: tilde sequences ──────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("tildeSequences")
    void tildeSequence(String label, byte[] seq, KeyType expectedType) {
        assertEquals(expectedType, AnsiKeyDecoder.decode(seq).type(), label);
    }

    static Stream<Arguments> tildeSequences() {
        return Stream.of(
            Arguments.of("ESC[1~ → HOME",      csi("1~"),  KeyType.HOME),
            Arguments.of("ESC[4~ → END",       csi("4~"),  KeyType.END),
            Arguments.of("ESC[7~ → HOME",      csi("7~"),  KeyType.HOME),
            Arguments.of("ESC[8~ → END",       csi("8~"),  KeyType.END),
            Arguments.of("ESC[3~ → DELETE",    csi("3~"),  KeyType.DELETE),
            Arguments.of("ESC[5~ → PAGE_UP",   csi("5~"),  KeyType.PAGE_UP),
            Arguments.of("ESC[6~ → PAGE_DOWN", csi("6~"),  KeyType.PAGE_DOWN)
        );
    }

    // ── Shift+Tab ─────────────────────────────────────────────────────────────

    @Test
    void shiftTab() {
        byte[] seq = csi("Z");
        assertEquals(KeyType.SHIFT_TAB, AnsiKeyDecoder.decode(seq).type());
    }

    // ── Arrows with modifiers ─────────────────────────────────────────────────

    @Test
    void shiftUpArrow() {
        // ESC [ 1 ; 2 A  (modifier=2 → Shift)
        byte[] seq = csi("1;2A");
        KeyEvent e = AnsiKeyDecoder.decode(seq);
        assertEquals(KeyType.ARROW_UP, e.type());
        assertTrue(e.shift());
        assertFalse(e.ctrl());
        assertFalse(e.alt());
    }

    @Test
    void ctrlUpArrow() {
        // ESC [ 1 ; 5 A  (modifier=5 → Ctrl)
        byte[] seq = csi("1;5A");
        KeyEvent e = AnsiKeyDecoder.decode(seq);
        assertEquals(KeyType.ARROW_UP, e.type());
        assertTrue(e.ctrl());
        assertFalse(e.shift());
        assertFalse(e.alt());
    }

    @Test
    void altUpArrow() {
        // ESC [ 1 ; 3 A  (modifier=3 → Alt)
        byte[] seq = csi("1;3A");
        KeyEvent e = AnsiKeyDecoder.decode(seq);
        assertEquals(KeyType.ARROW_UP, e.type());
        assertTrue(e.alt());
        assertFalse(e.ctrl());
        assertFalse(e.shift());
    }

    @Test
    void ctrlShiftUpArrow() {
        // modifier=6 → Ctrl+Shift (bits: Shift=1, Ctrl=4 → 5, +1 = 6)
        byte[] seq = csi("1;6A");
        KeyEvent e = AnsiKeyDecoder.decode(seq);
        assertEquals(KeyType.ARROW_UP, e.type());
        assertTrue(e.ctrl());
        assertTrue(e.shift());
    }

    @Test
    void shiftDelete() {
        // ESC [ 3 ; 2 ~  (n=3=DELETE, modifier=2=Shift)
        byte[] seq = csi("3;2~");
        KeyEvent e = AnsiKeyDecoder.decode(seq);
        assertEquals(KeyType.DELETE, e.type());
        assertTrue(e.shift());
    }

    @Test
    void ctrlPageUp() {
        // ESC [ 5 ; 5 ~  (n=5=PAGE_UP, modifier=5=Ctrl)
        byte[] seq = csi("5;5~");
        KeyEvent e = AnsiKeyDecoder.decode(seq);
        assertEquals(KeyType.PAGE_UP, e.type());
        assertTrue(e.ctrl());
    }

    // ── Alt+key ───────────────────────────────────────────────────────────────

    @Test
    void altEnter() {
        // ESC + \r
        byte[] seq = {0x1B, 0x0D};
        KeyEvent e = AnsiKeyDecoder.decode(seq);
        assertEquals(KeyType.ENTER, e.type());
        assertTrue(e.alt());
    }

    @Test
    void altLetter() {
        // ESC + 'a'
        byte[] seq = {0x1B, 'a'};
        KeyEvent e = AnsiKeyDecoder.decode(seq);
        assertEquals(KeyType.CHARACTER, e.type());
        assertEquals('a', e.character());
        assertTrue(e.alt());
    }

    // ── Modifier flags: no modifier = all false ───────────────────────────────

    @Test
    void noModifierFlagsFalseForPlainArrow() {
        KeyEvent e = AnsiKeyDecoder.decode(esc('[', 'A'));
        assertFalse(e.ctrl());
        assertFalse(e.alt());
        assertFalse(e.shift());
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    @Test
    void unknownSequenceFallsBackToCharacter() {
        byte[] seq = {0x1B, '[', 'X'};
        KeyEvent e = AnsiKeyDecoder.decode(seq);
        assertNotNull(e);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Single byte. */
    private static byte[] b(int v) { return new byte[]{(byte) v}; }

    /** ESC + second + final. */
    private static byte[] esc(char second, char finalByte) {
        return new byte[]{0x1B, (byte) second, (byte) finalByte};
    }

    /** ESC [ params... */
    private static byte[] csi(String params) {
        byte[] result = new byte[2 + params.length()];
        result[0] = 0x1B;
        result[1] = '[';
        for (int i = 0; i < params.length(); i++) result[2 + i] = (byte) params.charAt(i);
        return result;
    }
}
