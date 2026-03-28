package io.alive.tui.event;

/**
 * Represents a single input event — either a keyboard key or a mouse action.
 *
 * <p>For mouse events ({@link KeyType#MOUSE}), the keyboard fields are unused and
 * {@link #mouseEvent()} carries the mouse data. For all other types, {@link #mouseEvent()}
 * returns {@code null}.
 *
 * @author Jarvis (AI)
 */
public record KeyEvent(KeyType type, char character, boolean ctrl, boolean alt, boolean shift,
                       MouseEvent mouseEvent) {

    // --- Keyboard factories ---

    public static KeyEvent ofCharacter(char c) {
        return new KeyEvent(KeyType.CHARACTER, c, false, false, false, null);
    }

    public static KeyEvent ofCharacter(char c, boolean ctrl, boolean alt, boolean shift) {
        return new KeyEvent(KeyType.CHARACTER, c, ctrl, alt, shift, null);
    }

    public static KeyEvent of(KeyType type) {
        return new KeyEvent(type, '\0', false, false, false, null);
    }

    public static KeyEvent of(KeyType type, boolean ctrl, boolean alt, boolean shift) {
        return new KeyEvent(type, '\0', ctrl, alt, shift, null);
    }

    // --- Mouse factory ---

    /**
     * Creates a {@link KeyType#MOUSE} event carrying the given mouse data.
     *
     * @param mouse the mouse event data; must not be {@code null}
     */
    public static KeyEvent ofMouse(MouseEvent mouse) {
        if (mouse == null) throw new IllegalArgumentException("mouseEvent must not be null");
        return new KeyEvent(KeyType.MOUSE, '\0', false, false, false, mouse);
    }
}
