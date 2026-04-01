package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.backend.TerminalBackend;
import io.github.yehorsyrin.tui.event.KeyEvent;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.layout.LayoutEngine;
import io.github.yehorsyrin.tui.render.Renderer;
import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RadioGroupNode} and its factory {@link RadioGroup}.
 *
 * @author Jarvis (AI)
 */
class RadioGroupNodeTest {

    // --- FakeBackend for render-level tests ---

    static class FakeBackend implements TerminalBackend {
        final List<String> putChars = new ArrayList<>();
        final List<Style> putStyles = new ArrayList<>();

        @Override public void init() {}
        @Override public void shutdown() {}
        @Override public int getWidth()  { return 80; }
        @Override public int getHeight() { return 24; }
        @Override public void putChar(int col, int row, char c, Style style) {
            putChars.add(col + "," + row + "=" + c);
            putStyles.add(style);
        }
        @Override public void flush() {}
        @Override public void hideCursor() {}
        @Override public void showCursor() {}
        @Override public void setCursor(int col, int row) {}
        @Override public void clear() { putChars.clear(); putStyles.clear(); }
        @Override public KeyEvent readKey() throws InterruptedException { return KeyEvent.of(KeyType.EOF); }
        @Override public void setResizeListener(Runnable r) {}
    }

    // --- Construction via factory (varargs) ---

    @Test
    void factoryVarargsCreatesNodeWithCorrectOptions() {
        RadioGroupNode rg = RadioGroup.of("Alpha", "Beta", "Gamma");
        assertNotNull(rg);
        assertEquals(List.of("Alpha", "Beta", "Gamma"), rg.getOptions());
    }

    @Test
    void factoryListCreatesNodeWithCorrectOptions() {
        List<String> opts = List.of("One", "Two");
        RadioGroupNode rg = RadioGroup.of(opts);
        assertEquals(opts, rg.getOptions());
    }

    @Test
    void defaultSelectedIndexIsZero() {
        RadioGroupNode rg = RadioGroup.of("A", "B", "C");
        assertEquals(0, rg.getSelectedIndex());
    }

    @Test
    void getOptionsReturnsAllOptions() {
        RadioGroupNode rg = RadioGroup.of("X", "Y", "Z");
        List<String> opts = rg.getOptions();
        assertEquals(3, opts.size());
        assertEquals("X", opts.get(0));
        assertEquals("Y", opts.get(1));
        assertEquals("Z", opts.get(2));
    }

    @Test
    void constructorWithNullOptionsResultsInEmptyList() {
        RadioGroupNode rg = new RadioGroupNode(null);
        assertNotNull(rg.getOptions());
        assertTrue(rg.getOptions().isEmpty());
    }

    // --- setSelectedIndex ---

    @Test
    void setSelectedIndexUpdatesValue() {
        RadioGroupNode rg = RadioGroup.of("A", "B", "C");
        rg.setSelectedIndex(2);
        assertEquals(2, rg.getSelectedIndex());
    }

    @Test
    void setSelectedIndexClampsToLastWhenTooHigh() {
        RadioGroupNode rg = RadioGroup.of("A", "B", "C");
        rg.setSelectedIndex(99);
        assertEquals(2, rg.getSelectedIndex());
    }

    @Test
    void setSelectedIndexClampsToZeroWhenNegative() {
        RadioGroupNode rg = RadioGroup.of("A", "B");
        rg.setSelectedIndex(-5);
        assertEquals(0, rg.getSelectedIndex());
    }

    @Test
    void setSelectedIndexOnEmptyListStaysZero() {
        RadioGroupNode rg = new RadioGroupNode(List.of());
        rg.setSelectedIndex(3);
        assertEquals(0, rg.getSelectedIndex());
    }

    // --- Default styles ---

    @Test
    void defaultNormalStyleIsDefault() {
        RadioGroupNode rg = RadioGroup.of("A");
        assertEquals(Style.DEFAULT, rg.getNormalStyle());
    }

    @Test
    void defaultFocusedStyleIsBold() {
        RadioGroupNode rg = RadioGroup.of("A");
        assertTrue(rg.getFocusedStyle().isBold());
    }

