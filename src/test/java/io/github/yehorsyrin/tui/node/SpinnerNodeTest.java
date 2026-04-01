package io.github.yehorsyrin.tui.node;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpinnerNodeTest {

    @Test
    void defaultFramesNotEmpty() {
        SpinnerNode spinner = Spinner.of();
        assertNotNull(spinner.currentFrame());
        assertFalse(spinner.currentFrame().isEmpty());
    }

    @Test
    void nextFrameAdvances() {
        SpinnerNode spinner = Spinner.of();
        String first = spinner.nextFrame();
        String second = spinner.currentFrame();
        assertNotEquals(first, second);
    }

    @Test
    void frameWrapsAround() {
        String[] frames = {"A", "B"};
        SpinnerNode spinner = Spinner.of(frames);
        assertEquals("A", spinner.nextFrame());
        assertEquals("B", spinner.nextFrame());
        assertEquals("A", spinner.nextFrame()); // wrapped
    }

    @Test
    void emptyFramesThrows() {
        assertThrows(IllegalArgumentException.class, () -> Spinner.of(new String[0]));
        assertThrows(IllegalArgumentException.class, () -> Spinner.of(null));
    }
}
