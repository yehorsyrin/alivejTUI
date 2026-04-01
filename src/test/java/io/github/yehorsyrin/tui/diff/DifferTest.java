package io.github.yehorsyrin.tui.diff;

import io.github.yehorsyrin.tui.layout.LayoutEngine;
import io.github.yehorsyrin.tui.node.*;
import io.github.yehorsyrin.tui.style.Color;
import io.github.yehorsyrin.tui.style.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DifferTest {

    private Differ differ;
    private LayoutEngine layout;

    @BeforeEach
    void setUp() {
        differ = new Differ();
        layout = new LayoutEngine();
    }

    private void doLayout(io.github.yehorsyrin.tui.core.Node node) {
        layout.layout(node, 0, 0, 80, 24);
    }

    // --- First render (oldTree = null) ---

    @Test
    void firstRender_nullOld_allCellsAreChanges() {
        TextNode text = Text.of("Hi");
        doLayout(text);
        List<CellChange> changes = differ.diff(null, text);
        assertEquals(2, changes.size());
    }

    @Test
    void firstRender_charValuesCorrect() {
        TextNode text = Text.of("Hi");
        doLayout(text);
        List<CellChange> changes = differ.diff(null, text);
        Map<String, Character> byPos = changes.stream()
            .collect(Collectors.toMap(c -> c.col() + "," + c.row(), CellChange::character));
        assertEquals('H', byPos.get("0,0"));
        assertEquals('i', byPos.get("1,0"));
    }

    // --- Identical trees → no changes ---

    @Test
    void identicalTrees_noChanges() {
        TextNode a = Text.of("Hello");
        TextNode b = Text.of("Hello");
        doLayout(a);
        doLayout(b);
        assertEquals(0, differ.diff(a, b).size());
    }

    // --- One character changed ---

    @Test
    void oneCharChanged_oneChange() {
        TextNode a = Text.of("Hello");
        TextNode b = Text.of("Hellp");
        doLayout(a);
        doLayout(b);
        List<CellChange> changes = differ.diff(a, b);
        assertEquals(1, changes.size());
        CellChange c = changes.get(0);
        assertEquals(4, c.col());
        assertEquals(0, c.row());
        assertEquals('p', c.character());
    }

    // --- Node removed → cells erased ---

    @Test
    void nodeRemoved_cellsErasedWithSpace() {
        TextNode a = Text.of("Hi");
        TextNode b = Text.of("H");
        doLayout(a);
        doLayout(b);
        List<CellChange> changes = differ.diff(a, b);
        // 'i' at (1,0) should be replaced by space
        assertTrue(changes.stream().anyMatch(c -> c.col() == 1 && c.row() == 0 && c.character() == ' '));
    }

    // --- Style change ---

    @Test
    void styleChange_triggersChange() {
        TextNode a = Text.of("A");
        TextNode b = Text.of("A").bold();
        doLayout(a);
        doLayout(b);
        List<CellChange> changes = differ.diff(a, b);
        assertEquals(1, changes.size());
        assertTrue(changes.get(0).style().isBold());
    }

    // --- VBox with multiple children ---

    @Test
    void vbox_onlyChangedChildEmitsChange() {
        TextNode a1 = Text.of("Hello");
        TextNode a2 = Text.of("World");
        TextNode b1 = Text.of("Hello");
        TextNode b2 = Text.of("Wörld"); // changed

        VBoxNode treeA = VBox.of(a1, a2);
        VBoxNode treeB = VBox.of(b1, b2);
        doLayout(treeA);
        doLayout(treeB);

        List<CellChange> changes = differ.diff(treeA, treeB);
        // Only row 1 changes (ö vs o and subsequent bytes)
        assertTrue(changes.stream().allMatch(c -> c.row() == 1));
    }

    // --- Divider ---

    @Test
    void horizontalDivider_fillsEntireWidth() {
        DividerNode div = Divider.horizontal();
        doLayout(div);
        List<CellChange> changes = differ.diff(null, div);
        assertEquals(80, changes.size());
        assertTrue(changes.stream().allMatch(c -> c.character() == DividerNode.DEFAULT_HORIZONTAL_CHAR));
    }

    // --- ProgressBar ---

    @Test
    void progressBar_filledAndEmptyChars() {
        ProgressBarNode bar = new ProgressBarNode(0.5);
        layout.layout(bar, 0, 0, 10, 1);
        List<CellChange> changes = differ.diff(null, bar);
        assertEquals(10, changes.size());
        long filled = changes.stream().filter(c -> c.character() == ProgressBarNode.FILLED_CHAR).count();
        long empty  = changes.stream().filter(c -> c.character() == ProgressBarNode.EMPTY_CHAR).count();
        assertEquals(5, filled);
        assertEquals(5, empty);
    }

    // --- Button ---

    @Test
    void button_renderedWithBrackets() {
        ButtonNode btn = Button.of("OK", () -> {});
        doLayout(btn);
        List<CellChange> changes = differ.diff(null, btn);
        Map<String, Character> byPos = changes.stream()
            .collect(Collectors.toMap(c -> c.col() + "," + c.row(), CellChange::character));
        assertEquals('[', byPos.get("0,0"));
        assertEquals('O', byPos.get("1,0"));
        assertEquals('K', byPos.get("2,0"));
        assertEquals(']', byPos.get("3,0"));
    }

    // --- null newTree ---

    @Test
    void nullNewTree_allOldCellsErased() {
        TextNode a = Text.of("Hi");
        doLayout(a);
        List<CellChange> changes = differ.diff(a, null);
        assertEquals(2, changes.size());
        assertTrue(changes.stream().allMatch(c -> c.character() == ' '));
    }

    // --- Additional tests ---

    @Test
    void deepTree_changesDetected() {
        // VBox > HBox > Text
        TextNode textA = Text.of("A");
        HBoxNode hboxA = HBox.of(textA);
        VBoxNode treeA = VBox.of(hboxA);
        doLayout(treeA);

        TextNode textB = Text.of("B");
        HBoxNode hboxB = HBox.of(textB);
        VBoxNode treeB = VBox.of(hboxB);
        doLayout(treeB);

        List<CellChange> changes = differ.diff(treeA, treeB);
        // 'A' → 'B': exactly 1 changed cell
        assertEquals(1, changes.size());
        assertEquals('B', changes.get(0).character());
    }

    @Test
    void unicodeChar_rendered() {
        TextNode text = Text.of("α");
        doLayout(text);
        List<CellChange> changes = differ.diff(null, text);
        assertEquals(1, changes.size());
        assertEquals('α', changes.get(0).character());
    }

    @Test
    void wideTree_manyChanges() {
        // "hello" → "world": h≠w, e≠o, l≠r, l=l (same), o≠d → 4 changes
        TextNode prev = Text.of("hello");
        TextNode next = Text.of("world");
        doLayout(prev);
        doLayout(next);
        List<CellChange> changes = differ.diff(prev, next);
        assertEquals(4, changes.size());
    }
}