    // --- Focusable ---

    @Test
    void focusedDefaultIsFalse() {
        RadioGroupNode rg = RadioGroup.of("A");
        assertFalse(rg.isFocused());
    }

    @Test
    void setFocusedTrue() {
        RadioGroupNode rg = RadioGroup.of("A");
        rg.setFocused(true);
        assertTrue(rg.isFocused());
    }

    @Test
    void setFocusedFalse() {
        RadioGroupNode rg = RadioGroup.of("A");
        rg.setFocused(true);
        rg.setFocused(false);
        assertFalse(rg.isFocused());
    }

    @Test
    void getFocusIdWithKey() {
        RadioGroupNode rg = RadioGroup.of("A");
        rg.setKey("my-radio");
        assertEquals("my-radio", rg.getFocusId());
    }

    @Test
    void getFocusIdWithoutKeyReturnsDefault() {
        RadioGroupNode rg = RadioGroup.of("A");
        assertEquals("radio-group", rg.getFocusId());
    }

    // --- Layout ---

    @Test
    void layoutAssignsFullAvailableWidth() {
        RadioGroupNode rg = RadioGroup.of("Option A", "Option B");
        LayoutEngine le = new LayoutEngine();
        le.layout(rg, 0, 0, 40, 24);
        assertEquals(40, rg.getWidth());
    }

    @Test
    void layoutHeightEqualsOptionCount() {
        RadioGroupNode rg = RadioGroup.of("A", "B", "C");
        LayoutEngine le = new LayoutEngine();
        le.layout(rg, 0, 0, 80, 24);
        assertEquals(3, rg.getHeight());
    }

    @Test
    void layoutHeightIsAtLeastOneForEmptyOptions() {
        RadioGroupNode rg = new RadioGroupNode(List.of());
        LayoutEngine le = new LayoutEngine();
        le.layout(rg, 0, 0, 80, 24);
        assertEquals(1, rg.getHeight());
    }

    @Test
    void layoutSetsXAndY() {
        RadioGroupNode rg = RadioGroup.of("A");
        LayoutEngine le = new LayoutEngine();
        le.layout(rg, 5, 10, 80, 24);
        assertEquals(5, rg.getX());
        assertEquals(10, rg.getY());
    }

    // --- Rendering: selected bullet ---

