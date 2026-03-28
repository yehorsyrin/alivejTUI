package io.alive.tui.event;

/**
 * Represents a single mouse event with position and button information.
 *
 * <p>Coordinates are 0-based terminal column/row values matching the layout grid.
 *
 * @param type   the action type
 * @param col    terminal column (0-based)
 * @param row    terminal row (0-based)
 * @param button mouse button index: 0 = left, 1 = middle, 2 = right, -1 = none (scroll)
 *
 * @author Jarvis (AI)
 */
public record MouseEvent(MouseType type, int col, int row, int button) {

    /** Factory for a left-button press event at the given position. */
    public static MouseEvent press(int col, int row) {
        return new MouseEvent(MouseType.PRESS, col, row, 0);
    }

    /** Factory for a left-button release event at the given position. */
    public static MouseEvent release(int col, int row) {
        return new MouseEvent(MouseType.RELEASE, col, row, 0);
    }

    /** Factory for a left-button click event at the given position. */
    public static MouseEvent click(int col, int row) {
        return new MouseEvent(MouseType.CLICK, col, row, 0);
    }

    /** Factory for a scroll-up event at the given position. */
    public static MouseEvent scrollUp(int col, int row) {
        return new MouseEvent(MouseType.SCROLL_UP, col, row, -1);
    }

    /** Factory for a scroll-down event at the given position. */
    public static MouseEvent scrollDown(int col, int row) {
        return new MouseEvent(MouseType.SCROLL_DOWN, col, row, -1);
    }
}
