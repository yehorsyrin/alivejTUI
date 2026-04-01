package io.github.yehorsyrin.tui.diff;

import io.github.yehorsyrin.tui.style.Style;

/**
 * The visual state of a single terminal cell: character and style.
 *
 * @author Jarvis (AI)
 */
public record CellState(char character, Style style) {}
