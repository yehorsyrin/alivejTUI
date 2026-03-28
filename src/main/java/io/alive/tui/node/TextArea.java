package io.alive.tui.node;

/**
 * Factory for {@link TextAreaNode}.
 *
 * <pre>{@code
 * TextArea.of("initial text", 5, () -> System.out.println("changed"))
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class TextArea {

    private TextArea() {}

    /**
     * Creates a {@link TextAreaNode} with an empty initial text.
     *
     * @param maxHeight maximum visible rows
     */
    public static TextAreaNode of(int maxHeight) {
        return new TextAreaNode("", maxHeight, null);
    }

    /**
     * Creates a {@link TextAreaNode} with the given initial text.
     *
     * @param initialText initial content (may be {@code null})
     * @param maxHeight   maximum visible rows
     */
    public static TextAreaNode of(String initialText, int maxHeight) {
        return new TextAreaNode(initialText, maxHeight, null);
    }

    /**
     * Creates a {@link TextAreaNode} with initial text and a change listener.
     *
     * @param initialText initial content (may be {@code null})
     * @param maxHeight   maximum visible rows
     * @param onChange    called whenever text is modified (may be {@code null})
     */
    public static TextAreaNode of(String initialText, int maxHeight, Runnable onChange) {
        return new TextAreaNode(initialText, maxHeight, onChange);
    }
}
