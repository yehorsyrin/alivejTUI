package io.alive.tui.example;

import io.alive.tui.core.AliveJTUI;
import io.alive.tui.nativeio.backend.NativeBackend;

/**
 * Demo launcher — Native backend (JNA-based raw terminal I/O, no Lanterna).
 *
 * <p>Uses platform-native APIs to put the terminal into raw mode and render
 * via ANSI escape sequences: {@code WindowsRawMode} + {@code WindowsVtOutput}
 * on Windows, {@code PosixRawMode} on Linux / macOS.
 *
 * <p>Run directly from a real terminal (cmd.exe, PowerShell, bash, zsh).
 * Do <b>not</b> run from inside an IDE console — IDE consoles typically do
 * not support raw mode.
 *
 * @author Jarvis (AI)
 */
public class DemoNative {
    public static void main(String[] args) {
        AliveJTUI.run(new DemoApp(), NativeBackend.create());
    }
}
