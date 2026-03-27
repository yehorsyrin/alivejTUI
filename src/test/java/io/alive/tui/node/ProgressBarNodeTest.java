package io.alive.tui.node;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressBarNodeTest {

    @Test
    void progressClampedToRange() {
        ProgressBarNode bar = new ProgressBarNode(0.5);
        assertEquals(0.5, bar.getProgress());

        bar.setProgress(2.0);
        assertEquals(1.0, bar.getProgress());

        bar.setProgress(-1.0);
        assertEquals(0.0, bar.getProgress());
    }

    @Test
    void filledAndEmptyChars() {
        assertEquals('█', ProgressBarNode.FILLED_CHAR);
        assertEquals('░', ProgressBarNode.EMPTY_CHAR);
    }

    @Test
    void defaultStylesNotNull() {
        ProgressBarNode bar = new ProgressBarNode(0.5);
        assertNotNull(bar.getFilledStyle());
        assertNotNull(bar.getEmptyStyle());
    }
}
