package io.alive.tui.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MouseEvent} and {@link MouseType}.
 *
 * @author Jarvis (AI)
 */
class MouseEventTest {

    @Test
    void record_storesAllFields() {
        MouseEvent e = new MouseEvent(MouseType.PRESS, 10, 5, 0);
        assertEquals(MouseType.PRESS, e.type());
        assertEquals(10, e.col());
        assertEquals(5,  e.row());
        assertEquals(0,  e.button());
    }

    @Test
    void factory_press() {
        MouseEvent e = MouseEvent.press(3, 7);
        assertEquals(MouseType.PRESS, e.type());
        assertEquals(3, e.col());
        assertEquals(7, e.row());
        assertEquals(0, e.button());
    }

    @Test
    void factory_release() {
        MouseEvent e = MouseEvent.release(1, 2);
        assertEquals(MouseType.RELEASE, e.type());
    }

    @Test
    void factory_click() {
        MouseEvent e = MouseEvent.click(5, 5);
        assertEquals(MouseType.CLICK, e.type());
        assertEquals(0, e.button());
    }

    @Test
    void factory_scrollUp() {
        MouseEvent e = MouseEvent.scrollUp(0, 0);
        assertEquals(MouseType.SCROLL_UP, e.type());
        assertEquals(-1, e.button());
    }

    @Test
    void factory_scrollDown() {
        MouseEvent e = MouseEvent.scrollDown(0, 0);
        assertEquals(MouseType.SCROLL_DOWN, e.type());
        assertEquals(-1, e.button());
    }

    @Test
    void equality_sameFields() {
        MouseEvent a = new MouseEvent(MouseType.CLICK, 4, 4, 1);
        MouseEvent b = new MouseEvent(MouseType.CLICK, 4, 4, 1);
        assertEquals(a, b);
    }

    @Test
    void keyEvent_ofMouse_typeIsMouseKey() {
        MouseEvent m = MouseEvent.click(2, 3);
        KeyEvent ke = KeyEvent.ofMouse(m);
        assertEquals(KeyType.MOUSE, ke.type());
        assertEquals(m, ke.mouseEvent());
    }

    @Test
    void keyEvent_ofMouse_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> KeyEvent.ofMouse(null));
    }

    @Test
    void keyEvent_regularKey_mouseEventIsNull() {
        KeyEvent ke = KeyEvent.of(KeyType.ENTER);
        assertNull(ke.mouseEvent());
    }
}
