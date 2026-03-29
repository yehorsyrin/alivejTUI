package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.diff.CellChange;
import io.github.yehorsyrin.tui.diff.Differ;
import io.github.yehorsyrin.tui.event.EventBus;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.layout.LayoutEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HelpPanelNode}, {@link HelpPanel} factory, and
 * {@link EventBus#getRegisteredKeys()}.
 *
 * @author Jarvis (AI)
 */
class HelpPanelNodeTest {

    // ------------------------------------------------------------------ construction

    @Test
    void constructionWithBindings() {
        KeyBinding kb = new KeyBinding("ESC", "quit");
        HelpPanelNode node = new HelpPanelNode(List.of(kb));
        assertEquals(1, node.getBindings().size());
        assertEquals(kb, node.getBindings().get(0));
    }

    @Test
    void constructionWithNullListYieldsEmptyBindings() {
        HelpPanelNode node = new HelpPanelNode(null);
        assertTrue(node.getBindings().isEmpty());
    }

    @Test
    void bindingsListIsUnmodifiable() {
        HelpPanelNode node = HelpPanel.of(new KeyBinding("↑", "up"));
        assertThrows(UnsupportedOperationException.class, () -> node.getBindings().clear());
    }

    @Test
    void keyBindingRecordRejectsNullKey() {
        assertThrows(IllegalArgumentException.class, () -> new KeyBinding(null, "desc"));
    }

    @Test
    void keyBindingRecordRejectsNullDescription() {
        assertThrows(IllegalArgumentException.class, () -> new KeyBinding("ESC", null));
    }

    // ------------------------------------------------------------------ formatting

    @Test
    void formatProducesExpectedString() {
        KeyBinding kb = new KeyBinding("ENTER", "confirm");
        assertEquals("[ENTER]  confirm", HelpPanelNode.format(kb));
    }

    @Test
    void maxLineLengthIsLongestFormattedLine() {
        HelpPanelNode node = HelpPanel.of(
            new KeyBinding("↑", "up"),
            new KeyBinding("ESC", "quit application")
        );
        int expected = HelpPanelNode.format(new KeyBinding("ESC", "quit application")).length();
        assertEquals(expected, node.maxLineLength());
    }

    @Test
    void maxLineLengthIsZeroWhenEmpty() {
        HelpPanelNode node = HelpPanel.of();
        assertEquals(0, node.maxLineLength());
    }

    // ------------------------------------------------------------------ layout

    @Test
    void layoutSetsCorrectDimensions() {
        HelpPanelNode node = HelpPanel.of(
            new KeyBinding("↑", "up"),
            new KeyBinding("↓", "down"),
            new KeyBinding("ESC", "quit")
        );
        new LayoutEngine().layout(node, 0, 0, 80, 24);
        assertEquals(3, node.getHeight());
        int expectedWidth = node.maxLineLength(); // all lines fit in 80 cols
        assertEquals(expectedWidth, node.getWidth());
    }

    @Test
    void layoutCapsWidthAtAvailableWidth() {
        HelpPanelNode node = HelpPanel.of(new KeyBinding("ENTER", "confirm action now"));
        new LayoutEngine().layout(node, 0, 0, 5, 24);
        assertEquals(5, node.getWidth());
    }

    @Test
    void layoutHeightIsOneWhenNoBindings() {
        HelpPanelNode node = HelpPanel.of();
        new LayoutEngine().layout(node, 0, 0, 80, 24);
        assertEquals(1, node.getHeight());
    }

    // ------------------------------------------------------------------ rendering

    @Test
    void renderingProducesCorrectCellsForSingleBinding() {
        HelpPanelNode node = HelpPanel.of(new KeyBinding("Q", "quit"));
        new LayoutEngine().layout(node, 2, 3, 80, 24);

        List<CellChange> changes = new Differ().diff(null, node);
        // Build a lookup map: "col,row" → char
        Map<String, Character> cellMap = changes.stream()
            .collect(Collectors.toMap(c -> c.col() + "," + c.row(), CellChange::character));

        String expected = "[Q]  quit";
        for (int i = 0; i < expected.length(); i++) {
            String cellKey = (2 + i) + ",3";
            assertTrue(cellMap.containsKey(cellKey), "Missing cell at " + cellKey);
            assertEquals(expected.charAt(i), cellMap.get(cellKey), "Wrong char at col " + i);
        }
    }

    @Test
    void renderingMultipleBindingsAtCorrectRows() {
        HelpPanelNode node = HelpPanel.of(
            new KeyBinding("↑", "up"),
            new KeyBinding("↓", "down")
        );
        new LayoutEngine().layout(node, 0, 0, 80, 24);

        List<CellChange> changes = new Differ().diff(null, node);
        Map<String, Character> cellMap = changes.stream()
            .collect(Collectors.toMap(c -> c.col() + "," + c.row(), CellChange::character));

        // First line at y=0 starts with '['
        assertEquals('[', cellMap.get("0,0"));
        // Second line at y=1 starts with '['
        assertEquals('[', cellMap.get("0,1"));
    }

    // ------------------------------------------------------------------ HelpPanel factory

    @Test
    void factoryVarargs() {
        HelpPanelNode node = HelpPanel.of(
            new KeyBinding("A", "action a"),
            new KeyBinding("B", "action b")
        );
        assertEquals(2, node.getBindings().size());
    }

    @Test
    void factoryList() {
        List<KeyBinding> list = List.of(new KeyBinding("X", "exit"));
        HelpPanelNode node = HelpPanel.of(list);
        assertEquals(1, node.getBindings().size());
    }

    @Test
    void factoryForBusIncludesOnlyMappedRegisteredKeys() {
        EventBus bus = new EventBus();
        bus.register(KeyType.ESCAPE, () -> {});
        bus.register(KeyType.ENTER, () -> {});

        HelpPanelNode node = HelpPanel.forBus(bus,
            Map.of(KeyType.ESCAPE, "quit", KeyType.ARROW_UP, "move up"));

        // ESCAPE is registered AND in descriptions → included
        // ENTER is registered but NOT in descriptions → excluded
        // ARROW_UP is in descriptions but NOT registered → excluded
        assertEquals(1, node.getBindings().size());
        assertEquals("ESCAPE", node.getBindings().get(0).key());
        assertEquals("quit",   node.getBindings().get(0).description());
    }

    @Test
    void factoryForBusWithNullBusReturnsEmpty() {
        HelpPanelNode node = HelpPanel.forBus(null, Map.of(KeyType.ESCAPE, "quit"));
        assertTrue(node.getBindings().isEmpty());
    }

    // ------------------------------------------------------------------ EventBus.getRegisteredKeys

    @Test
    void getRegisteredKeysReturnsRegisteredKeys() {
        EventBus bus = new EventBus();
        bus.register(KeyType.ESCAPE, () -> {});
        bus.register(KeyType.ENTER,  () -> {});

        Set<KeyType> keys = bus.getRegisteredKeys();
        assertTrue(keys.contains(KeyType.ESCAPE));
        assertTrue(keys.contains(KeyType.ENTER));
        assertEquals(2, keys.size());
    }

    @Test
    void getRegisteredKeysIsEmptyWhenNoHandlers() {
        EventBus bus = new EventBus();
        assertTrue(bus.getRegisteredKeys().isEmpty());
    }

    @Test
    void getRegisteredKeysIsUnmodifiable() {
        EventBus bus = new EventBus();
        bus.register(KeyType.ESCAPE, () -> {});
        assertThrows(UnsupportedOperationException.class,
            () -> bus.getRegisteredKeys().clear());
    }

    @Test
    void getRegisteredKeysExcludesUnregisteredKeys() {
        EventBus bus = new EventBus();
        bus.register(KeyType.ESCAPE, () -> {});
        Runnable h = () -> {};
        bus.register(KeyType.ENTER, h);
        bus.unregister(KeyType.ENTER, h);

        Set<KeyType> keys = bus.getRegisteredKeys();
        assertTrue(keys.contains(KeyType.ESCAPE));
        assertFalse(keys.contains(KeyType.ENTER));
    }

    @Test
    void getRegisteredKeysAfterClearIsEmpty() {
        EventBus bus = new EventBus();
        bus.register(KeyType.ESCAPE, () -> {});
        bus.clear();
        assertTrue(bus.getRegisteredKeys().isEmpty());
    }
}
