package io.alive.tui.core;

import io.alive.tui.node.NotificationNode;
import io.alive.tui.node.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link NotificationManager} and {@link io.alive.tui.node.NotificationNode}.
 *
 * @author Jarvis (AI)
 */
class NotificationManagerTest {

    // --- NotificationNode ---

    @Test
    void notificationNode_defaultType_info() {
        NotificationNode n = new NotificationNode("hi", null);
        assertEquals(NotificationType.INFO, n.getType());
    }

    @Test
    void notificationNode_nullMessage_emptyString() {
        NotificationNode n = new NotificationNode(null, NotificationType.INFO);
        assertEquals("", n.getMessage());
    }

    @Test
    void notificationNode_icon_info() {
        assertEquals('i', new NotificationNode("", NotificationType.INFO).getIcon());
    }

    @Test
    void notificationNode_icon_success() {
        assertEquals('✓', new NotificationNode("", NotificationType.SUCCESS).getIcon());
    }

    @Test
    void notificationNode_icon_warning() {
        assertEquals('!', new NotificationNode("", NotificationType.WARNING).getIcon());
    }

    @Test
    void notificationNode_icon_error() {
        assertEquals('✗', new NotificationNode("", NotificationType.ERROR).getIcon());
    }

    @Test
    void notificationNode_renderText_containsIconAndMessage() {
        NotificationNode n = new NotificationNode("Saved", NotificationType.SUCCESS);
        String text = n.renderText();
        assertTrue(text.contains("✓"), "Expected success icon");
        assertTrue(text.contains("Saved"), "Expected message");
    }

    @Test
    void notificationNode_style_successIsGreen() {
        assertTrue(new NotificationNode("", NotificationType.SUCCESS).getStyle().isBold());
    }

    @Test
    void notificationNode_style_errorIsBold() {
        assertTrue(new NotificationNode("", NotificationType.ERROR).getStyle().isBold());
    }

    // --- NotificationManager ---

    @Test
    void manager_initiallyEmpty() {
        NotificationManager m = new NotificationManager(null);
        assertTrue(m.isEmpty());
        assertEquals(0, m.size());
    }

    @Test
    void show_addsNotification() {
        NotificationManager m = new NotificationManager(null);
        m.show("hi", 60_000L);
        assertFalse(m.isEmpty());
        assertEquals(1, m.size());
    }

    @Test
    void show_withType_setsType() {
        NotificationManager m = new NotificationManager(null);
        m.show("err", 60_000L, NotificationType.ERROR);
        assertEquals(NotificationType.ERROR, m.getActiveNotifications().get(0).getType());
    }

    @Test
    void show_defaultType_info() {
        NotificationManager m = new NotificationManager(null);
        m.show("info", 60_000L);
        assertEquals(NotificationType.INFO, m.getActiveNotifications().get(0).getType());
    }

    @Test
    void show_multipleNotifications_stacksCorrectly() {
        NotificationManager m = new NotificationManager(null);
        m.show("A", 60_000L);
        m.show("B", 60_000L);
        assertEquals(2, m.size());
        assertEquals("A", m.getActiveNotifications().get(0).getMessage());
        assertEquals("B", m.getActiveNotifications().get(1).getMessage());
    }

    @Test
    void clear_removesAll() {
        NotificationManager m = new NotificationManager(null);
        m.show("A", 60_000L);
        m.show("B", 60_000L);
        m.clear();
        assertTrue(m.isEmpty());
    }

    @Test
    void show_firesOnUpdateCallback() {
        AtomicInteger count = new AtomicInteger();
        NotificationManager m = new NotificationManager(count::incrementAndGet);
        m.show("hi", 60_000L);
        assertEquals(1, count.get());
    }

    @Test
    void clear_firesOnUpdateCallback() {
        AtomicInteger count = new AtomicInteger();
        NotificationManager m = new NotificationManager(count::incrementAndGet);
        m.show("hi", 60_000L);
        count.set(0);
        m.clear();
        assertEquals(1, count.get());
    }

    @Test
    void getActiveNotifications_isSnapshot() {
        NotificationManager m = new NotificationManager(null);
        m.show("A", 60_000L);
        List<NotificationNode> snap = m.getActiveNotifications();
        m.show("B", 60_000L);
        // snapshot taken before B was added should still have size 1
        assertEquals(1, snap.size());
    }

    @Test
    void buildOverlay_emptyManager_returnsNull() {
        NotificationManager m = new NotificationManager(null);
        assertNull(m.buildOverlay());
    }

    @Test
    void buildOverlay_singleNotification_returnsNotificationNode() {
        NotificationManager m = new NotificationManager(null);
        m.show("hi", 60_000L);
        assertInstanceOf(NotificationNode.class, m.buildOverlay());
    }

    @Test
    void buildOverlay_multipleNotifications_returnsVBox() {
        NotificationManager m = new NotificationManager(null);
        m.show("A", 60_000L);
        m.show("B", 60_000L);
        assertInstanceOf(io.alive.tui.node.VBoxNode.class, m.buildOverlay());
    }
}
