package io.alive.tui.node;

import io.alive.tui.core.AliveJTUI;
import io.alive.tui.core.Node;
import io.alive.tui.style.Style;

/**
 * Factory and helper for {@link DialogNode}.
 *
 * <pre>{@code
 * // Simple usage — push as overlay and close on key press:
 * Node dlg = Dialog.of("Confirm", Text.of("Delete item?"));
 * AliveJTUI.pushOverlay(dlg);   // show
 * AliveJTUI.popOverlay();       // hide
 *
 * // Convenience:
 * Dialog.show("Alert", Text.of("Something happened!"));
 * Dialog.hide();
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Dialog {

    private Dialog() {}

    /**
     * Creates a titled dialog containing {@code content}.
     *
     * @param title   the title shown in the top border
     * @param content the node to display inside the dialog
     */
    public static DialogNode of(String title, Node content) {
        return new DialogNode(title, content, Style.DEFAULT);
    }

    /**
     * Creates a borderless-title dialog (no title in the top border).
     *
     * @param content the node to display inside the dialog
     */
    public static DialogNode of(Node content) {
        return new DialogNode(null, content, Style.DEFAULT);
    }

    /**
     * Creates a titled dialog with a custom border style.
     *
     * @param title       the title string
     * @param content     the content node
     * @param borderStyle style for border characters
     */
    public static DialogNode of(String title, Node content, Style borderStyle) {
        return new DialogNode(title, content, borderStyle);
    }

    // --- Overlay convenience methods ---

    /**
     * Pushes a titled dialog as the current overlay and triggers a re-render.
     * Must be called from the event loop thread.
     *
     * @param title   dialog title
     * @param content dialog content
     */
    public static void show(String title, Node content) {
        AliveJTUI.pushOverlay(of(title, content));
    }

    /**
     * Pushes a no-title dialog as the current overlay and triggers a re-render.
     * Must be called from the event loop thread.
     *
     * @param content dialog content
     */
    public static void show(Node content) {
        AliveJTUI.pushOverlay(of(content));
    }

    /**
     * Removes the current overlay (dismisses the dialog) and triggers a re-render.
     * Must be called from the event loop thread.
     */
    public static void hide() {
        AliveJTUI.popOverlay();
    }
}
