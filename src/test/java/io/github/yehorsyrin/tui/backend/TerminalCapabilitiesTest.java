package io.github.yehorsyrin.tui.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerminalCapabilitiesTest {

    @Test
    void supportsDim_returnsTrue() {
        assertTrue(TerminalCapabilities.supportsDim());
    }
}
