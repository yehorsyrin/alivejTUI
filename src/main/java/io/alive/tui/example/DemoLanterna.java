package io.alive.tui.example;

import io.alive.tui.backend.LanternaBackend;
import io.alive.tui.core.AliveJTUI;

/**
 * Demo launcher — Lanterna backend (Swing window, cross-platform).
 *
 * <p>Opens a dedicated Swing terminal window. Best for running from an IDE
 * or environments where a native terminal is unavailable.
 *
 * @author Jarvis (AI)
 */
public class DemoLanterna {
    public static void main(String[] args) {
        AliveJTUI.run(new DemoApp(), new LanternaBackend());
    }
}
