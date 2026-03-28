package io.alive.tui.event;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeyEventTest {

    @Test
    void ofCharacterSetsTypeAndChar() {
        KeyEvent e = KeyEvent.ofCharacter('a');
        assertEquals(KeyType.CHARACTER, e.type());
        assertEquals('a', e.character());
    }

    @Test
    void ofTypeSetsNullChar() {
        KeyEvent e = KeyEvent.of(KeyType.ENTER);
        assertEquals(KeyType.ENTER, e.type());
        assertEquals('\0', e.character());
    }

    @Test
    void recordEquality() {
        assertEquals(KeyEvent.of(KeyType.ESCAPE), KeyEvent.of(KeyType.ESCAPE));
        assertNotEquals(KeyEvent.of(KeyType.ENTER), KeyEvent.of(KeyType.ESCAPE));
    }

    @Test
    void allKeyTypesAreAccessible() {
        // Ensure all enum values exist (no typos)
        for (KeyType kt : KeyType.values()) {
            assertNotNull(kt);
        }
    }

    @Test
    void modifiers_defaultFalse() {
        KeyEvent e = KeyEvent.of(KeyType.ENTER);
        assertFalse(e.ctrl());
        assertFalse(e.alt());
        assertFalse(e.shift());
    }

    @Test
    void ofCharacterWithModifiers() {
        KeyEvent e = KeyEvent.ofCharacter('c', true, false, false);
        assertEquals(KeyType.CHARACTER, e.type());
        assertEquals('c', e.character());
        assertTrue(e.ctrl());
        assertFalse(e.alt());
        assertFalse(e.shift());
    }

    @Test
    void ofTypeWithModifiers() {
        KeyEvent e = KeyEvent.of(KeyType.ARROW_UP, false, true, true);
        assertEquals(KeyType.ARROW_UP, e.type());
        assertFalse(e.ctrl());
        assertTrue(e.alt());
        assertTrue(e.shift());
    }

    @Test
    void modifiersCombined() {
        KeyEvent e = KeyEvent.of(KeyType.DELETE, true, true, true);
        assertTrue(e.ctrl());
        assertTrue(e.alt());
        assertTrue(e.shift());
    }
}
