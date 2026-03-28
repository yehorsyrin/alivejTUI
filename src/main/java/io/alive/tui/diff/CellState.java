package io.alive.tui.diff;

import io.alive.tui.style.Style;

/**
 * The visual state of a single terminal cell: character and style.
 *
 * @author Jarvis (AI)
 */
public record CellState(char character, Style style) {}
