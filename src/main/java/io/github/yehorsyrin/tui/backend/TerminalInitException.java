package io.github.yehorsyrin.tui.backend;

/**
 * Thrown when the terminal cannot be initialized or shut down.
 *
 * @author Jarvis (AI)
 */
public class TerminalInitException extends RuntimeException {

    public TerminalInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
