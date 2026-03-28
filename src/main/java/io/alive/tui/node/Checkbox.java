package io.alive.tui.node;

/**
 * Factory for {@link CheckboxNode}.
 *
 * <pre>{@code
 * Checkbox.of("Enable feature", false, () -> System.out.println("toggled"))
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Checkbox {

    private Checkbox() {}

    /**
     * Creates a new {@link CheckboxNode} with the given label, initial state, and callback.
     *
     * @param label    display label
     * @param checked  initial checked state
     * @param onChange callback invoked on toggle; may be {@code null}
     * @return a new {@link CheckboxNode}
     */
    public static CheckboxNode of(String label, boolean checked, Runnable onChange) {
        return new CheckboxNode(label, checked, onChange);
    }
}
