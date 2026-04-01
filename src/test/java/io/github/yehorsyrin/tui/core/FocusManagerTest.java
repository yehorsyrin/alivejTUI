package io.github.yehorsyrin.tui.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FocusManagerTest {

    /** Minimal Focusable stub for testing. */
    static class TestFocusable implements Focusable {
        private boolean focused = false;
        private final String id;

        TestFocusable(String id) { this.id = id; }

        @Override public void setFocused(boolean focused) { this.focused = focused; }
        @Override public boolean isFocused() { return focused; }
        @Override public String getFocusId() { return id; }
    }

    private FocusManager fm;
    private TestFocusable a;
    private TestFocusable b;
    private TestFocusable c;

    @BeforeEach
    void setUp() {
        fm = new FocusManager();
        a = new TestFocusable("a");
        b = new TestFocusable("b");
        c = new TestFocusable("c");
    }

    // --- Registration ---

    @Test
    void registerIncreasesSize() {
        fm.register(a);
        fm.register(b);
        assertEquals(2, fm.size());
    }

    @Test
    void registerNullIgnored() {
        assertDoesNotThrow(() -> fm.register(null));
        assertEquals(0, fm.size());
    }

    @Test
    void registerDuplicateIgnored() {
        fm.register(a);
        fm.register(a);
        assertEquals(1, fm.size());
    }

    @Test
    void unregisterDecreasesSize() {
        fm.register(a);
        fm.register(b);
        fm.unregister(a);
        assertEquals(1, fm.size());
    }

    @Test
    void unregisterNullIgnored() {
        fm.register(a);
        assertDoesNotThrow(() -> fm.unregister(null));
        assertEquals(1, fm.size());
    }

    @Test
    void unregisterFocusedNodeClearsFocus() {
        fm.register(a);
        fm.focusNext();
        assertTrue(a.isFocused());
        fm.unregister(a);
        assertFalse(a.isFocused());
        assertNull(fm.getFocused());
    }

    @Test
    void clearEmptiesAndClearsFocus() {
        fm.register(a);
        fm.register(b);
        fm.focusNext();
        fm.clear();
        assertEquals(0, fm.size());
        assertFalse(a.isFocused());
        assertFalse(b.isFocused());
        assertNull(fm.getFocused());
    }

    // --- Focus cycling ---

    @Test
    void focusNextSetsFirstNode() {
        fm.register(a);
        fm.register(b);
        fm.focusNext();
        assertTrue(a.isFocused());
        assertFalse(b.isFocused());
        assertSame(a, fm.getFocused());
    }

    @Test
    void focusNextCycles() {
        fm.register(a);
        fm.register(b);
        fm.register(c);
        fm.focusNext(); // a
        fm.focusNext(); // b
        fm.focusNext(); // c
        fm.focusNext(); // back to a
        assertTrue(a.isFocused());
        assertFalse(b.isFocused());
        assertFalse(c.isFocused());
    }

    @Test
    void focusNextDefocusPrevious() {
        fm.register(a);
        fm.register(b);
        fm.focusNext(); // a focused
        fm.focusNext(); // b focused, a should lose focus
        assertFalse(a.isFocused());
        assertTrue(b.isFocused());
    }

    @Test
    void focusPrevSetsLastNode() {
        fm.register(a);
        fm.register(b);
        fm.register(c);
        fm.focusPrev(); // wraps to c (index 2)
        assertTrue(c.isFocused());
    }

    @Test
    void focusPrevCycles() {
        fm.register(a);
        fm.register(b);
        fm.focusNext(); // a
        fm.focusPrev(); // wraps back to b
        assertFalse(a.isFocused());
        assertTrue(b.isFocused());
    }

    @Test
    void focusNextOnEmptyDoesNotThrow() {
        assertDoesNotThrow(() -> fm.focusNext());
    }

    @Test
    void focusPrevOnEmptyDoesNotThrow() {
        assertDoesNotThrow(() -> fm.focusPrev());
    }

    // --- focusById ---

    @Test
    void focusByIdSetsFocusOnMatch() {
        fm.register(a);
        fm.register(b);
        fm.focusById("b");
        assertFalse(a.isFocused());
        assertTrue(b.isFocused());
    }

    @Test
    void focusByIdNoMatchDoesNothing() {
        fm.register(a);
        fm.focusNext(); // a focused
        fm.focusById("nonexistent");
        assertTrue(a.isFocused()); // still focused
    }

    @Test
    void focusByIdNullIgnored() {
        fm.register(a);
        fm.focusNext();
        assertDoesNotThrow(() -> fm.focusById(null));
        assertTrue(a.isFocused()); // still focused
    }

    // --- null safety ---

    @Test
    void getFocusedWhenNoneReturnsNull() {
        assertNull(fm.getFocused());
    }

    @Test
    void getFocusedAfterClearReturnsNull() {
        fm.register(a);
        fm.focusNext();
        fm.clear();
        assertNull(fm.getFocused());
    }
}
