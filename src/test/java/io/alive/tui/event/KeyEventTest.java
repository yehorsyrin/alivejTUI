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
}
