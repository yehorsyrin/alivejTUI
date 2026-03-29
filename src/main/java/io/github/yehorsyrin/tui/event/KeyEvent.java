package io.github.yehorsyrin.tui.event;

/**
 * Represents a single keyboard input event.
 *
 * @author Jarvis (AI)
 */
public record KeyEvent(KeyType type, char character, boolean ctrl, boolean alt, boolean shift) {

    public static KeyEvent ofCharacter(char c) {
        return new KeyEvent(KeyType.CHARACTER, c, false, false, false);
    }

    public static KeyEvent ofCharacter(char c, boolean ctrl, boolean alt, boolean shift) {
        return new KeyEvent(KeyType.CHARACTER, c, ctrl, alt, shift);
    }

    public static KeyEvent of(KeyType type) {
        return new KeyEvent(type, '\0', false, false, false);
    }

    public static KeyEvent of(KeyType type, boolean ctrl, boolean alt, boolean shift) {
        return new KeyEvent(type, '\0', ctrl, alt, shift);
    }
}
