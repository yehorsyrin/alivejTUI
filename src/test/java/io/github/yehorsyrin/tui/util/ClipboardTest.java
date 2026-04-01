package io.github.yehorsyrin.tui.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Clipboard}.
 *
 * <p>Copy/paste tests are disabled in headless CI environments where the system clipboard
 * is not available. All tests must tolerate headless gracefully (no exceptions).
 *
 * @author Jarvis (AI)
 */
class ClipboardTest {

    @Test
    void copy_null_doesNotThrow() {
        assertDoesNotThrow(() -> Clipboard.copy(null));
    }

    @Test
    void copy_emptyString_doesNotThrow() {
        assertDoesNotThrow(() -> Clipboard.copy(""));
    }

    @Test
    void paste_doesNotThrow() {
        assertDoesNotThrow(() -> Clipboard.paste());
    }

    @Test
    void paste_returnsNonNull() {
        assertNotNull(Clipboard.paste());
    }

    @Test
    void hasText_doesNotThrow() {
        assertDoesNotThrow(() -> Clipboard.hasText());
    }

    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void copyAndPaste_roundTrip() {
        String text = "AliveJTUI clipboard test \u2713";
        Clipboard.copy(text);
        String pasted = Clipboard.paste();
        assertEquals(text, pasted);
    }

    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void hasText_trueAfterCopy() {
        Clipboard.copy("test");
        assertTrue(Clipboard.hasText());
    }

    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void copy_null_pasteReturnsEmpty() {
        Clipboard.copy(null);
        // null is normalized to "" — pasting should return ""
        assertEquals("", Clipboard.paste());
    }
}
