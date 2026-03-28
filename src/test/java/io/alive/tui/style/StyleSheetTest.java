package io.alive.tui.style;

import io.alive.tui.node.TextNode;
import io.alive.tui.node.VBoxNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StyleSheet}.
 *
 * @author Jarvis (AI)
 */
class StyleSheetTest {

    @Test
    void ruleCount_empty() {
        assertEquals(0, new StyleSheet().ruleCount());
    }

    @Test
    void add_returnsThis_forChaining() {
        StyleSheet sheet = new StyleSheet();
        assertSame(sheet, sheet.add(Selector.byId("x"), Style.DEFAULT));
    }

    @Test
    void add_null_selector_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new StyleSheet().add(null, Style.DEFAULT));
    }

    @Test
    void add_null_style_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new StyleSheet().add(Selector.byId("x"), null));
    }

    @Test
    void resolve_noMatch_returnsDefault() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        StyleSheet sheet = new StyleSheet()
                .add(Selector.byId("other"), Style.DEFAULT.withBold(true));
        assertEquals(Style.DEFAULT, sheet.resolve(node));
    }

    @Test
    void resolve_singleMatch_returnsStyle() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        node.withClassName("primary");
        Style bold = Style.DEFAULT.withBold(true);
        StyleSheet sheet = new StyleSheet().add(Selector.byClass("primary"), bold);
        assertEquals(bold, sheet.resolve(node));
    }

    @Test
    void resolve_multipleMatches_mergedLaterWins() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        node.withClassName("highlight").withId("title");

        Style boldStyle  = Style.DEFAULT.withBold(true);
        Style cyanStyle  = Style.DEFAULT.withForeground(Color.CYAN);

        StyleSheet sheet = new StyleSheet()
                .add(Selector.byClass("highlight"), boldStyle)
                .add(Selector.byId("title"),        cyanStyle);

        Style resolved = sheet.resolve(node);
        assertTrue(resolved.isBold());
        assertEquals(Color.CYAN, resolved.getForeground());
    }

    @Test
    void resolve_null_node_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new StyleSheet().resolve(null));
    }

    @Test
    void applyToTree_setsComputedStyleOnMatchingNodes() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        node.withId("special");
        Style bold = Style.DEFAULT.withBold(true);

        new StyleSheet().add(Selector.byId("special"), bold).applyToTree(node);

        assertEquals(bold, node.getComputedStyle());
    }

    @Test
    void applyToTree_noMatch_computedStyleRemainsNull() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        new StyleSheet().add(Selector.byId("other"), Style.DEFAULT.withBold(true)).applyToTree(node);
        assertNull(node.getComputedStyle());
    }

    @Test
    void applyToTree_null_doesNothing() {
        // Should not throw
        new StyleSheet().applyToTree(null);
    }

    @Test
    void applyToTree_recursivelyStylesChildren() {
        TextNode child = new TextNode("child", Style.DEFAULT);
        child.withClassName("highlight");
        VBoxNode parent = new VBoxNode(0);
        parent.addChild(child);

        Style italicStyle = Style.DEFAULT.withItalic(true);
        new StyleSheet().add(Selector.byClass("highlight"), italicStyle).applyToTree(parent);

        assertNull(parent.getComputedStyle());          // parent has no class
        assertEquals(italicStyle, child.getComputedStyle());  // child matched
    }

    @Test
    void withId_withClassName_fluentReturnsSameNode() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        assertSame(node, node.withId("myId").withClassName("myClass"));
        assertEquals("myId",    node.getId());
        assertEquals("myClass", node.getClassName());
    }

    @Test
    void node_getComputedStyle_initiallyNull() {
        assertNull(new TextNode("x", Style.DEFAULT).getComputedStyle());
    }
}
