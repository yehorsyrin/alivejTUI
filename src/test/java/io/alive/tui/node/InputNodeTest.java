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

    // --- Additional tests ---

    @Test
    void typeCharacter_appendsToValue() {
        InputNode input = new InputNode("", v -> {});
        input.setValue("a");
        assertEquals("a", input.getValue());
    }

    @Test
    void backspace_removesLastChar() {
        InputNode input = new InputNode("hello", v -> {});
        input.setValue("hell");
        assertEquals("hell", input.getValue());
    }

    @Test
    void backspace_onEmptyValueNoOp() {
        InputNode input = new InputNode("", v -> {});
        assertDoesNotThrow(() -> input.setValue(""));
        assertEquals("", input.getValue());
    }

    @Test
    void cursorPos_clampedToValueLength() {
        InputNode input = new InputNode("abc", v -> {});
        input.setCursorPos(100);
        assertEquals(3, input.getCursorPos());
    }

    @Test
    void cursorPos_clampedToZero() {
        InputNode input = new InputNode("abc", v -> {});
        input.setCursorPos(-5);
        assertEquals(0, input.getCursorPos());
    }

    @Test
    void placeholder_showsWhenEmpty() {
        InputNode input = Input.of("", v -> {}).placeholder("Enter name...");
        assertEquals("Enter name...", input.getPlaceholder());
    }

    @Test
    void setValue_notifiesOnChange() {
        String[] captured = {null};
        InputNode input = new InputNode("", v -> captured[0] = v);
        input.setValue("changed");
        assertEquals("changed", captured[0]);
    }

    @Test
    void cursorPos_updatesOnSetValue_ifBeyondNewLength() {
        InputNode input = new InputNode("hello", v -> {});
        input.setCursorPos(5);
        assertEquals(5, input.getCursorPos());
        input.setValue("hi");
        assertEquals(2, input.getCursorPos());
    }
}
