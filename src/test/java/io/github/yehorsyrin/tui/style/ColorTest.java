package io.github.yehorsyrin.tui.style;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorTest {

    @Test
    void ansi16ConstantsHaveCorrectIndices() {
        assertEquals(Color.ColorType.ANSI_16, Color.BLACK.getType());
        assertEquals(0, Color.BLACK.getAnsiIndex());
        assertEquals(7, Color.WHITE.getAnsiIndex());
        assertEquals(15, Color.BRIGHT_WHITE.getAnsiIndex());
    }

    @Test
    void ansi256RejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> Color.ansi256(-1));
        assertThrows(IllegalArgumentException.class, () -> Color.ansi256(256));
    }

    @Test
    void ansi256AcceptsValidRange() {
        Color c = Color.ansi256(128);
        assertEquals(Color.ColorType.ANSI_256, c.getType());
        assertEquals(128, c.getAnsiIndex());
    }

    @Test
    void rgbStoresValues() {
        Color c = Color.rgb(10, 20, 30);
        assertEquals(Color.ColorType.RGB, c.getType());
        assertEquals(10, c.getR());
        assertEquals(20, c.getG());
        assertEquals(30, c.getB());
    }

    @Test
    void rgbRejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> Color.rgb(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Color.rgb(0, 256, 0));
        assertThrows(IllegalArgumentException.class, () -> Color.rgb(0, 0, 300));
    }

    @Test
    void equalityAndHashCode() {
        Color a = Color.rgb(1, 2, 3);
        Color b = Color.rgb(1, 2, 3);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Color.rgb(1, 2, 4));
    }

    @Test
    void toStringIsReadable() {
        assertTrue(Color.GREEN.toString().contains("ANSI_16"));
        assertTrue(Color.ansi256(100).toString().contains("ANSI_256"));
        assertTrue(Color.rgb(1, 2, 3).toString().contains("RGB"));
    }
}
