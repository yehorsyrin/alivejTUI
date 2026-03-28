package io.alive.tui.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerminalCapabilitiesTest {

    @Test
    void supportsDim_returnsFalse() {
        assertFalse(TerminalCapabilities.supportsDim());
    }
}
