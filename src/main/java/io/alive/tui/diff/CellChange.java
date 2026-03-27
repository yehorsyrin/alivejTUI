package io.alive.tui.diff;

import io.alive.tui.style.Style;

/**
 * An atomic terminal cell update: draw {@code character} at ({@code col}, {@code row})
 * using the given {@code style}.
 *
 * @author Jarvis (AI)
 */
public record CellChange(int col, int row, char character, Style style) {}
