# Advanced Features

---

## Focus Management

AliveJTUI provides automatic `Tab` / `Shift+Tab` focus cycling. Register focusable nodes in `mount()` to include them in the cycle.

### Registering Focusable Nodes

```java
@Override
public void mount(Runnable onStateChange, EventBus eventBus) {
    super.mount(onStateChange, eventBus);

    // Declare fields
    // ButtonNode saveBtn = Button.of("[ Save ]", this::save);
    // InputNode nameInput = Input.of("", v -> setState(() -> name = v));

    // Register in Tab order
    registerFocusable(nameInput);
    registerFocusable(saveBtn);
    registerFocusable(cancelBtn);
}
```

### How Focus Works

| Action | Result |
|--------|--------|
| `Tab` | Move focus to the next registered node |
| `Shift+Tab` | Move focus to the previous registered node |
| `Enter` | If a `ButtonNode` is focused, its click callback fires |
| Visual indicator | Focused nodes are styled with `Theme.focused()` automatically |

### Focusable Node Types

The following node types implement `Focusable` and can be registered:

- `ButtonNode`
- `InputNode`
- `TextAreaNode`
- `CheckboxNode`
- `RadioGroupNode`
- `SelectNode`
- `VirtualListNode`

!!! info "Registration order matters"
    Nodes cycle in the order they were registered. Register them in the logical reading order of your UI (top to bottom, left to right).

---

## Timers

AliveJTUI provides one-shot and repeating timers that fire on the event loop thread — so you can safely call `setState()` inside them.

### One-shot Timer

Fires once after the specified delay (milliseconds).

```java
// Show a message for 2 seconds, then clear it
setState(() -> this.statusMessage = "Saved!");
AliveJTUI.schedule(2000, () -> setState(() -> this.statusMessage = ""));
```

### Repeating Timer

Fires repeatedly at the given interval until cancelled.

```java
// Animate a spinner at 10 fps
Runnable tick = () -> setState(() -> spinFrame = (spinFrame + 1) % SPIN_FRAMES.length);
AliveJTUI.scheduleRepeating(100, tick);

// Cancel later
AliveJTUI.cancelTimer(tick);
```

!!! tip "Spinner shortcut"
    The `SpinnerNode` has a built-in `nextFrame()` method designed for this pattern:

    ```java
    SpinnerNode spin = Spinner.of();
    AliveJTUI.scheduleRepeating(100, () -> setState(() -> spin.nextFrame()));
    ```

### Cancelling Timers

```java
Runnable tick = () -> setState(() -> doSomething());
AliveJTUI.scheduleRepeating(500, tick);

// Later — cancel by reference
AliveJTUI.cancelTimer(tick);
```

!!! warning "Cancel on unmount"
    If you start a repeating timer in `mount()`, cancel it in `unmount()` to avoid callbacks firing after the component is gone:

    ```java
    private Runnable spinTick;

    @Override
    public void mount(Runnable onStateChange, EventBus eventBus) {
        super.mount(onStateChange, eventBus);
        spinTick = () -> setState(() -> spin.nextFrame());
        AliveJTUI.scheduleRepeating(100, spinTick);
    }

    @Override
    public void unmount() {
        AliveJTUI.cancelTimer(spinTick);
        super.unmount();
    }
    ```

---

## Notifications (Toast Messages)

The `NotificationManager` renders non-blocking toast messages over your UI.

### Setup

```java
public class MyApp extends Component {

    private final NotificationManager notif =
        new NotificationManager(() -> setState(() -> {}));
```

The constructor takes a state-change trigger so notifications can dismiss themselves after their timeout.

### Showing Notifications

```java
notif.show("File saved successfully!", 2000);
notif.show("Warning: disk almost full",   4000, NotificationType.WARNING);
notif.show("Connection failed",           5000, NotificationType.ERROR);
notif.show("Upload complete",             2500, NotificationType.SUCCESS);
```

### Notification Types

| Type | Visual |
|------|--------|
| `NotificationType.INFO` (default) | Neutral / informational |
| `NotificationType.SUCCESS` | Green — positive outcome |
| `NotificationType.WARNING` | Yellow — needs attention |
| `NotificationType.ERROR` | Red — something went wrong |

### Rendering in render()

```java
@Override
public Node render() {
    Node root = VBox.of(/* your UI */);

    // Push notification overlay if one is active
    Node overlay = notif.buildOverlay();
    if (overlay != null) {
        AliveJTUI.pushOverlay(overlay);
    }

    return root;
}
```

