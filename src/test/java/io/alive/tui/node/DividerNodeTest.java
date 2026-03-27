package io.alive.tui.node;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DividerNodeTest {

    @Test
    void horizontalDivider() {
        DividerNode d = Divider.horizontal();
        assertEquals(DividerNode.Orientation.HORIZONTAL, d.getOrientation());
        assertEquals(DividerNode.DEFAULT_HORIZONTAL_CHAR, d.getCharacter());
    }

    @Test
    void verticalDivider() {
        DividerNode d = Divider.vertical();
        assertEquals(DividerNode.Orientation.VERTICAL, d.getOrientation());
        assertEquals(DividerNode.DEFAULT_VERTICAL_CHAR, d.getCharacter());
    }

    @Test
    void customCharacter() {
        DividerNode d = Divider.horizontal().character('=');
        assertEquals('=', d.getCharacter());
    }
}
