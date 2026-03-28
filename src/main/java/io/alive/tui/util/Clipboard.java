package io.alive.tui.util;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Utility for system clipboard access (copy / paste).
 *
 * <p>All methods are static and safe to call from the event loop thread.
 * On headless environments (servers without a display), operations are silently
 * degraded: {@link #copy(String)} is a no-op and {@link #paste()} returns {@code ""}.
 *
 * @author Jarvis (AI)
 */
public final class Clipboard {

    private Clipboard() {}

    /**
     * Copies {@code text} to the system clipboard.
     *
     * @param text the text to copy; {@code null} is treated as an empty string
     */
    public static void copy(String text) {
        try {
            StringSelection sel = new StringSelection(text != null ? text : "");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
        } catch (HeadlessException | IllegalStateException ignored) {
            // Headless or clipboard temporarily unavailable — silently ignore
        }
    }

    /**
     * Pastes the current text content of the system clipboard.
     *
     * @return the clipboard text, or {@code ""} if the clipboard is empty, unavailable,
     *         or does not contain text data
     */
    public static String paste() {
        try {
            java.awt.datatransfer.Transferable content = Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getContents(null);
            if (content != null && content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) content.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (HeadlessException | IllegalStateException | UnsupportedFlavorException
                 | IOException ignored) {
            // Silently return empty string on any failure
        }
        return "";
    }

    /**
     * Returns {@code true} if the system clipboard currently contains text data.
     *
     * @return {@code false} if the clipboard is unavailable, headless, or has no text
     */
    public static boolean hasText() {
        try {
            java.awt.datatransfer.Clipboard cb =
                    Toolkit.getDefaultToolkit().getSystemClipboard();
            return cb.getContents(null) != null
                && cb.isDataFlavorAvailable(DataFlavor.stringFlavor);
        } catch (HeadlessException | IllegalStateException ignored) {
            return false;
        }
    }
}
