# Node Reference

All node factory methods return objects with a fluent builder API. Every node can be given an `id` and `className` for [StyleSheet](styling.md#stylesheet-css-like-selectors) targeting.

```java
node.withId("title")
node.withClassName("muted")
node.withClassName("primary")
```

---

## Text & Display

### Text

Single-line text with inline styling.

```java
Text.of("hello")                          // plain text
Text.of("hello").bold()                   // bold
Text.of("hello").italic()                 // italic
Text.of("hello").underline()              // underlined
Text.of("hello").strikethrough()          // strikethrough
Text.of("hello").dim()                    // dimmed/muted
Text.of("hello").color(Color.CYAN)        // foreground color
Text.of("hello").background(Color.BLUE)   // background color
Text.of("hello").style(myStyle)           // full Style object
```

#### Inline Markdown

```java
Text.ofMarkdown("**bold** and *italic* and `code` and ~~strikethrough~~")
```

Supported tokens: `**bold**`, `*italic*`, `` `code` ``, `~~strikethrough~~`.

### Paragraph

Word-wrapped multi-line text. Useful for long descriptions that should reflow to the terminal width.

```java
Paragraph.of("This is a long paragraph that will be word-wrapped...")
Paragraph.ofMarkdown("**Important:** This paragraph supports *inline markdown* too.")
```

### Divider

```java
Divider.horizontal()   // horizontal rule: ───────────
Divider.vertical()     // vertical rule:  │
```

---

## Layout

### VBox

Stacks children vertically.

```java
VBox.of(node1, node2, node3)
VBox.of(nodes).gap(1)          // 1 blank line between children
VBox.of(List.of(node1, node2)) // from a list
```

### HBox

Stacks children horizontally.

```java
HBox.of(node1, node2, node3)
HBox.of(nodes).gap(2)          // 2 spaces between children
```

### BoxNode (bordered container)

```java
import io.github.yehorsyrin.tui.node.*;

Node bordered = new BoxNode(child, true, borderStyle);
```

!!! tip "Combining layouts"
    Nest `VBox` and `HBox` freely to build complex layouts:

    ```java
    VBox.of(
        Text.of("Title").bold(),
        Divider.horizontal(),
        HBox.of(
            VBox.of(Text.of("Left column"), button1),
            VBox.of(Text.of("Right column"), button2)
        )
    )
    ```

---

## Interactive Widgets

### Button

```java
ButtonNode btn = Button.of("[ OK ]", () -> handleClick());
registerFocusable(btn);   // participate in Tab cycling
```

- Press `Tab` to focus, `Enter` to activate.
- The callback fires immediately on click or Enter.

### Input (single line)

```java
InputNode input = Input.of("initial value", value -> setState(() -> this.text = value));
registerFocusable(input);
```

- When focused, typed characters update the value.
- The callback is invoked on every keystroke.

### TextArea (multi-line)

```java
TextAreaNode area = TextArea.of("", 5);  // empty, 5 visible rows
registerFocusable(area);

// Programmatic manipulation
area.insertChar('h');
area.insertChar('i');
String content = area.getText();
```

### Checkbox

```java
CheckboxNode cb = Checkbox.of("Enable feature", false,
    () -> setState(() -> checked = !checked));
registerFocusable(cb);

// Wire Space (and optionally Enter) to toggle when focused
onKey(KeyType.SPACE, () -> {
    if (cb.isFocused()) setState(() -> cb.toggle());
});
```

- Displayed as `[✓] label` when checked, `[ ] label` when unchecked.
- `toggle()` flips the state and fires the `onChange` callback.

### RadioGroup

```java
RadioGroupNode radio = RadioGroup.of("Option A", "Option B", "Option C");
registerFocusable(radio);

// Wire arrow keys to navigate when focused
onKey(KeyType.ARROW_DOWN, () -> {
    if (radio.isFocused()) setState(() ->
        radio.setSelectedIndex(Math.min(
            radio.getSelectedIndex() + 1, radio.getOptions().size() - 1)));
});
onKey(KeyType.ARROW_UP, () -> {
    if (radio.isFocused()) setState(() ->
        radio.setSelectedIndex(Math.max(radio.getSelectedIndex() - 1, 0)));
});

// Read/write selection
int idx = radio.getSelectedIndex();
radio.setSelectedIndex(1);
```

### Select (dropdown-style)

Renders as a single-row `[ Option ▾ ]` header. When opened it expands below to show all options with a `›` cursor.

```java
SelectNode sel = Select.of("Red", "Green", "Blue");
registerFocusable(sel);

// Enter opens/closes the dropdown when focused
onKey(KeyType.ENTER, () -> {
    if (sel.isFocused()) setState(() -> sel.toggle());
});
// Arrow keys navigate when the dropdown is open
onKey(KeyType.ARROW_DOWN, () -> {
    if (sel.isOpen()) setState(() -> sel.moveDown());
});
onKey(KeyType.ARROW_UP, () -> {
    if (sel.isOpen()) setState(() -> sel.moveUp());
});
// Enter also confirms selection when open; Escape cancels
onKey(KeyType.ESCAPE, () -> {
    if (sel.isOpen()) setState(() -> sel.close());
});

// Read selected value
String current = sel.getSelectedValue();
sel.setSelectedIndex(2);  // programmatic selection
```

---

## Lists & Tables

### Table

Scrollable tabular data with keyboard navigation.

```java
List<String> headers = List.of("Name", "Role", "City");
List<List<String>> rows = List.of(
    List.of("Alice", "Engineer", "Berlin"),
    List.of("Bob",   "Designer", "London"),
    List.of("Carol", "Manager",  "Paris")
);
TableNode table = Table.of(headers, rows, 8); // show 8 rows at a time

// Navigation
table.selectDown();
table.selectUp();
```

Wire navigation to key handlers:

```java
onKey(KeyType.ARROW_DOWN, () -> setState(() -> table.selectDown()));
onKey(KeyType.ARROW_UP,   () -> setState(() -> table.selectUp()));
```

### VirtualList (large data sets)

Renders only the visible rows — suitable for lists with thousands of items.

```java
List<String> items = IntStream.range(1, 100_001)
    .mapToObj(i -> "Item " + i)
    .collect(toList());

VirtualListNode list = VirtualList.of(items, 15); // 15 visible rows

// Navigation
list.selectDown();
list.selectUp();
list.pageDown();
list.pageUp();
list.selectFirst();
list.selectLast();

int idx = list.getSelectedIndex();

registerFocusable(list);
```

### Viewport (scrollable window)

Wraps any `Node` tree in a scrollable window showing a fixed number of rows.

```java
Node content = VBox.of(/* many nodes */);
ViewportNode vp = Viewport.of(content, 10); // 10 visible rows

// Scroll methods
vp.scrollDown();
vp.scrollUp();
vp.pageDown();
vp.pageUp();
vp.scrollToTop();
vp.scrollToBottom();
vp.showScrollbar(false);   // hide the scroll indicator
```

Wire to key handlers (store `vp` as a field):

```java
onKey(KeyType.ARROW_DOWN, () -> setState(() -> vp.scrollDown()));
onKey(KeyType.ARROW_UP,   () -> setState(() -> vp.scrollUp()));
onKey(KeyType.PAGE_DOWN,  () -> setState(() -> vp.pageDown()));
onKey(KeyType.PAGE_UP,    () -> setState(() -> vp.pageUp()));
```

---

## Progress Bar

```java
ProgressBarNode bar = new ProgressBarNode(0.65);   // 65%

bar.setProgress(0.80);                             // update progress

// Custom styling
bar.filledStyle(Style.DEFAULT.withForeground(Color.GREEN));
bar.emptyStyle(Style.DEFAULT.withForeground(Color.BRIGHT_BLACK));
```

Progress value is a `double` in the range `0.0` – `1.0`.

---

## Spinner

An animated spinner that cycles through frames.

```java
// Default frames: | / - \
SpinnerNode spin = Spinner.of();

// Braille dot frames
SpinnerNode spin = Spinner.of(new String[]{ "⠋","⠙","⠹","⠸","⠼","⠴","⠦","⠧","⠇","⠏" });

// Advance frame on a timer
AliveJTUI.scheduleRepeating(100, () -> setState(() -> spin.nextFrame()));
```

---

## Collapsible

An expandable/collapsible section with a title.

```java
// Starts collapsed
Node section = Collapsible.of("Settings",
    Text.of("  Option A"),
    Text.of("  Option B"),
    Text.of("  Option C")
);

// Starts expanded
Node section = Collapsible.expanded("Settings",
    Text.of("  Option A"),
    Text.of("  Option B")
);

// Cast to manipulate programmatically
CollapsibleNode col = (CollapsibleNode) section;
col.toggle();
col.expand();
col.collapse();
boolean open = col.isExpanded();
```

---

## Dialog (overlay)

Push a dialog on top of the current UI. The dialog is rendered as an overlay; the background UI is still visible but inactive.

```java
Node dialog = Dialog.of("Confirm Delete", VBox.of(
    Text.of("  Are you sure you want to delete this file?"),
    Text.of(""),
    HBox.of(
        Button.of("  [Yes]  ", () -> {
            AliveJTUI.popOverlay();
            setState(() -> performDelete());
        }),
        Button.of("  [No]  ", () -> AliveJTUI.popOverlay())
    )
));

AliveJTUI.pushOverlay(dialog);
```

```java
// Dismiss the topmost overlay
AliveJTUI.popOverlay();
```

!!! warning "Button registration in dialogs"
    Buttons inside dialogs that need focus/Enter handling should be registered with `registerFocusable()` before pushing the overlay.

---

## Help Panel

Display a keyboard shortcut reference at the bottom of the screen.

```java
Node help = HelpPanel.of(
    new KeyBinding("Tab",     "Next field"),
    new KeyBinding("Enter",   "Confirm"),
    new KeyBinding("↑ ↓",    "Navigate"),
    new KeyBinding("ESC",     "Quit")
);
```

---

## Node Quick Reference

| Node | Factory | Focusable |
|------|---------|-----------|
| `TextNode` | `Text.of(string)` | No |
| `ParagraphNode` | `Paragraph.of(string)` | No |
| `Divider` | `Divider.horizontal()` / `.vertical()` | No |
| `VBoxNode` | `VBox.of(...)` | No |
| `HBoxNode` | `HBox.of(...)` | No |
| `BoxNode` | `new BoxNode(child, border, style)` | No |
| `ButtonNode` | `Button.of(label, action)` | Yes |
| `InputNode` | `Input.of(value, onChange)` | Yes |
| `TextAreaNode` | `TextArea.of(value, rows)` | Yes |
| `CheckboxNode` | `Checkbox.of(label, checked, toggle)` | Yes |
| `RadioGroupNode` | `RadioGroup.of(option...)` | Yes |
| `SelectNode` | `Select.of(option...)` | Yes |
| `TableNode` | `Table.of(headers, rows, visibleRows)` | No |
| `VirtualListNode` | `VirtualList.of(items, visibleRows)` | Yes |
| `ViewportNode` | `Viewport.of(content, visibleRows)` | No |
| `ProgressBarNode` | `new ProgressBarNode(progress)` | No |
| `SpinnerNode` | `Spinner.of()` | No |
| `CollapsibleNode` | `Collapsible.of(title, children...)` | No |
| `DialogNode` | `Dialog.of(title, content)` | No |
| `HelpPanelNode` | `HelpPanel.of(bindings...)` | No |
