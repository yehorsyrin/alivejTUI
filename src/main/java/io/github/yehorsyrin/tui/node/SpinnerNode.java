package io.github.yehorsyrin.tui.node;

import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.style.Style;

/**
 * An animated spinner that cycles through frames on each render.
 *
 * @author Jarvis (AI)
 */
public class SpinnerNode extends Node {

    public static final String[] DEFAULT_FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    private final String[] frames;
    private int frame;
    private Style style;

    public SpinnerNode() {
        this(DEFAULT_FRAMES);
    }

    public SpinnerNode(String[] frames) {
        if (frames == null || frames.length == 0) throw new IllegalArgumentException("Spinner frames must not be empty");
        this.frames = frames;
        this.frame = 0;
        this.style = Style.DEFAULT;
    }

    /** Returns the current frame character and advances to the next. */
    public String nextFrame() {
        String current = frames[frame];
        frame = (frame + 1) % frames.length;
        return current;
    }

    /** Returns the current frame character without advancing. */
    public String currentFrame() {
        return frames[frame];
    }

    public int getFrameIndex() { return frame; }
    public String[] getFrames() { return frames; }
    public Style getStyle() { return style; }

    public SpinnerNode style(Style style) {
        this.style = style;
        return this;
    }
}
