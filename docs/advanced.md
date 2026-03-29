# Advanced Features

---

## Focus Management

AliveJTUI provides automatic `Tab` / `Shift+Tab` focus cycling. Register focusable nodes in `mount()` to include them in the cycle.

### Registering Focusable Nodes

Declare focusable nodes as fields and register them in `mount()` in the order you want Tab to cycle through them.

The example below is a complete, runnable login form:

```java
import io.github.yehorsyrin.tui.core.Component;
import io.github.yehorsyrin.tui.core.Node;
import io.github.yehorsyrin.tui.event.EventBus;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.node.*;

public class LoginForm extends Component {

    // --- State ---
    private String status = "";

    // --- Widgets (fields so they survive re-renders) ---
    private final InputNode    userInput  = Input.of("", null);
    private final InputNode    passInput  = Input.of("", null);
    private final CheckboxNode rememberCb = Checkbox.of("Remember me", false, null);
    private final ButtonNode   loginBtn   = Button.of("[ Login ]",  this::doLogin);
    private final ButtonNode   cancelBtn  = Button.of("[ Cancel ]", this::doCancel);

    @Override
    public void mount(Runnable onStateChange, EventBus eventBus) {
        super.mount(onStateChange, eventBus);

        // Assign stable keys so focusById() can find them later
        userInput.setKey("user");
        passInput.setKey("pass");

        // Tab order: top → bottom
        registerFocusable(userInput);
        registerFocusable(passInput);
        registerFocusable(rememberCb);
        registerFocusable(loginBtn);
        registerFocusable(cancelBtn);

        // Character input: route to the focused InputNode,
        // or toggle the checkbox on Space
        eventBus.registerCharacter(c -> {
            if (c == ' ' && rememberCb.isFocused()) {
                setState(() -> rememberCb.toggle());
                return;
            }
            if (c < 32) return;
            if (userInput.isFocused())
                setState(() -> userInput.setValue(userInput.getValue() + c));
            if (passInput.isFocused())
                setState(() -> passInput.setValue(passInput.getValue() + c));
        });

        onKey(KeyType.BACKSPACE, () -> {
            if (userInput.isFocused()) {
                String v = userInput.getValue();
                if (!v.isEmpty()) setState(() -> userInput.setValue(v.substring(0, v.length() - 1)));
            }
            if (passInput.isFocused()) {
                String v = passInput.getValue();
                if (!v.isEmpty()) setState(() -> passInput.setValue(v.substring(0, v.length() - 1)));
            }
        });
    }

    @Override
    public Node render() {
        return VBox.of(
            Text.of("  Login").bold(),
            Divider.horizontal(),
            Text.of(""),
            HBox.of(Text.of("  Username : "), userInput),
            HBox.of(Text.of("  Password : "), passInput),
            HBox.of(Text.of("  "), rememberCb),
            Text.of(""),
            HBox.of(Text.of("  "), loginBtn, Text.of("  "), cancelBtn),
            Text.of(""),
            Text.of("  " + status).dim()
        );
    }

    private void doLogin() {
        if (userInput.getValue().isEmpty()) {
            setState(() -> status = "Username is required.");
            getFocusManager().focusById("user");
            return;
        }
        if (passInput.getValue().isEmpty()) {
            setState(() -> status = "Password is required.");
            getFocusManager().focusById("pass");
            return;
        }
        setState(() -> status = "Logging in as " + userInput.getValue() + " …");
        // perform actual authentication here
    }

    private void doCancel() {
        setState(() -> {
            userInput.setValue("");
            passInput.setValue("");
            if (rememberCb.isChecked()) rememberCb.toggle();
            status = "";
        });
    }
}
```

### How Focus Works

| Action | Result |
|--------|--------|
| `Tab` | Move focus to the next registered node |
| `Shift+Tab` | Move focus to the previous registered node |
| `Enter` | If a `ButtonNode` is focused, its click callback fires automatically |
| Visual indicator | Focused nodes are highlighted with `Theme.focused()` automatically |

`ButtonNode` is the only widget with built-in key handling. All other focusable nodes — `InputNode`, `CheckboxNode`, `RadioGroupNode`, `SelectNode` — require you to wire their interactions via `onKey()` or `eventBus.registerCharacter()` and guard on `isFocused()`.

### Focusable Node Types

The following node types implement `Focusable` and can be registered:

- `ButtonNode` — Enter fires click automatically when focused
- `InputNode` — wire `registerCharacter` + `BACKSPACE` manually
- `TextAreaNode` — wire `registerCharacter` + `BACKSPACE` + `ARROW` keys manually
- `CheckboxNode` — wire `Space` / `Enter` to `toggle()` manually
- `RadioGroupNode` — wire `Arrow Up` / `Down` to `setSelectedIndex()` manually
- `SelectNode` — wire `Enter` to `toggle()`, then arrow keys to `moveDown/Up()` and `accept()` manually
- `VirtualListNode` — wire `Arrow` / `Page` keys to `selectDown/Up()` manually

!!! info "Registration order matters"
    Nodes cycle in the order they were registered. Register them in the logical reading order of your UI (top to bottom, left to right).

### Programmatic Focus

`getFocusManager().focusById(key)` jumps focus to a node by its key. Set the key with `node.setKey("...")` before calling `registerFocusable`. The example above uses this to jump back to the empty field after a failed validation.

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
