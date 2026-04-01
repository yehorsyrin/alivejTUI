package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.style.Style;

/**
 * A bordered dialog container with an optional title displayed in the top border.
 *
 * <p>The dialog renders as a box with rounded corners and an optional centred title:
 * <pre>
 * ╭─ Title ─────────╮
 * │ content here    │
 * ╰─────────────────╯
 * </pre>
 *
 * <p>Dialogs are typically pushed as an overlay via
 * {@link io.github.yehorsyrin.tui.core.AliveJTUI#pushOverlay} and removed with
 * {@link io.github.yehorsyrin.tui.core.AliveJTUI#popOverlay}.
 *
 * @author Jarvis (AI)
 */
public class DialogNode extends Node {

    private final String title;
    private Style borderStyle;

    /**
     * Creates a dialog node.
     *
     * @param title       optional title shown in the top border; {@code null} or empty for no title
     * @param content     the content node rendered inside the dialog; may be {@code null}
     * @param borderStyle style for border characters; defaults to {@link Style#DEFAULT}
     */
    public DialogNode(String title, Node content, Style borderStyle) {
        this.title       = title != null ? title : "";
        this.borderStyle = borderStyle != null ? borderStyle : Style.DEFAULT;
        if (content != null) {
            this.children.add(content);
        }
    }

    /** Returns the dialog title, or an empty string if none was set. */
    public String getTitle() { return title; }

    /** Returns {@code true} if this dialog has a non-empty title. */
    public boolean hasTitle() { return !title.isEmpty(); }

    /** Returns the style used for border characters. */
    public Style getBorderStyle() { return borderStyle; }

    /** Sets the border style and returns {@code this} for chaining. */
    public DialogNode borderStyle(Style style) {
        this.borderStyle = style != null ? style : Style.DEFAULT;
        return this;
    }
}
