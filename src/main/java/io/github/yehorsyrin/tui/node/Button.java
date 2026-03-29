package io.github.yehorsyrin.tui.node;

/**
 * Factory for {@link ButtonNode}.
 *
 * <pre>{@code
 * Button.of("[+]", () -> count++)
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Button {

    private Button() {}

    public static ButtonNode of(String label, Runnable onClick) {
        return new ButtonNode(label, onClick);
    }
}
