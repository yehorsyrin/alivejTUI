package io.alive.tui.node;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InputNodeTest {

    @Test
    void initialValueAndCursorAtEnd() {
        InputNode input = Input.of("hello", v -> {});
        assertEquals("hello", input.getValue());
        assertEquals(5, input.getCursorPos());
    }

    @Test
    void nullValueBecomesEmpty() {
        InputNode input = Input.of(null, null);
        assertEquals("", input.getValue());
    }

    @Test
    void setValueFiresOnChange() {
        String[] captured = {null};
        InputNode input = new InputNode("", v -> captured[0] = v);
        input.setValue("new");
        assertEquals("new", captured[0]);
    }

    @Test
    void setCursorPosClamped() {
        InputNode input = Input.of("abc", v -> {});
        input.setCursorPos(100);
        assertEquals(3, input.getCursorPos());
        input.setCursorPos(-5);
        assertEquals(0, input.getCursorPos());
    }

    @Test
    void placeholderFluent() {
        InputNode input = Input.of("", v -> {}).placeholder("Type here...");
        assertEquals("Type here...", input.getPlaceholder());
    }

    @Test
    void focusedDefaultFalse() {
        assertFalse(Input.of("", v -> {}).isFocused());
    }
}
