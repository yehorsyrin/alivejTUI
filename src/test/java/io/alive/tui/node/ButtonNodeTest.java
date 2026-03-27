package io.alive.tui.node;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ButtonNodeTest {

    @Test
    void labelAndCallback() {
        boolean[] called = {false};
        ButtonNode btn = Button.of("OK", () -> called[0] = true);
        assertEquals("OK", btn.getLabel());
        btn.click();
        assertTrue(called[0]);
    }

    @Test
    void nullOnClickDoesNotThrow() {
        ButtonNode btn = Button.of("X", null);
        assertDoesNotThrow(btn::click);
    }

    @Test
    void nullLabelBecomesEmpty() {
        ButtonNode btn = new ButtonNode(null, null);
        assertEquals("", btn.getLabel());
    }

    @Test
    void focusedDefaultFalse() {
        assertFalse(Button.of("X", () -> {}).isFocused());
    }

    @Test
    void setFocused() {
        ButtonNode btn = Button.of("X", () -> {});
        btn.setFocused(true);
        assertTrue(btn.isFocused());
    }
}
