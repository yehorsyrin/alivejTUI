package io.github.yehorsyrin.tui.layout;

import io.github.yehorsyrin.tui.node.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LayoutEngineTest {

    private LayoutEngine engine;

    @BeforeEach
    void setUp() { engine = new LayoutEngine(); }

    // --- TextNode ---

    @Test
    void textNode_widthEqualsLength() {
        TextNode t = Text.of("Hello");
        engine.layout(t, 0, 0, 80, 24);
        assertEquals(5, t.getWidth());
        assertEquals(1, t.getHeight());
        assertEquals(0, t.getX());
        assertEquals(0, t.getY());
    }

    @Test
    void textNode_widthClampedToAvailable() {
        TextNode t = Text.of("Hello World");
        engine.layout(t, 0, 0, 5, 24);
        assertEquals(5, t.getWidth());
    }

    // --- VBoxNode ---

    @Test
    void vbox_childrenStackedVertically() {
        TextNode a = Text.of("A");
        TextNode b = Text.of("B");
        TextNode c = Text.of("C");
        VBoxNode box = VBox.of(a, b, c);
        engine.layout(box, 0, 0, 80, 24);

        assertEquals(0, a.getY());
        assertEquals(1, b.getY());
        assertEquals(2, c.getY());
        assertEquals(3, box.getHeight());
    }

    @Test
    void vbox_gapApplied() {
        TextNode a = Text.of("A");
        TextNode b = Text.of("B");
        VBoxNode box = VBox.of(a, b).gap(2);
        engine.layout(box, 0, 0, 80, 24);

        assertEquals(0, a.getY());
        assertEquals(3, b.getY()); // 1 (height of A) + 2 (gap)
        assertEquals(4, box.getHeight()); // 1 + 2 + 1
    }

    @Test
    void vbox_childrenGetFullWidth() {
        TextNode a = Text.of("Hi");
        VBoxNode box = VBox.of(a);
        engine.layout(box, 0, 0, 80, 24);
        assertEquals(80, box.getWidth());
        assertEquals(2, a.getWidth());
    }

    @Test
    void vbox_xOffsetPropagated() {
        TextNode a = Text.of("A");
        VBoxNode box = VBox.of(a);
        engine.layout(box, 10, 5, 70, 24);
        assertEquals(10, a.getX());
        assertEquals(5, a.getY());
    }

    // --- HBoxNode ---

    @Test
    void hbox_childrenSideBySide() {
        TextNode a = Text.of("A");
        TextNode b = Text.of("B");
        HBoxNode box = HBox.of(a, b);
        engine.layout(box, 0, 0, 80, 24);

        assertEquals(0,  a.getX());
        assertEquals(40, b.getX());
        assertEquals(80, box.getWidth());
    }

    @Test
    void hbox_gapReducesChildWidth() {
        TextNode a = Text.of("A");
        TextNode b = Text.of("B");
        HBoxNode box = HBox.of(a, b).gap(2);
        engine.layout(box, 0, 0, 80, 24);

        // usable = 80 - 2 = 78, each child = 39
        assertEquals(0,  a.getX());
        assertEquals(41, b.getX()); // 39 + 2 (gap)
    }

    @Test
    void hbox_emptyHasZeroSize() {
        HBoxNode box = HBox.of();
        engine.layout(box, 0, 0, 80, 24);
        assertEquals(0, box.getWidth());
        assertEquals(0, box.getHeight());
    }

    // --- BoxNode with border ---

    @Test
    void boxWithBorder_innerContentOffset() {
        TextNode inner = Text.of("Hi");
        BoxNode box = new BoxNode(inner, true, null);
        engine.layout(box, 0, 0, 80, 24);
        assertEquals(1, inner.getX()); // border offset
        assertEquals(1, inner.getY());
        assertEquals(3, box.getHeight()); // 1 (top) + 1 (content) + 1 (bottom)
    }

    @Test
    void boxWithoutBorder_innerContentNotOffset() {
        TextNode inner = Text.of("Hi");
        BoxNode box = new BoxNode(inner, false, null);
        engine.layout(box, 0, 0, 80, 24);
        assertEquals(0, inner.getX());
        assertEquals(0, inner.getY());
    }

    // --- ButtonNode ---

    @Test
    void button_widthIsLabelPlusBrackets() {
        ButtonNode btn = Button.of("OK", () -> {});
        engine.layout(btn, 0, 0, 80, 24);
        assertEquals(4, btn.getWidth()); // "[OK]" = 4
        assertEquals(1, btn.getHeight());
    }

    // --- InputNode ---

    @Test
    void input_fillsAvailableWidth() {
        InputNode input = Input.of("", v -> {});
        engine.layout(input, 0, 0, 60, 24);
        assertEquals(60, input.getWidth());
        assertEquals(1, input.getHeight());
    }

    // --- ProgressBarNode ---

    @Test
    void progressBar_fillsWidth() {
        ProgressBarNode bar = new ProgressBarNode(0.5);
        engine.layout(bar, 0, 0, 50, 24);
        assertEquals(50, bar.getWidth());
        assertEquals(1, bar.getHeight());
    }

    // --- SpinnerNode ---

    @Test
    void spinner_isOneByOne() {
        SpinnerNode spinner = Spinner.of();
        engine.layout(spinner, 0, 0, 80, 24);
        assertEquals(1, spinner.getWidth());
        assertEquals(1, spinner.getHeight());
    }

    // --- DividerNode ---

    @Test
    void horizontalDivider_fillsWidth() {
        DividerNode div = Divider.horizontal();
        engine.layout(div, 0, 0, 80, 24);
        assertEquals(80, div.getWidth());
        assertEquals(1, div.getHeight());
    }

    @Test
    void verticalDivider_fillsHeight() {
        DividerNode div = Divider.vertical();
        engine.layout(div, 0, 0, 80, 24);
        assertEquals(1, div.getWidth());
        assertEquals(24, div.getHeight());
    }

    // --- Edge cases ---

    @Test
    void layout_nullNodeDoesNotThrow() {
        assertDoesNotThrow(() -> engine.layout(null, 0, 0, 80, 24));
    }

    @Test
    void layout_zeroAvailableSize() {
        TextNode t = Text.of("Hello");
        assertDoesNotThrow(() -> engine.layout(t, 0, 0, 0, 0));
        assertEquals(0, t.getWidth());
        assertEquals(1, t.getHeight());
    }

    // --- Additional edge-case tests ---

    @Test
    void veryNarrowConsole_textTruncated() {
        TextNode t = Text.of("hello");
        engine.layout(t, 0, 0, 3, 24);
        assertEquals(3, t.getWidth());
    }

    @Test
    void veryNarrowConsole_buttonFits() {
        ButtonNode btn = Button.of("A", () -> {});
        engine.layout(btn, 0, 0, 3, 24);
        // "[A]" = 3 chars, must clamp to available width = 3
        assertEquals(3, btn.getWidth());
    }

    @Test
    void zeroWidthContainer_noException() {
        TextNode t = Text.of("test");
        assertDoesNotThrow(() -> engine.layout(t, 0, 0, 0, 24));
    }

    @Test
    void zeroHeightContainer_noException() {
        TextNode t = Text.of("test");
        assertDoesNotThrow(() -> engine.layout(t, 0, 0, 80, 0));
    }

    @Test
    void nodeWiderThanScreen_clampedToWidth() {
        TextNode t = Text.of("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        engine.layout(t, 0, 0, 5, 24);
        assertEquals(5, t.getWidth());
    }
}
