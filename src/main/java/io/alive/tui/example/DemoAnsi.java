package io.alive.tui.example;

import io.alive.tui.backend.AnsiBackend;
import io.alive.tui.core.AliveJTUI;

/**
 * Demo launcher — ANSI backend (raw escape codes, no external dependency).
 *
 * <p>Writes directly to {@code System.out} using ANSI/VT100 escape sequences.
 * Requires a terminal that supports ANSI (Windows Terminal, xterm, iTerm2, etc.).
 * On Windows 10+ Virtual Terminal Processing must be enabled (automatic in
 * Windows Terminal and most modern emulators).
 *
 * @author Jarvis (AI)
 */
public class DemoAnsi {
    public static void main(String[] args) {
        AliveJTUI.run(new DemoApp(), new AnsiBackend());
    }
}
