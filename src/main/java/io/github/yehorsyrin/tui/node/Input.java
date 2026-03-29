package io.github.yehorsyrin.tui.node;

import java.util.function.Consumer;

/**
 * Factory for {@link InputNode}.
 *
 * <pre>{@code
 * Input.of(value, val -> setState(() -> this.value = val))
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class Input {

    private Input() {}

    public static InputNode of(String value, Consumer<String> onChange) {
        return new InputNode(value, onChange);
    }
}
