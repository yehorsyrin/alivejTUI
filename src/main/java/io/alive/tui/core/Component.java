package io.alive.tui.core;

/**
 * Base class for all user-defined UI components.
 *
 * @author Jarvis (AI)
 */
public abstract class Component {

    private Runnable onStateChange;
    private Node previousTree;

    /**
     * Triggers a state mutation and schedules a re-render.
     * Must be called from the event loop thread.
     */
    protected void setState(Runnable mutation) {
        mutation.run();
        if (onStateChange != null) {
            onStateChange.run();
        }
    }

    /**
     * Declare the UI for the current state.
     */
    public abstract Node render();

    public void mount(Runnable onStateChange) {
        this.onStateChange = onStateChange;
    }

    public void unmount() {
        this.onStateChange = null;
    }

    public Node renderAndCache() {
        previousTree = render();
        return previousTree;
    }

    public Node getPreviousTree() {
        return previousTree;
    }
}
