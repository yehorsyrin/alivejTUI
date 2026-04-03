package io.github.yehorsyrin.tui.node;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ListNode}.
 *
 * @author Jarvis (AI)
 */
class ListNodeTest {

    // ── Construction ──────────────────────────────────────────────────────────

    @Test
    void constructor_withItems_storesDefensiveCopy() {
        List<String> items = new java.util.ArrayList<>(List.of("A", "B", "C"));
        ListNode node = new ListNode(items);
        items.add("D");
        assertEquals(3, node.getItems().size());
    }

    @Test
    void constructor_withNull_createsEmptyList() {
        ListNode node = new ListNode(null);
        assertNotNull(node.getItems());
        assertTrue(node.getItems().isEmpty());
    }

    @Test
    void constructor_defaultSelectedIndex_isZero() {
        ListNode node = new ListNode(List.of("X"));
        assertEquals(0, node.getSelectedIndex());
    }

    @Test
    void constructor_defaultScrollOffset_isZero() {
        ListNode node = new ListNode(List.of("X"));
        assertEquals(0, node.getScrollOffset());
    }

    @Test
    void constructor_defaultStyles_nonNull() {
        ListNode node = new ListNode(List.of("X"));
        assertNotNull(node.getSelectedStyle());
        assertNotNull(node.getNormalStyle());
    }

    // ── setSelectedIndex ──────────────────────────────────────────────────────

    @Test
    void setSelectedIndex_validIndex_updates() {
        ListNode node = new ListNode(List.of("A", "B", "C"));
        node.setSelectedIndex(2);
        assertEquals(2, node.getSelectedIndex());
    }

    @Test
    void setSelectedIndex_negativeIndex_clampsToZero() {
        ListNode node = new ListNode(List.of("A", "B"));
        node.setSelectedIndex(-5);
        assertEquals(0, node.getSelectedIndex());
    }

    @Test
    void setSelectedIndex_beyondEnd_clampsToLastIndex() {
        ListNode node = new ListNode(List.of("A", "B", "C"));
        node.setSelectedIndex(99);
        assertEquals(2, node.getSelectedIndex());
    }

    @Test
    void setSelectedIndex_onEmptyList_doesNothing() {
        ListNode node = new ListNode(null);
        assertDoesNotThrow(() -> node.setSelectedIndex(5));
        assertEquals(0, node.getSelectedIndex());
    }

    // ── adjustScroll (via setSelectedIndex with height set) ───────────────────

    @Test
    void setSelectedIndex_scrollsDownWhenBeyondViewport() {
        ListNode node = new ListNode(List.of("A", "B", "C", "D", "E"));
        node.setHeight(3); // viewport shows 3 rows
        node.setSelectedIndex(4); // row 4 is outside viewport [0..2]
        // scrollOffset should be 4 - 3 + 1 = 2
        assertEquals(2, node.getScrollOffset());
    }

    @Test
    void setSelectedIndex_scrollsUpWhenAboveViewport() {
        ListNode node = new ListNode(List.of("A", "B", "C", "D", "E"));
        node.setHeight(3);
        node.setSelectedIndex(4); // scroll to bottom
        node.setSelectedIndex(0); // scroll back to top
        assertEquals(0, node.getScrollOffset());
    }

    @Test
    void setSelectedIndex_heightZero_doesNotAdjustScroll() {
        ListNode node = new ListNode(List.of("A", "B", "C", "D", "E"));
        // height defaults to 0 — adjustScroll should be a no-op
        node.setSelectedIndex(4);
        assertEquals(0, node.getScrollOffset());
    }

    // ── setScrollOffset ───────────────────────────────────────────────────────

    @Test
    void setScrollOffset_positiveValue_applies() {
        ListNode node = new ListNode(List.of("A", "B", "C"));
        node.setScrollOffset(2);
        assertEquals(2, node.getScrollOffset());
    }

    @Test
    void setScrollOffset_negative_clampsToZero() {
        ListNode node = new ListNode(List.of("A", "B", "C"));
        node.setScrollOffset(-3);
        assertEquals(0, node.getScrollOffset());
    }

    // ── Style fluent API ──────────────────────────────────────────────────────

    @Test
    void selectedStyle_fluentSetterReturnsNode() {
        ListNode node = new ListNode(List.of("A"));
        io.github.yehorsyrin.tui.style.Style style =
                io.github.yehorsyrin.tui.style.Style.DEFAULT.withBold(true);
        assertSame(node, node.selectedStyle(style));
        assertEquals(style, node.getSelectedStyle());
    }

    @Test
    void normalStyle_fluentSetterReturnsNode() {
        ListNode node = new ListNode(List.of("A"));
        io.github.yehorsyrin.tui.style.Style style =
                io.github.yehorsyrin.tui.style.Style.DEFAULT.withDim(true);
        assertSame(node, node.normalStyle(style));
        assertEquals(style, node.getNormalStyle());
    }
}