    @Test
    void renderShowsSelectedBulletForFirstOption() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        RadioGroupNode rg = RadioGroup.of("Alpha", "Beta");
        renderer.render(rg);
        // Row 0: "(●) Alpha" — col 1 should be '●'
        assertTrue(backend.putChars.contains("1,0=" + RadioGroupNode.SELECTED_BULLET),
                "selected row should show '●'");
    }

    @Test
    void renderShowsUnselectedSpaceForOtherOptions() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        RadioGroupNode rg = RadioGroup.of("Alpha", "Beta");
        renderer.render(rg);
        // Row 1: "( ) Beta" — col 1 should be ' '
        assertTrue(backend.putChars.contains("1,1=" + RadioGroupNode.UNSELECTED_BULLET),
                "unselected row should show space bullet");
    }

    @Test
    void renderSelectedBulletMovesWithSelectedIndex() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        RadioGroupNode rg = RadioGroup.of("A", "B", "C");
        rg.setSelectedIndex(2);
        renderer.render(rg);
        // Row 0 and 1 are unselected
        assertTrue(backend.putChars.contains("1,0=" + RadioGroupNode.UNSELECTED_BULLET));
        assertTrue(backend.putChars.contains("1,1=" + RadioGroupNode.UNSELECTED_BULLET));
        // Row 2 is selected
        assertTrue(backend.putChars.contains("1,2=" + RadioGroupNode.SELECTED_BULLET));
    }

    @Test
    void renderPrefixCharacters() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        RadioGroupNode rg = RadioGroup.of("Hi", "Lo");
        renderer.render(rg);
        // Row 0: "(●) Hi" → col 0='(', col 1='●', col 2=')', col 3=' ', col 4='H', col 5='i'
        assertTrue(backend.putChars.contains("0,0=("));
        assertTrue(backend.putChars.contains("2,0=)"));
        assertTrue(backend.putChars.contains("3,0= "));
        assertTrue(backend.putChars.contains("4,0=H"));
        assertTrue(backend.putChars.contains("5,0=i"));
        // Row 1: "( ) Lo"
        assertTrue(backend.putChars.contains("0,1=("));
        assertTrue(backend.putChars.contains("2,1=)"));
    }

    // --- Rendering: focused style ---

    @Test
    void renderSelectedRowUsesFocusedStyleWhenFocused() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        Style boldStyle = Style.DEFAULT.withBold(true);
        RadioGroupNode rg = RadioGroup.of("A", "B");
        rg.focusedStyle(boldStyle);
        rg.setFocused(true);
        renderer.render(rg);
        // The selected row (row 0) characters should use boldStyle.
        // We verify by checking the putStyles list for the '(' char at col=0, row=0.
        boolean foundBoldOnRow0 = false;
        for (int i = 0; i < backend.putChars.size(); i++) {
            if (backend.putChars.get(i).equals("0,0=(")) {
                foundBoldOnRow0 = backend.putStyles.get(i).isBold();
                break;
            }
        }
        assertTrue(foundBoldOnRow0, "focused selected row should use bold focusedStyle");
    }

    @Test
    void renderUnfocusedUsesNormalStyleForAllRows() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        Style customNormal = Style.DEFAULT;
        RadioGroupNode rg = RadioGroup.of("A", "B");
        rg.normalStyle(customNormal);
        rg.setFocused(false);
        renderer.render(rg);
        // All chars should use normalStyle regardless of selectedIndex
        for (Style s : backend.putStyles) {
            assertEquals(customNormal, s);
        }
    }

    @Test
    void renderUnfocusedSelectedRowUsesNormalStyle() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);
        RadioGroupNode rg = RadioGroup.of("A", "B");
        rg.setFocused(false);
        renderer.render(rg);
        // Row 0 is selected but not focused — should use normalStyle (non-bold DEFAULT)
        for (int i = 0; i < backend.putChars.size(); i++) {
            if (backend.putChars.get(i).contains(",0=")) {
                assertFalse(backend.putStyles.get(i).isBold(),
                        "unfocused selected row should not use bold");
            }
        }
    }

    // --- selectedIndex change updates rendering ---

    @Test
    void changingSelectedIndexUpdatesRendering() {
        FakeBackend backend = new FakeBackend();
        Renderer renderer = new Renderer(backend);

        // Initially index 0 is selected
        renderer.render(RadioGroup.of("A", "B", "C"));
        assertTrue(backend.putChars.contains("1,0=" + RadioGroupNode.SELECTED_BULLET));

        // Re-render with a new node instance so the differ can detect the change.
        // (The Renderer stores the previous tree by reference; in-place mutation of the same
        // node object would produce an identical flatten result for both old and new.)
        backend.clear();
        RadioGroupNode rg2 = RadioGroup.of("A", "B", "C");
        rg2.setSelectedIndex(1);
        renderer.render(rg2);
        assertTrue(backend.putChars.contains("1,0=" + RadioGroupNode.UNSELECTED_BULLET),
                "previously selected row should now be unselected");
        assertTrue(backend.putChars.contains("1,1=" + RadioGroupNode.SELECTED_BULLET),
                "new selected row should show selected bullet");
    }

    // --- Fluent setters ---

    @Test
    void normalStyleFluentSetterWorks() {
        Style custom = Style.DEFAULT.withBold(true);
        RadioGroupNode rg = RadioGroup.of("A").normalStyle(custom);
        assertEquals(custom, rg.getNormalStyle());
    }

    @Test
    void focusedStyleFluentSetterWorks() {
        Style custom = Style.DEFAULT.withBold(true);
        RadioGroupNode rg = RadioGroup.of("A").focusedStyle(custom);
        assertEquals(custom, rg.getFocusedStyle());
    }
}
