package io.alive.tui.node;

import io.alive.tui.core.Node;
import io.alive.tui.style.Style;

/**
 * A horizontal or vertical separator line.
 *
 * @author Jarvis (AI)
 */
public class DividerNode extends Node {

    public enum Orientation { HORIZONTAL, VERTICAL }

    public static final char DEFAULT_HORIZONTAL_CHAR = '─';
    public static final char DEFAULT_VERTICAL_CHAR   = '│';

    private final Orientation orientation;
    private char character;
    private Style style;

    public DividerNode(Orientation orientation) {
        this.orientation = orientation;
        this.character = orientation == Orientation.HORIZONTAL
            ? DEFAULT_HORIZONTAL_CHAR
            : DEFAULT_VERTICAL_CHAR;
        this.style = Style.DEFAULT;
    }

    public Orientation getOrientation() { return orientation; }
    public char getCharacter() { return character; }
    public Style getStyle() { return style; }

    public DividerNode character(char character) {
        this.character = character;
        return this;
    }

    public DividerNode style(Style style) {
        this.style = style;
        return this;
    }
}