!!! tip "Single overlay at a time"
    Call `notif.buildOverlay()` once per render and only push if non-null. Pushing multiple overlays is fine — they stack — but for notifications you only need one.

---

## Overlays (Dialogs)

The overlay API lets you push any `Node` as a full-screen overlay. Dialogs, notifications, and help screens all use this mechanism.

```java
// Push an overlay
AliveJTUI.pushOverlay(dialogNode);

// Remove the topmost overlay
AliveJTUI.popOverlay();
```

### Confirmation Dialog Pattern

```java
private void showConfirmDialog() {
    Node dialog = Dialog.of("Confirm", VBox.of(
        Text.of("  Delete this item?"),
        Text.of(""),
        HBox.of(
            Button.of("  [Yes]  ", () -> {
                AliveJTUI.popOverlay();
                setState(() -> performDelete());
            }),
            Button.of("  [No]   ", () -> AliveJTUI.popOverlay())
        )
    ));
    AliveJTUI.pushOverlay(dialog);
}
```

---

## Undo / Redo

`UndoManager` provides a simple command-pattern undo stack.

### Setup

```java
UndoManager undo = new UndoManager();      // default: 100 entries
UndoManager undo = new UndoManager(50);    // custom capacity
```

### Recording Operations

```java
String prevText = this.text;
this.text = "new value";

undo.record(
    () -> setState(() -> this.text = prevText),        // undo action
    () -> setState(() -> this.text = "new value")      // redo action
);
```

### Triggering Undo/Redo

```java
// In mount():
onKey(KeyType.CHARACTER, () -> {
    // Ctrl+Z = undo, Ctrl+Y = redo
    if (event.ctrl() && event.character() == 'z') undo.undo();
    if (event.ctrl() && event.character() == 'y') undo.redo();
});
```

### Querying State

```java
boolean canUndo = undo.canUndo();
boolean canRedo = undo.canRedo();
undo.clear();   // wipe the stack
```

### Showing Undo Status

```java
HBox.of(
    Button.of("[ Undo ]", () -> { if (undo.canUndo()) undo.undo(); })
          .style(undo.canUndo() ? activeStyle : dimStyle),
    Button.of("[ Redo ]", () -> { if (undo.canRedo()) undo.redo(); })
          .style(undo.canRedo() ? activeStyle : dimStyle)
)
```

---

## Virtual Lists

`VirtualListNode` renders only the rows currently visible on screen. It handles millions of items efficiently because the layout engine never measures off-screen rows.

```java
List<MyItem> items = loadAllItems();  // could be 100,000 items

VirtualListNode list = VirtualList.of(
    items.stream().map(MyItem::toString).collect(toList()),
    20   // 20 visible rows
);

registerFocusable(list);

// Key handlers
onKey(KeyType.ARROW_DOWN, () -> setState(() -> list.selectDown()));
onKey(KeyType.ARROW_UP,   () -> setState(() -> list.selectUp()));
onKey(KeyType.PAGE_DOWN,  () -> setState(() -> list.pageDown()));
onKey(KeyType.PAGE_UP,    () -> setState(() -> list.pageUp()));
onKey(KeyType.HOME,       () -> setState(() -> list.selectFirst()));
onKey(KeyType.END,        () -> setState(() -> list.selectLast()));

// Read selection
int selected = list.getSelectedIndex();
```

!!! info "Performance"
    The virtual list's rendering cost is proportional to the number of _visible_ rows, not the total item count. A list of 1,000,000 items with 20 visible rows renders the same as a list of 20 items.

---

## Async Background Work

See [Component Model — Async State](component-model.md#async-state) for full details.

Quick reference:

```java
// Pattern 1: setStateAsync (simple)
setStateAsync(() -> {
    Data result = fetchFromNetwork();     // background
    return () -> this.data = result;      // event loop
});

// Pattern 2: AsyncTask (with error handling)
AliveJTUI.runAsync(AsyncTask.of(
    () -> fetchFromNetwork(),
    result -> setState(() -> this.data = result),
    err    -> setState(() -> this.error = err.getMessage())
));
```

---

## Custom Backend

Implement `TerminalBackend` to integrate any rendering layer:

```java
public class MyBackend implements TerminalBackend {
    // ... implement the interface
}

AliveJTUI.run(new MyApp(), new MyBackend());
```

The `TerminalBackend` interface abstracts terminal dimensions, drawing cells, and delivering key events. This makes it straightforward to target WebSockets, SSH sessions, or any other transport.
