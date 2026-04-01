package io.github.yehorsyrin.tui.node;

/**
 * Factory for {@link SpinnerNode}.
 *
 * <pre>{@code
 * Spinner.of()
 * Spinner.of(new String[]{"◐","◓","◑","◒"})
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Spinner {

    private Spinner() {}

    public static SpinnerNode of() {
        return new SpinnerNode();
    }

    public static SpinnerNode of(String[] frames) {
        return new SpinnerNode(frames);
    }
}
