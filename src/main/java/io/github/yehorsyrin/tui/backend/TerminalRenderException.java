package io.github.yehorsyrin.tui.backend;

/**
 * Thrown when a terminal render or I/O operation fails at runtime
 * (as opposed to initialization/shutdown failures).
 *
 * @author Jarvis (AI)
 */
public class TerminalRenderException extends RuntimeException {

    public TerminalRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
