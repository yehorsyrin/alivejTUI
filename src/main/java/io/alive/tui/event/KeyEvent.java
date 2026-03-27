package io.alive.tui.event;

/**
 * Represents a single keyboard event.
 *
 * @author Jarvis (AI)
 */
public record KeyEvent(KeyType type, char character) {

    public static KeyEvent ofCharacter(char c) {
        return new KeyEvent(KeyType.CHARACTER, c);
    }

    public static KeyEvent of(KeyType type) {
        return new KeyEvent(type, '\0');
    }
}
