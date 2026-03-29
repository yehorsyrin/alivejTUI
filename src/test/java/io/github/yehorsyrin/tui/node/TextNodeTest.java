package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.style.Color;
import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextNodeTest {

    @Test
    void defaultStyleApplied() {
        TextNode node = Text.of("Hello");
        assertEquals("Hello", node.getText());
        assertEquals(Style.DEFAULT, node.getStyle());
    }

    @Test
    void nullTextBecomesEmpty() {
        TextNode node = new TextNode(null, null);
        assertEquals("", node.getText());
        assertEquals(Style.DEFAULT, node.getStyle());
    }

    @Test
    void fluentColorSetsForeground() {
        TextNode node = Text.of("Hi").color(Color.RED);
        assertEquals(Color.RED, node.getStyle().getForeground());
    }

    @Test
    void fluentBold() {
        TextNode node = Text.of("Hi").bold();
        assertTrue(node.getStyle().isBold());
    }

    @Test
    void fluentItalic() {
        assertTrue(Text.of("Hi").italic().getStyle().isItalic());
    }

    @Test
    void fluentUnderline() {
        assertTrue(Text.of("Hi").underline().getStyle().isUnderline());
    }

    @Test
    void fluentDim() {
        assertTrue(Text.of("Hi").dim().getStyle().isDim());
    }

    @Test
    void fluentStrikethrough() {
        assertTrue(Text.of("Hi").strikethrough().getStyle().isStrikethrough());
    }

    @Test
    void fluentBackground() {
        TextNode node = Text.of("Hi").background(Color.BLUE);
        assertEquals(Color.BLUE, node.getStyle().getBackground());
    }

    @Test
    void chaining_doesNotMutateIndependentInstances() {
        TextNode a = Text.of("A").bold();
        TextNode b = Text.of("B").color(Color.GREEN);
        assertFalse(a.getStyle().getForeground() != null);
        assertNull(b.getStyle().getForeground() == Color.GREEN ? null : b.getStyle().getForeground());
        assertFalse(b.getStyle().isBold());
    }

    @Test
    void hasNoChildrenByDefault() {
        assertTrue(Text.of("x").getChildren().isEmpty());
    }
}
