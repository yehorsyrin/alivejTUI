package io.alive.tui.style;

import io.alive.tui.node.ButtonNode;
import io.alive.tui.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Selector}.
 *
 * @author Jarvis (AI)
 */
class SelectorMatchTest {

    @Test
    void byId_matchesCorrectId() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        node.withId("myId");
        assertTrue(Selector.byId("myId").matches(node));
    }

    @Test
    void byId_noMatchDifferentId() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        node.withId("otherId");
        assertFalse(Selector.byId("myId").matches(node));
    }

    @Test
    void byId_noMatchNullId() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        assertFalse(Selector.byId("myId").matches(node));
    }

    @Test
    void byClass_matchesClassName() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        node.withClassName("primary");
        assertTrue(Selector.byClass("primary").matches(node));
    }

    @Test
    void byClass_noMatchDifferentClass() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        node.withClassName("secondary");
        assertFalse(Selector.byClass("primary").matches(node));
    }

    @Test
    void byType_matchesExactType() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        assertTrue(Selector.byType(TextNode.class).matches(node));
    }

    @Test
    void byType_noMatchDifferentType() {
        ButtonNode btn = new ButtonNode("Click", () -> {});
        assertFalse(Selector.byType(TextNode.class).matches(btn));
    }

    @Test
    void byType_matchesSubclass() {
        // TextNode extends Node — byType(Node.class) should also match
        TextNode node = new TextNode("x", Style.DEFAULT);
        assertTrue(Selector.byType(io.alive.tui.core.Node.class).matches(node));
    }

    @Test
    void and_bothMatch() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        node.withClassName("label");
        Selector s = Selector.byType(TextNode.class).and(Selector.byClass("label"));
        assertTrue(s.matches(node));
    }

    @Test
    void and_onlyOneFails() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        node.withClassName("other");
        Selector s = Selector.byType(TextNode.class).and(Selector.byClass("label"));
        assertFalse(s.matches(node));
    }

    @Test
    void or_firstMatches() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        node.withId("foo");
        Selector s = Selector.byId("foo").or(Selector.byClass("bar"));
        assertTrue(s.matches(node));
    }

    @Test
    void or_secondMatches() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        node.withClassName("bar");
        Selector s = Selector.byId("foo").or(Selector.byClass("bar"));
        assertTrue(s.matches(node));
    }

    @Test
    void or_noneMatch() {
        TextNode node = new TextNode("x", Style.DEFAULT);
        Selector s = Selector.byId("foo").or(Selector.byClass("bar"));
        assertFalse(s.matches(node));
    }

    @Test
    void matches_nullNode_false() {
        assertFalse(Selector.byId("x").matches(null));
    }

    @Test
    void toString_containsDescription() {
        String s = Selector.byId("foo").toString();
        assertTrue(s.contains("foo"));
    }

    @Test
    void byId_nullArg_throws() {
        assertThrows(NullPointerException.class, () -> Selector.byId(null));
    }

    @Test
    void byClass_nullArg_throws() {
        assertThrows(NullPointerException.class, () -> Selector.byClass(null));
    }

    @Test
    void byType_nullArg_throws() {
        assertThrows(NullPointerException.class, () -> Selector.byType(null));
    }
}
