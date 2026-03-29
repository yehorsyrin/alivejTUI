# Component Model

AliveJTUI's component model is deliberately close to React. If you've used React class components, you already know 80% of this.

---

## The Component Class

Every UI in AliveJTUI is a `Component`. Subclass it to create your application:

```java
import io.github.yehorsyrin.tui.core.*;
import io.github.yehorsyrin.tui.event.*;
import io.github.yehorsyrin.tui.node.*;

public class MyApp extends Component {

    // --- State fields ---
    private String inputText = "";
    private boolean checked = false;
    private int counter = 0;

    @Override
    public void mount(Runnable onStateChange, EventBus eventBus) {
        super.mount(onStateChange, eventBus);

        // Key handlers are registered here and auto-removed on unmount
        onKey(KeyType.ENTER, () -> setState(() -> counter++));

        eventBus.registerCharacter(c -> {
            if (c >= 32) setState(() -> inputText += c);
        });
    }

    @Override
    public Node render() {
        // Return a fresh Node tree every call — the diff engine handles the rest
        return VBox.of(
            Text.of("Input: " + inputText),
            Text.of("Count: " + counter),
            Checkbox.of("Option", checked, () -> setState(() -> checked = !checked))
        );
    }
}
```

!!! tip "State fields are plain Java fields"
    Unlike React hooks, state in AliveJTUI is just instance fields on your class. There is no special state container — `setState()` is purely a signal to re-render.

---

## setState

`setState(Runnable mutation)` applies the mutation and queues a re-render. Multiple rapid calls within the same event are batched.

```java
// Single mutation
setState(() -> this.count++);

// Multiple fields in one mutation
setState(() -> {
    this.count++;
    this.label = "clicked";
    this.lastAction = Instant.now();
});
```

!!! warning "Mutation must be synchronous"
    The `Runnable` passed to `setState` runs on the event loop thread. Do not perform blocking I/O or slow computation inside it. For background work, use `setStateAsync` instead.

---

## Async State

For background work (network calls, file I/O, database queries), use `setStateAsync`:

```java
setStateAsync(() -> {
    // This lambda runs on a background thread
    String result = fetchFromDatabase();

    // Return a Runnable that runs on the event loop thread
    return () -> this.data = result;
});
```

For more control — including error handling — use `AliveJTUI.runAsync`:

```java
AliveJTUI.runAsync(AsyncTask.of(
    () -> fetchUserData(userId),                         // background thread
    result -> setState(() -> this.user = result),        // success — event loop
    err    -> setState(() -> this.error = err.getMessage()) // failure — event loop
));
```

!!! info "Thread safety"
    AliveJTUI has a single event loop thread. `setState` and all render callbacks always execute on that thread. You never need explicit synchronization as long as you don't modify state fields directly from background threads — always funnel changes back through `setState`.

---

## Lifecycle

| Method | When it is called |
|--------|-------------------|
| `mount(onStateChange, eventBus)` | The component enters the UI tree. Register key handlers and timers here. |
| `render()` | Called after every `setState()`. Should return a pure, stateless `Node` tree. |
| `unmount()` | The component leaves the UI tree. Key handlers registered via `onKey()` are automatically removed. |
| `onError(Exception e)` | If `render()` throws, this is called. Return a fallback `Node` to display. |
| `shouldUpdate()` | Override to prevent unnecessary re-renders. Default: always `true`. |

### mount

```java
@Override
public void mount(Runnable onStateChange, EventBus eventBus) {
    super.mount(onStateChange, eventBus); // Always call super first

    // Register key handlers (auto-removed on unmount)
    onKey(KeyType.ARROW_DOWN, () -> setState(() -> selectedRow++));
    onKey(KeyType.ARROW_UP,   () -> setState(() -> selectedRow--));

    // Register focusable nodes
    registerFocusable(myButton);
    registerFocusable(myInput);

    // Start a repeating timer
    AliveJTUI.scheduleRepeating(100, () -> setState(() -> spin.nextFrame()));
}
```

### render

```java
@Override
public Node render() {
    // Pure function — no side effects, no I/O
    return VBox.of(
        Text.of("Row: " + selectedRow).bold(),
        myButton,
        myInput
    );
}
```

!!! warning "render() must be pure"
    Do not call `setState()` from inside `render()` — it will cause an infinite render loop. `render()` should only read state fields and build a `Node` tree.

### onError

```java
@Override
public Node onError(Exception e) {
    return VBox.of(
        Text.of("Something went wrong:").bold().color(Color.RED),
        Text.of("  " + e.getMessage()).color(Color.BRIGHT_RED)
    );
}
```

### shouldUpdate

```java
@Override
public boolean shouldUpdate() {
    // Only re-render if the data actually changed
    return !Objects.equals(previousData, currentData);
}
```

---

## Key Handling

Register key handlers in `mount()`. They are automatically unregistered when the component unmounts.

```java
@Override
public void mount(Runnable onStateChange, EventBus eventBus) {
    super.mount(onStateChange, eventBus);

    // Named key types
    onKey(KeyType.ARROW_DOWN, () -> setState(() -> selectedRow++));
    onKey(KeyType.ARROW_UP,   () -> setState(() -> selectedRow--));
    onKey(KeyType.PAGE_DOWN,  () -> setState(() -> selectedRow += 10));
    onKey(KeyType.ENTER,      () -> confirmSelection());
    onKey(KeyType.ESCAPE,     () -> AliveJTUI.stop());

    // A handler that may consume the event (stops propagation)
    onKey(KeyType.BACKSPACE, () -> {
        if (!inputText.isEmpty()) {
            setState(() -> inputText = inputText.substring(0, inputText.length() - 1));
            return true; // consumed — no further processing
        }
        return false; // not consumed — propagate
    });

    // Any printable character
    eventBus.registerCharacter(c -> {
        if (c >= 32) setState(() -> inputText += c);
    });
}
```

### All Key Types

| Key Type | Description |
|----------|-------------|
| `CHARACTER` | Any printable character |
| `ENTER` | Enter / Return |
| `BACKSPACE` | Backspace |
| `DELETE` | Delete |
| `ARROW_UP` | Up arrow |
| `ARROW_DOWN` | Down arrow |
| `ARROW_LEFT` | Left arrow |
| `ARROW_RIGHT` | Right arrow |
| `ESCAPE` | Escape |
| `TAB` | Tab |
| `SHIFT_TAB` | Shift+Tab |
| `HOME` | Home |
| `END` | End |
| `PAGE_UP` | Page Up |
| `PAGE_DOWN` | Page Down |
| `EOF` | Ctrl+D / end of input |

---

## Running the Application

```java
// Default backend (Lanterna — opens Swing window or terminal)
AliveJTUI.run(new MyApp());

// Custom backend
AliveJTUI.run(new MyApp(), new MyCustomBackend());

// Programmatic stop
AliveJTUI.stop();
```

---

## Project Structure

```
src/main/java/io/github/yehorsyrin/tui/
  core/      AliveJTUI, Component, Node, FocusManager,
             NotificationManager, TimerManager, UndoManager,
             AsyncTask, Focusable
  node/      All node types (Text, VBox, Button, Input, ...)
  style/     Color, Style, Theme, StyleSheet, Selector
  event/     EventBus, KeyEvent, KeyType
  backend/   TerminalBackend, LanternaBackend, MockBackend
  render/    Renderer, LayoutEngine, Differ, TreeFlattener
```
