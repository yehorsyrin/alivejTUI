package io.github.yehorsyrin.tui.example;

import io.github.yehorsyrin.tui.core.AliveJTUI;
import io.github.yehorsyrin.tui.platform.backend.Backends;

/**
 * Demo launcher — native backend (no Lanterna dependency).
 *
 * <p>Selects the backend automatically:
 * <ul>
 *   <li><b>Headless / server / terminal</b> — {@code NativeTerminalBackend}:
 *       reads from stdin, writes ANSI escape sequences to stdout.
 *       Works in Windows Terminal, iTerm2, GNOME Terminal, xterm, etc.</li>
 *   <li><b>GUI desktop</b> (display available) — {@code SwingBackend}:
 *       opens a dedicated {@code JFrame} terminal window, no external terminal needed.</li>
 * </ul>
 *
 * @author Jarvis (AI)
 */
public class DemoNative {
    public static void main(String[] args) {
        AliveJTUI.run(new DemoApp(), Backends.createAuto());
    }
}
