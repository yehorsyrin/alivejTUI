package io.alive.tui.node;

import io.alive.tui.core.Focusable;
import io.alive.tui.core.Node;
import io.alive.tui.style.Style;

import java.util.List;

/**
 * A dropdown / select component that shows the selected option with an arrow indicator.
 *
 * <p>When closed the node occupies a single row:
 * <pre>[ Selected Option ▾ ]</pre>
 *
 * <p>When open it expands below the header to show all options, highlighting the
 * currently hovered item:
 * <pre>
 * [ Selected Option ▴ ]
 *   Option A
 * › Option B          ← hovered
 *   Option C
 * </pre>
 *
 * <p>Typical usage with keyboard handlers in a {@code Component.render()}:
 * <pre>{@code
 * if (select.isOpen()) {
 *     bus.register(KeyType.ArrowDown, () -> { select.moveDown(); markDirty(); });
 *     bus.register(KeyType.ArrowUp,   () -> { select.moveUp();   markDirty(); });
 *     bus.register(KeyType.Enter,     () -> { select.accept();   markDirty(); });
 *     bus.register(KeyType.Escape,    () -> { select.close();    markDirty(); });
 * }
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public class SelectNode extends Node implements Focusable {

    /** Arrow shown when the dropdown is collapsed. */
    public static final char ARROW_DOWN = '▾';
    /** Arrow shown when the dropdown is expanded. */
    public static final char ARROW_UP   = '▴';
    /** Cursor prefix for the hovered option row. */
    public static final String CURSOR   = "› ";
    /** Padding prefix for non-hovered option rows (same width as CURSOR). */
    public static final String PADDING  = "  ";

    private final List<String> options;
    private int selectedIndex;
    private int hoverIndex;
    private boolean open      = false;
    private boolean focused   = false;

    private Style normalStyle   = Style.DEFAULT;
    private Style focusedStyle  = Style.DEFAULT.withBold(true);
    private Style hoverStyle    = Style.DEFAULT.withBold(true);
    private Style selectedStyle = Style.DEFAULT.withBold(true);

    private final Runnable onChange;

    /**
     * Creates a select node.
     *
     * @param options       list of option labels; must not be null or empty
     * @param selectedIndex initially selected index (clamped to valid range)
     * @param onChange      called when the selected index changes; may be null
     */
    public SelectNode(List<String> options, int selectedIndex, Runnable onChange) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("SelectNode requires at least one option");
        }
        this.options       = List.copyOf(options);
        this.selectedIndex = Math.max(0, Math.min(selectedIndex, options.size() - 1));
        this.hoverIndex    = this.selectedIndex;
        this.onChange      = onChange;
    }

    // --- State accessors ---

    /** Returns the immutable list of option labels. */
    public List<String> getOptions() { return options; }

    /** Returns the index of the currently selected option. */
    public int getSelectedIndex() { return selectedIndex; }

    /** Returns the label of the currently selected option. */
    public String getSelectedOption() { return options.get(selectedIndex); }

    /** Returns the index of the currently hovered option (only meaningful when open). */
    public int getHoverIndex() { return hoverIndex; }

    /** Returns {@code true} if the dropdown list is currently expanded. */
    public boolean isOpen() { return open; }

    // --- Open / close ---

    /** Opens the dropdown and resets the hover cursor to the selected index. */
    public void open() {
        this.hoverIndex = this.selectedIndex;
        this.open = true;
    }

    /** Closes the dropdown without changing the selected index. */
    public void close() { this.open = false; }

    /** Toggles the open state. If opening, resets the hover cursor to the selected index. */
    public void toggle() { if (open) close(); else open(); }

    // --- Keyboard navigation (call from event handlers while open) ---

    /** Moves the hover cursor down by one step (wraps around). */
    public void moveDown() {
        hoverIndex = (hoverIndex + 1) % options.size();
    }

    /** Moves the hover cursor up by one step (wraps around). */
    public void moveUp() {
        hoverIndex = (hoverIndex - 1 + options.size()) % options.size();
    }

    /**
     * Confirms the current hover index as the selected index, closes the dropdown,
     * and fires the {@code onChange} callback if the selection actually changed.
     */
    public void accept() {
        int prev = selectedIndex;
        selectedIndex = hoverIndex;
        open = false;
        if (prev != selectedIndex && onChange != null) {
            onChange.run();
        }
    }

    // --- Styles ---

    public Style getNormalStyle()   { return normalStyle; }
    public Style getFocusedStyle()  { return focusedStyle; }
    public Style getHoverStyle()    { return hoverStyle; }
    public Style getSelectedStyle() { return selectedStyle; }

    public SelectNode normalStyle(Style s)   { this.normalStyle   = s != null ? s : Style.DEFAULT; return this; }
    public SelectNode focusedStyle(Style s)  { this.focusedStyle  = s != null ? s : Style.DEFAULT; return this; }
    public SelectNode hoverStyle(Style s)    { this.hoverStyle    = s != null ? s : Style.DEFAULT; return this; }
    public SelectNode selectedStyle(Style s) { this.selectedStyle = s != null ? s : Style.DEFAULT; return this; }

    // --- Focusable ---

    @Override public boolean isFocused()           { return focused; }
    @Override public void    setFocused(boolean f) { this.focused = f; }
    @Override public String  getFocusId()          { return options.get(0); }
}
