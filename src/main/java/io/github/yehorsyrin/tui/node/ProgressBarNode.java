package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.style.Color;
import io.github.yehorsyrin.tui.style.Style;

/**
 * A horizontal progress bar (0.0 – 1.0).
 *
 * @author Jarvis (AI)
 */
public class ProgressBarNode extends Node {

    public static final char FILLED_CHAR = '█';
    public static final char EMPTY_CHAR  = '░';

    private double progress;
    private Style filledStyle;
    private Style emptyStyle;

    public ProgressBarNode(double progress) {
        this.progress = clamp(progress);
        this.filledStyle = Style.DEFAULT.withForeground(Color.GREEN);
        this.emptyStyle  = Style.DEFAULT.withForeground(Color.BRIGHT_BLACK);
    }

    public double getProgress() { return progress; }

    public void setProgress(double progress) {
        this.progress = clamp(progress);
    }

    public Style getFilledStyle() { return filledStyle; }
    public Style getEmptyStyle()  { return emptyStyle; }

    public ProgressBarNode filledStyle(Style style) {
        this.filledStyle = style;
        return this;
    }

    public ProgressBarNode emptyStyle(Style style) {
        this.emptyStyle = style;
        return this;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
