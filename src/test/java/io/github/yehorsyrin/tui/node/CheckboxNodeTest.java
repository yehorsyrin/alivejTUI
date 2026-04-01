package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.backend.TerminalBackend;
import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.layout.LayoutEngine;
import io.github.yehorsyrin.tui.render.Renderer;
import io.github.yehorsyrin.tui.style.Color;
import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CheckboxNode} and its factory {@link Checkbox}.
 *
 * @author Jarvis (AI)
 */
class CheckboxNodeTest {

    // --- FakeBackend for render-level tests ---

    static class FakeBackend implements TerminalBackend {
        final List<String> putChars = new ArrayList<>();

        @Override public void init() {}
        @Override public void shutdown() {}
        @Override public int getWidth()  { return 80; }
        @Override public int getHeight() { return 24; }
        @Override public void putChar(int col, int row, char c, Style style) {
            putChars.add(col + "," + row + "=" + c);
        }
        @Override public void flush() {}
        @Override public void hideCursor() {}
        @Override public void showCursor() {}
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() { putChars.clear(); }
        @Override public KeyEvent readKey() throws InterruptedException { return KeyEvent.of(KeyType.EOF); }
        @Override public void setResizeListener(Runnable r) {}
    }

    // --- Construction ---

    @Test
    void factoryCreatesNode() {
        CheckboxNode cb = Checkbox.of("Option A", false, null);
        assertNotNull(cb);
        assertEquals("Option A", cb.getLabel());
        assertFalse(cb.isChecked());
    }

    @Test
    void constructorWithCheckedTrue() {
        CheckboxNode cb = new CheckboxNode("Checked", true, null);
        assertTrue(cb.isChecked());
    }

    @Test
    void nullLabelBecomesEmpty() {
        CheckboxNode cb = new CheckboxNode(null, false, null);
        assertEquals("", cb.getLabel());
    }

    // --- Toggle ---

    @Test
    void toggleUncheckedBecomesChecked() {
        CheckboxNode cb = Checkbox.of("A", false, null);
        cb.toggle();
        assertTrue(cb.isChecked());
    }

    @Test
    void toggleCheckedBecomesUnchecked() {
        CheckboxNode cb = Checkbox.of("A", true, null);
        cb.toggle();
        assertFalse(cb.isChecked());
    }

    @Test
    void toggleTwiceRestoresOriginalState() {
        CheckboxNode cb = Checkbox.of("A", false, null);
        cb.toggle();
        cb.toggle();
        assertFalse(cb.isChecked());
    }

    @Test
    void onChangeCalledOnToggle() {
        boolean[] called = {false};
        CheckboxNode cb = Checkbox.of("A", false, () -> called[0] = true);
        cb.toggle();
        assertTrue(called[0]);
    }

    @Test
    void onChangeNullDoesNotThrow() {
        CheckboxNode cb = Checkbox.of("A", false, null);
        assertDoesNotThrow(cb::toggle);
    }

    @Test
    void onChangeCalledEachToggle() {
        int[] count = {0};
        CheckboxNode cb = Checkbox.of("A", false, () -> count[0]++);
        cb.toggle();
        cb.toggle();
        cb.toggle();
        assertEquals(3, count[0]);
    }

    // --- Focus ---

    @Test
    void focusedDefaultFalse() {
        CheckboxNode cb = Checkbox.of("A", false, null);
        assertFalse(cb.isFocused());
    }

    @Test
    void setFocused() {
        CheckboxNode cb = Checkbox.of("A", false, null);
        cb.setFocused(true);
        assertTrue(cb.isFocused());
    }

    @Test
    void getFocusIdReturnsLabel() {
        CheckboxNode cb = Checkbox.of("MyOption", false, null);
        assertEquals("MyOption", cb.getFocusId());
    }

    // --- Style ---

    @Test
    void defaultStyleIsDefault() {
        CheckboxNode cb = Checkbox.of("A", false, null);
        assertEquals(Style.DEFAULT, cb.getStyle());
    }

    @Test
    void focusedStyleIsYellowBold() {
        CheckboxNode cb = Checkbox.of("A", false, null);
        Style fs = cb.getFocusedStyle();
        assertTrue(fs.isBold(), "focused style should be bold");
        assertEquals(Color.YELLOW, fs.getForeground(), "focused style foreground should be YELLOW");
    }

    // --- Layout ---

    @Test
    void layoutWidth_unchecked() {
        CheckboxNode cb = Checkbox.of("Hello", false, null);
        LayoutEngine le = new LayoutEngine();
        le.layout(cb, 0, 0, 80, 24);
        // "[✓] ".length() = 4, "Hello".length() = 5 → 9
        assertEquals(9, cb.getWidth());
        assertEquals(1, cb.getHeight());
    }

    @Test
    void layoutWidth_clampedToAvailable() {
        CheckboxNode cb = Checkbox.of("Hello", false, null);
        LayoutEngine le = new LayoutEngine();
        le.layout(cb, 0, 0, 5, 24);
        assertEquals(5, cb.getWidth());
        assertEquals(1, cb.getHeight());
    }

    // --- Rendering ---

    @Test
    void renderUnchecked_containsEmptyBox() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        CheckboxNode cb = Checkbox.of("Hi", false, null);
        renderer.render(cb);
        // "[ ] Hi" → characters: [, space, ], space, H, i
        assertTrue(backend.putChars.contains("0,0=["), "should render '['");
        assertTrue(backend.putChars.contains("1,0= "), "should render space (unchecked)");
        assertTrue(backend.putChars.contains("2,0=]"), "should render ']'");
        assertTrue(backend.putChars.contains("4,0=H"), "should render 'H'");
        assertTrue(backend.putChars.contains("5,0=i"), "should render 'i'");
    }

    @Test
    void renderChecked_containsCheckMark() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        CheckboxNode cb = Checkbox.of("Hi", true, null);
        renderer.render(cb);
        // "[✓] Hi" → col 1 = ✓
        assertTrue(backend.putChars.contains("1,0=✓"), "should render checkmark when checked");
    }
}
