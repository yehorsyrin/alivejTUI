package io.alive.tui.node;

import io.alive.tui.core.Node;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VBoxHBoxTest {

    @Test
    void vbox_containsChildren() {
        TextNode a = Text.of("A");
        TextNode b = Text.of("B");
        VBoxNode box = VBox.of(a, b);
        assertEquals(2, box.getChildren().size());
        assertSame(a, box.getChildren().get(0));
        assertSame(b, box.getChildren().get(1));
    }

    @Test
    void vbox_defaultGapIsZero() {
        assertEquals(0, VBox.of().getGap());
    }

    @Test
    void vbox_gapFluent() {
        assertEquals(2, VBox.of().gap(2).getGap());
    }

    @Test
    void vbox_nullChildrenIgnored() {
        VBoxNode box = VBox.of(Text.of("A"), null, Text.of("B"));
        assertEquals(2, box.getChildren().size());
    }

    @Test
    void hbox_containsChildren() {
        TextNode a = Text.of("A");
        TextNode b = Text.of("B");
        HBoxNode box = HBox.of(a, b);
        assertEquals(2, box.getChildren().size());
    }

    @Test
    void hbox_defaultGapIsZero() {
        assertEquals(0, HBox.of().getGap());
    }

    @Test
    void hbox_gapFluent() {
        assertEquals(3, HBox.of().gap(3).getGap());
    }
}
