package io.github.yehorsyrin.tui.platform.size;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jarvis (AI)
 */
class TerminalSizeTest {

    @Test
    void validSizeStored() {
        TerminalSize s = new TerminalSize(120, 40);
        assertEquals(120, s.cols());
        assertEquals(40, s.rows());
    }

    @Test
    void zeroColsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalSize(0, 24));
    }

    @Test
    void zeroRowsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalSize(80, 0));
    }

    @Test
    void negativeColsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalSize(-1, 24));
    }

    @Test
    void fallbackIs80x24() {
        assertEquals(80, TerminalSize.FALLBACK.cols());
        assertEquals(24, TerminalSize.FALLBACK.rows());
    }

    @Test
    void equalityByValue() {
        assertEquals(new TerminalSize(80, 24), new TerminalSize(80, 24));
    }

    @Test
    void inequalityOnDifferentValues() {
        assertNotEquals(new TerminalSize(80, 24), new TerminalSize(100, 30));
    }
}
