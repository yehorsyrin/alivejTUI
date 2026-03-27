package io.alive.tui.style;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StyleTest {

    @Test
    void defaultStyleHasNoDecorations() {
        Style s = Style.DEFAULT;
        assertNull(s.getForeground());
        assertNull(s.getBackground());
        assertFalse(s.isBold());
        assertFalse(s.isItalic());
        assertFalse(s.isUnderline());
        assertFalse(s.isDim());
        assertFalse(s.isStrikethrough());
    }

    @Test
    void withMethodsReturnNewInstance() {
        Style base = Style.DEFAULT;
        Style bold = base.withBold(true);
        assertNotSame(base, bold);
        assertFalse(base.isBold());
        assertTrue(bold.isBold());
    }

    @Test
    void withForegroundPreservesOtherFields() {
        Style s = Style.DEFAULT.withBold(true).withForeground(Color.RED);
        assertEquals(Color.RED, s.getForeground());
        assertTrue(s.isBold());
    }

    @Test
    void equalityBasedOnFields() {
        Style a = new Style(Color.GREEN, null, true, false, false, false, false);
        Style b = new Style(Color.GREEN, null, true, false, false, false, false);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void inequalityOnDifferentFields() {
        Style a = Style.DEFAULT.withBold(true);
        Style b = Style.DEFAULT.withItalic(true);
        assertNotEquals(a, b);
    }
}
