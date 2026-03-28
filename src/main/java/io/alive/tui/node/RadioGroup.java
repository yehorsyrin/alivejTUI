package io.alive.tui.node;

import java.util.Arrays;
import java.util.List;

/**
 * Factory for {@link RadioGroupNode}.
 *
 * <pre>{@code
 * RadioGroupNode rg = RadioGroup.of("Option A", "Option B", "Option C");
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public final class RadioGroup {

    private RadioGroup() {}

    /**
     * Creates a new {@link RadioGroupNode} from varargs option labels.
     *
     * @param options the option labels
     * @return a new {@link RadioGroupNode}
     */
    public static RadioGroupNode of(String... options) {
        return new RadioGroupNode(Arrays.asList(options));
    }

    /**
     * Creates a new {@link RadioGroupNode} from a list of option labels.
     *
     * @param options the option labels
     * @return a new {@link RadioGroupNode}
     */
    public static RadioGroupNode of(List<String> options) {
        return new RadioGroupNode(options);
    }
}
