package io.alive.tui.nativeio;

import io.alive.tui.event.MouseEvent;
import io.alive.tui.event.MouseType;
import io.alive.tui.nativeio.input.MouseDecoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MouseDecoder} — SGR mouse sequence → {@link MouseEvent}.
 *
 * @author Jarvis (AI)
 */
class MouseDecoderTest {

    // Helper: build byte array from ints
    private static byte[] b(int... ints) {
        byte[] arr = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) arr[i] = (byte) ints[i];
        return arr;
    }

    // ESC '[' '<' params M/m — build from string params
    private static byte[] sgr(String params, char finalByte) {
        String full = "\033[<" + params + finalByte;
        return full.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    // --- Edge cases ---

    @Test
    void decode_null_returnsNull() {
        assertNull(MouseDecoder.decode(null));
    }

    @Test
    void decode_tooShort_returnsNull() {
        assertNull(MouseDecoder.decode(b(0x1B, '[', '<')));
    }

    @Test
    void decode_notEsc_returnsNull() {
        assertNull(MouseDecoder.decode(b('X', '[', '<', '0', ';', '1', ';', '1', 'M')));
    }

    @Test
    void decode_notBracket_returnsNull() {
        assertNull(MouseDecoder.decode(b(0x1B, 'O', '<', '0', ';', '1', ';', '1', 'M')));
    }

    @Test
    void decode_notLessThan_returnsNull() {
        assertNull(MouseDecoder.decode(b(0x1B, '[', '0', '0', ';', '1', ';', '1', 'M')));
    }

    @Test
    void decode_invalidFinalByte_returnsNull() {
        assertNull(MouseDecoder.decode(sgr("0;1;1", 'X')));
    }

    @Test
    void decode_malformedParams_returnsNull() {
        assertNull(MouseDecoder.decode(sgr("garbage", 'M')));
    }

    // --- Left button press ---

    @Test
    void decode_leftButtonPress() {
        // ESC [ < 0 ; col ; row M  (col=10, row=5 → 0-based: 9, 4)
        MouseEvent ev = MouseDecoder.decode(sgr("0;10;5", 'M'));
        assertNotNull(ev);
        assertEquals(MouseType.PRESS, ev.type());
        assertEquals(9, ev.col());
        assertEquals(4, ev.row());
        assertEquals(0, ev.button()); // left
    }

    // --- Left button release ---

    @Test
    void decode_leftButtonRelease() {
        MouseEvent ev = MouseDecoder.decode(sgr("0;5;3", 'm'));
        assertNotNull(ev);
        assertEquals(MouseType.RELEASE, ev.type());
        assertEquals(4, ev.col());
        assertEquals(2, ev.row());
        assertEquals(0, ev.button());
    }

    // --- Middle button ---

    @Test
    void decode_middleButtonPress() {
        MouseEvent ev = MouseDecoder.decode(sgr("1;1;1", 'M'));
        assertNotNull(ev);
        assertEquals(MouseType.PRESS, ev.type());
        assertEquals(1, ev.button()); // middle
    }

    // --- Right button ---

    @Test
    void decode_rightButtonPress() {
        MouseEvent ev = MouseDecoder.decode(sgr("2;20;10", 'M'));
        assertNotNull(ev);
        assertEquals(MouseType.PRESS, ev.type());
        assertEquals(2, ev.button()); // right
        assertEquals(19, ev.col());
        assertEquals(9, ev.row());
    }

    // --- Scroll up ---

    @Test
    void decode_scrollUp() {
        // Cb = 64 → scroll up
        MouseEvent ev = MouseDecoder.decode(sgr("64;1;1", 'M'));
        assertNotNull(ev);
        assertEquals(MouseType.SCROLL_UP, ev.type());
        assertEquals(0, ev.col());
        assertEquals(0, ev.row());
    }

    // --- Scroll down ---

    @Test
    void decode_scrollDown() {
        // Cb = 65 → scroll down
        MouseEvent ev = MouseDecoder.decode(sgr("65;5;3", 'M'));
        assertNotNull(ev);
        assertEquals(MouseType.SCROLL_DOWN, ev.type());
    }

    // --- Coordinates: 1-based to 0-based conversion ---

    @Test
    void decode_coordinates_convertTo0Based() {
        MouseEvent ev = MouseDecoder.decode(sgr("0;1;1", 'M'));
        assertNotNull(ev);
        assertEquals(0, ev.col());
        assertEquals(0, ev.row());
    }

    @Test
    void decode_largeCoordinates() {
        MouseEvent ev = MouseDecoder.decode(sgr("0;200;50", 'M'));
        assertNotNull(ev);
        assertEquals(199, ev.col());
        assertEquals(49,  ev.row());
    }

    // --- decodeString ---

    @Test
    void decodeString_null_returnsNull() {
        assertNull(MouseDecoder.decodeString(null));
    }

    @Test
    void decodeString_validSequence_decodesCorrectly() {
        MouseEvent ev = MouseDecoder.decodeString("\033[<0;5;3M");
        assertNotNull(ev);
        assertEquals(MouseType.PRESS, ev.type());
        assertEquals(4, ev.col());
        assertEquals(2, ev.row());
    }

    @Test
    void decodeString_scrollDown() {
        MouseEvent ev = MouseDecoder.decodeString("\033[<65;10;5M");
        assertNotNull(ev);
        assertEquals(MouseType.SCROLL_DOWN, ev.type());
    }

    // --- Parametrized: various button+action combinations ---

    static Stream<Arguments> buttonVectors() {
        return Stream.of(
                Arguments.of("0;1;1", 'M', MouseType.PRESS,    0),
                Arguments.of("0;1;1", 'm', MouseType.RELEASE,  0),
                Arguments.of("1;1;1", 'M', MouseType.PRESS,    1),
                Arguments.of("2;1;1", 'M', MouseType.PRESS,    2),
                Arguments.of("64;1;1", 'M', MouseType.SCROLL_UP,   -1),
                Arguments.of("65;1;1", 'M', MouseType.SCROLL_DOWN, -1)
        );
    }

    @ParameterizedTest
    @MethodSource("buttonVectors")
    void decode_buttonVectors(String params, char fin, MouseType expectedType, int expectedButton) {
        MouseEvent ev = MouseDecoder.decode(sgr(params, fin));
        assertNotNull(ev, "Expected non-null for params=" + params);
        assertEquals(expectedType, ev.type());
        if (expectedButton >= 0) assertEquals(expectedButton, ev.button());
    }
}
