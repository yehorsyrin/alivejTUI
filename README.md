```
╔══════════════════════════════════════════════════════════════════════════╗
║                                                                          ║
║   █████╗ ██╗     ██╗██╗   ██╗███████╗      ██╗████████╗██╗   ██╗██╗      ║
║  ██╔══██╗██║     ██║██║   ██║██╔════╝      ██║╚══██╔══╝██║   ██║██║      ║
║  ███████║██║     ██║╚██╗ ██╔╝█████╗        ██║   ██║   ██║   ██║██║      ║
║  ██╔══██║██║     ██║ ╚████╔╝ ██╔══╝   ██   ██║   ██║   ██║   ██║██║      ║
║  ██║  ██║███████╗██║  ╚═══╝  ███████╗ ╚█████╔╝   ██║   ╚██████╔╝██║      ║
║  ╚═╝  ╚═╝╚══════╝╚═╝         ╚══════╝  ╚════╝    ╚═╝    ╚═════╝ ╚═╝      ║
║                                                                          ║
║          Declarative TUI library for Java              v0.1.1            ║
║          ─────────────────────────────────────────────────────────       ║
║                    crafted with pride by  J A R V I S  (AI)              ║
╚══════════════════════════════════════════════════════════════════════════╝
```

# AliveJTUI

[![Maven Central](https://img.shields.io/maven-central/v/io.github.yehorsyrin/alivejTUI)](https://central.sonatype.com/artifact/io.github.yehorsyrin/alivejTUI)
[![SonarCloud](https://github.com/yehorsyrin/alivejTUI/actions/workflows/sonar.yml/badge.svg)](https://github.com/yehorsyrin/alivejTUI/actions/workflows/sonar.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=yehorsyrin_alivejTUI&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=yehorsyrin_alivejTUI)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=yehorsyrin_alivejTUI&metric=coverage)](https://sonarcloud.io/summary/new_code?id=yehorsyrin_alivejTUI)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=yehorsyrin_alivejTUI&metric=bugs)](https://sonarcloud.io/summary/new_code?id=yehorsyrin_alivejTUI)
[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

A declarative TUI (Terminal User Interface) library for Java.
Build terminal UIs as component trees — like React, but for the terminal.

**[Documentation](https://yehorsyrin.github.io/alivejTUI)**

```
 AliveJTUI Demo v0.1.0  theme: [Dark]
 1:Widgets  2:Table  3:VirtualList  4:Text  5:Layout
──────────────────────────────────────────────────────
  [ Click Me! ]  Clicked: 3  Spin: |
  Progress [+][-]
  [████████████░░░░░░░░]  60%
  ☑ Notifications enabled    Input: [hello_]
  Theme radio [Up/Down]:  (x) Dark  ( ) Light
  Color select [S]:  << Cyan >>
──────────────────────────────────────────────────────
  1-5:Tab  T:Theme  D:Dialog  N:Notify  C:Collapse  X:Checkbox  S:Select  +/-:Progress  ESC:Quit
```

---

## Features

- **Declarative rendering** — describe UI as a `Node` tree; the library diffs and redraws only changed cells
- **React-style components** — subclass `Component`, call `setState()`, let the framework re-render
- **Rich node library** — text, buttons, inputs, checkboxes, radio groups, selects, tables, virtual lists, viewports, dialogs, spinners, progress bars, and more
- **Focus management** — `Tab` / `Shift+Tab` cycle through focusable nodes; `Enter` fires the focused button
- **Theme system** — swap `Theme.DARK` / `Theme.LIGHT` (or implement your own) at runtime
- **CSS-like styling** — `StyleSheet` with `#id`, `.class`, and type selectors
- **Overlay API** — push/pop dialogs and toast notifications on top of any UI
- **Async state** — run background work and apply results safely on the event loop thread
- **Timers** — one-shot and repeating callbacks with automatic re-render
- **Virtual lists** — render 10,000+ items with only visible rows drawn
- **Undo/Redo** — built-in `UndoManager` for reversible operations
- **Diff-based renderer** — only changed terminal cells are redrawn; no full-screen flicker
- **Pluggable backends** — `LanternaBackend` (default), `MockBackend` (testing), or bring your own

---

## Requirements

- Java 17+
- Maven 3.8+

---

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>io.github.yehorsyrin</groupId>
    <artifactId>alivejTUI</artifactId>
    <version>0.1.1</version>
</dependency>
```

### 2. Write a component

```java
import io.github.yehorsyrin.tui.core.*;
import io.github.yehorsyrin.tui.event.*;
import io.github.yehorsyrin.tui.node.*;
import io.github.yehorsyrin.tui.style.Color;

public class CounterApp extends Component {

    private int count = 0;

    @Override
    public void mount(Runnable onStateChange, EventBus eventBus) {
        super.mount(onStateChange, eventBus);
        onKey(KeyType.ARROW_UP,   () -> setState(() -> count++));
        onKey(KeyType.ARROW_DOWN, () -> setState(() -> count--));
    }

    @Override
    public Node render() {
        return VBox.of(
            Text.of("  Counter Demo").bold().color(Color.CYAN),
            Divider.horizontal(),
            HBox.of(
                Text.of("  Count: ").dim(),
                Text.of(String.valueOf(count)).bold().color(Color.GREEN)
            ),
            Text.of(""),
            Text.of("  Up/Down: +/-   ESC: quit").dim()
        );
    }

    public static void main(String[] args) {
        AliveJTUI.run(new CounterApp());
    }
}
```

### 3. Run the demo jar

```bash
java -jar alivejTUI-demo.jar
```

On systems with a graphical display (X11/Wayland/macOS/Windows) a Swing window opens.
On headless Linux the app runs directly in the terminal.

---

## Component Model

### Subclassing Component

```java
public class MyApp extends Component {

    // State fields
    private String text = "";
    private boolean checked = false;

    @Override
    public void mount(Runnable onStateChange, EventBus eventBus) {
        super.mount(onStateChange, eventBus);
        // Register key handlers here (auto-unregistered on unmount)
        onKey(KeyType.ENTER, () -> setState(() -> text += "!"));
        eventBus.registerCharacter(c -> {
            if (c >= 32) setState(() -> text += c);
        });
    }

    @Override
    public Node render() {
        // Return a new Node tree every call — the diff engine handles the rest
        return VBox.of(
            Text.of("Input: " + text),
            Checkbox.of("Option", checked, () -> setState(() -> checked = !checked))
        );
    }
}
```

### setState

`setState(Runnable mutation)` — applies the mutation and triggers a re-render.

```java
setState(() -> {
    this.count++;
    this.label = "clicked";
});
```

### Async state

`setStateAsync(Supplier<Runnable> task)` — runs work on a background thread, then applies the mutation on the event loop thread.

```java
setStateAsync(() -> {
    String result = fetchFromNetwork(); // background thread
    return () -> this.data = result;    // event loop thread
});
```

Or use `AliveJTUI.runAsync(AsyncTask)` directly:

```java
AliveJTUI.runAsync(AsyncTask.of(
    () -> fetchData(),
    result -> setState(() -> this.data = result),
    err    -> setState(() -> this.error = err.getMessage())
));
```

### Lifecycle

| Method | Called when |
|--------|-------------|
| `mount(onStateChange, eventBus)` | Component enters the UI tree |
| `render()` | State changes; should return a pure Node tree |
| `unmount()` | Component leaves the UI tree; key handlers auto-unregistered |
| `onError(Exception)` | `render()` throws; return a fallback node |
| `shouldUpdate()` | Override to skip re-render (optimization; default: always) |

---

## Node Reference

All factory methods return a node with a fluent builder API.

### Text & Display

| Expression | Description |
|------------|-------------|
| `Text.of("hello")` | Plain single-line text |
| `Text.of("hello").bold()` | Bold text |
| `Text.of("hello").italic()` | Italic text |
| `Text.of("hello").underline()` | Underlined text |
| `Text.of("hello").strikethrough()` | Strikethrough text |
| `Text.of("hello").dim()` | Dimmed/muted text |
| `Text.of("hello").color(Color.CYAN)` | Foreground color |
| `Text.of("hello").background(Color.BLUE)` | Background color |
| `Text.ofMarkdown("**bold** and *italic*")` | Inline markdown |
| `Paragraph.of("long text...")` | Word-wrapped plain text |
| `Paragraph.ofMarkdown("**bold** paragraph")` | Word-wrapped markdown |
| `Divider.horizontal()` | Horizontal rule `───────` |
| `Divider.vertical()` | Vertical rule `│` |

**Supported markdown syntax:** `**bold**`, `*italic*`, `` `code` ``, `~~strikethrough~~`

### Layout

| Expression | Description |
|------------|-------------|
| `VBox.of(node1, node2, ...)` | Vertical stack |
| `VBox.of(nodes).gap(1)` | Vertical stack with spacing |
| `HBox.of(node1, node2, ...)` | Horizontal stack |
| `HBox.of(nodes).gap(2)` | Horizontal stack with spacing |
| `new BoxNode(child, true, borderStyle)` | Bordered container |

### Interactive Widgets

#### Button

```java
ButtonNode btn = Button.of("[ OK ]", () -> System.out.println("clicked"));
registerFocusable(btn);   // participate in Tab cycling
// Tab to focus, Enter to click
```

#### Text Input

```java
InputNode input = Input.of("initial", value -> setState(() -> this.text = value));
registerFocusable(input); // Tab to focus; typed characters update value
```

#### TextArea

```java
TextAreaNode area = TextArea.of("", 5); // 5 visible rows
area.insertChar('h');
area.insertChar('i');
String content = area.getText();
```

#### Checkbox

```java
CheckboxNode cb = Checkbox.of("Enable feature", checked, () -> setState(() -> checked = !checked));
// Press X (or bind any key) to toggle
```

#### RadioGroup

```java
RadioGroupNode radio = RadioGroup.of("Option A", "Option B", "Option C");
// radio.getSelectedIndex(), radio.setSelectedIndex(1)
```

#### Select

```java
SelectNode sel = Select.of("Red", "Green", "Blue");
// sel.getSelectedValue(), sel.setSelectedIndex(2)
```

### Lists & Tables

#### Table

```java
List<String> headers = List.of("Name", "Role", "City");
List<List<String>> rows = List.of(
    List.of("Alice", "Engineer", "Berlin"),
    List.of("Bob",   "Designer", "London")
);
TableNode table = Table.of(headers, rows, 8); // show 8 rows
table.selectDown(); // navigate
table.selectUp();
```

#### VirtualList (large data sets)

```java
List<String> items = IntStream.range(1, 100_001)
    .mapToObj(i -> "Item " + i)
    .collect(toList());

VirtualListNode list = VirtualList.of(items, 15); // 15 visible rows
list.selectDown();
list.selectUp();
list.pageDown();
list.pageUp();
list.selectFirst();
list.selectLast();
int idx = list.getSelectedIndex();
```

#### Viewport (scrollable window)

```java
Node content = VBox.of(/* many nodes */);
ViewportNode vp = Viewport.of(content, 10); // 10 visible rows
vp.scrollDown();
vp.scrollUp();
vp.pageDown();
vp.pageUp();
vp.scrollToTop();
vp.scrollToBottom();
vp.showScrollbar(false); // hide scroll bar
```

Store the viewport as a field and wire scroll to key handlers:

```java
// in mount():
onKey(KeyType.ARROW_DOWN, () -> setState(() -> viewport.scrollDown()));
onKey(KeyType.ARROW_UP,   () -> setState(() -> viewport.scrollUp()));
```

### Progress Bar

```java
ProgressBarNode bar = new ProgressBarNode(0.65); // 65%
bar.setProgress(0.80);
bar.filledStyle(Style.DEFAULT.withForeground(Color.GREEN));
bar.emptyStyle(Style.DEFAULT.withForeground(Color.BRIGHT_BLACK));
```

### Spinner

```java
SpinnerNode spin = Spinner.of();            // default frames: | / - \
SpinnerNode spin = Spinner.of(new String[]{ "⠋","⠙","⠹","⠸","⠼","⠴","⠦","⠧","⠇","⠏" });

// Advance frame on a timer:
AliveJTUI.scheduleRepeating(100, () -> setState(() -> spin.nextFrame()));
```

### Collapsible

```java
Node section = Collapsible.of("Settings",
    Text.of("  Option A"),
    Text.of("  Option B")
);
// Collapsible.expanded("Title", ...) — starts expanded
CollapsibleNode col = (CollapsibleNode) section;
col.toggle();    // expand/collapse
col.expand();
col.collapse();
boolean open = col.isExpanded();
```

### Dialog

```java
// Push a dialog overlay
Node dialog = Dialog.of("Confirm", VBox.of(
    Text.of("Are you sure?"),
    HBox.of(
        Button.of("[Yes]", () -> setState(() -> { dialogNode = null; /* confirm */ })),
        Button.of("[No]",  () -> setState(() -> dialogNode = null))
    )
));
AliveJTUI.pushOverlay(dialog);

// Dismiss
AliveJTUI.popOverlay();
```

### Help Panel

```java
Node help = HelpPanel.of(
    new KeyBinding("Tab",     "Next field"),
    new KeyBinding("Enter",   "Confirm"),
    new KeyBinding("ESC",     "Quit")
);
```

---

## Styling

### Style

`Style` is immutable. Build from `Style.DEFAULT`:

```java
Style bold  = Style.DEFAULT.withBold(true);
Style fancy = Style.DEFAULT
    .withForeground(Color.CYAN)
    .withBackground(Color.BRIGHT_BLACK)
    .withBold(true)
    .withItalic(true);
```

Apply to a node:

```java
Text.of("hello").style(fancy)
// or shorthand:
Text.of("hello").bold().color(Color.CYAN)
```

### Color

```java
// Standard ANSI 16
Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE,
Color.CYAN, Color.MAGENTA, Color.WHITE, Color.BLACK,
Color.BRIGHT_RED, Color.BRIGHT_GREEN, /* ... */

// 256-color
Color.ansi256(202) // orange

// True color
Color.rgb(255, 128, 0) // orange (if terminal supports it)
```

### Theme

```java
// Global theme (default: DARK)
AliveJTUI.setTheme(Theme.LIGHT);
AliveJTUI.setTheme(Theme.DARK);

// Use in components
Theme t = AliveJTUI.getTheme();
Text.of("Title").style(t.primary())
Text.of("Hint").style(t.muted())
Text.of("OK").style(t.success())
Text.of("Error").style(t.error())
```

Semantic roles: `foreground()`, `muted()`, `primary()`, `secondary()`, `success()`, `warning()`, `error()`, `focused()`.

**Custom theme:**

```java
Theme myTheme = new Theme.BuiltinTheme(
    Style.DEFAULT,                                         // foreground
    Style.DEFAULT.withDim(true),                          // muted
    Style.DEFAULT.withForeground(Color.rgb(0,200,255)).withBold(true), // primary
    Style.DEFAULT.withForeground(Color.MAGENTA),          // secondary
    Style.DEFAULT.withForeground(Color.GREEN),            // success
    Style.DEFAULT.withForeground(Color.YELLOW),           // warning
    Style.DEFAULT.withForeground(Color.RED),              // error
    Style.DEFAULT.withForeground(Color.CYAN).withBold(true) // focused
);
AliveJTUI.setTheme(myTheme);
```

### StyleSheet (CSS-like)

```java
StyleSheet sheet = new StyleSheet()
    .add(Selector.byId("title"),        Style.DEFAULT.withForeground(Color.CYAN).withBold(true))
    .add(Selector.byClass("muted"),     Style.DEFAULT.withDim(true))
    .add(Selector.byType(ButtonNode.class), Style.DEFAULT.withForeground(Color.YELLOW));

// Tag nodes
Text.of("Hello").withId("title")
Text.of("hint").withClassName("muted")

// Apply to tree
sheet.applyToTree(rootNode);
```

---

## Notifications

```java
NotificationManager notif = new NotificationManager(() -> setState(() -> {}));

notif.show("Saved successfully!", 2000);
notif.show("Warning: low disk space", 4000, NotificationType.WARNING);
notif.show("Error: connection failed", 5000, NotificationType.ERROR);
notif.show("File uploaded",            2500, NotificationType.SUCCESS);
// NotificationType: INFO (default), SUCCESS, WARNING, ERROR

// In render():
Node overlay = notif.buildOverlay();
if (overlay != null) AliveJTUI.pushOverlay(overlay);
```

---

## Timers

```java
// One-shot after 2 seconds
AliveJTUI.schedule(2000, () -> setState(() -> this.message = "done"));

// Repeating every 150ms (e.g. spinner animation)
Runnable tick = () -> setState(() -> spinFrame = (spinFrame + 1) % SPIN.length);
AliveJTUI.scheduleRepeating(150, tick);

// Cancel
AliveJTUI.cancelTimer(tick);
```

---

## Focus Management

```java
// Register focusable nodes in mount():
registerFocusable(myButton);
registerFocusable(myInput);
registerFocusable(anotherButton);

// Tab  → focusNext()
// Shift+Tab → focusPrev()
// Enter → click() on the focused ButtonNode
```

Nodes implementing `Focusable`: `ButtonNode`, `InputNode`, `TextAreaNode`, `CheckboxNode`, `RadioGroupNode`, `SelectNode`, `VirtualListNode`.

---

## Undo / Redo

```java
UndoManager undo = new UndoManager(); // default 100 entries
// or: new UndoManager(50);

String prev = text;
text = "new value";
undo.record(
    () -> setState(() -> text = prev),          // undo
    () -> setState(() -> text = "new value")    // redo
);

// In key handlers:
onKey(KeyType.CHARACTER, () -> {
    if (event.ctrl() && event.character() == 'z') undo.undo();
    if (event.ctrl() && event.character() == 'y') undo.redo();
});

boolean canUndo = undo.canUndo();
boolean canRedo = undo.canRedo();
undo.clear();
```

---

## Key Handling

```java
@Override
public void mount(Runnable onStateChange, EventBus eventBus) {
    super.mount(onStateChange, eventBus);

    // Special keys
    onKey(KeyType.ARROW_DOWN, () -> setState(() -> selectedRow++));
    onKey(KeyType.ARROW_UP,   () -> setState(() -> selectedRow--));
    onKey(KeyType.PAGE_DOWN,  () -> setState(() -> selectedRow += 10));
    onKey(KeyType.ENTER,      () -> confirmSelection());

    // Consuming (return true stops propagation)
    onKey(KeyType.BACKSPACE, () -> {
        if (myInput.length() > 0) {
            setState(() -> myInput = myInput.substring(0, myInput.length() - 1));
            return true; // consumed
        }
        return false;
    });

    // Any printable character
    eventBus.registerCharacter(c -> {
        if (c >= 32) setState(() -> inputText += c);
    });
}
```

**All key types:** `CHARACTER`, `ENTER`, `BACKSPACE`, `DELETE`, `ARROW_UP`, `ARROW_DOWN`, `ARROW_LEFT`, `ARROW_RIGHT`, `ESCAPE`, `TAB`, `SHIFT_TAB`, `HOME`, `END`, `PAGE_UP`, `PAGE_DOWN`, `EOF`.

---

## Backends

| Backend | Use case |
|---------|----------|
| `LanternaBackend` | Default. Cross-platform. Opens a Swing window when a display is available; falls back to in-terminal mode on headless Linux. |
| `MockBackend` | Unit testing — no real terminal required. |

```java
// Default (LanternaBackend)
AliveJTUI.run(new MyApp());

// Custom backend — implement TerminalBackend
AliveJTUI.run(new MyApp(), new MyCustomBackend());

// Testing
AliveJTUI.run(new MyApp(), new MockBackend(80, 24));
```

`TerminalBackend` is a plain interface — implement it to integrate any other rendering layer (ncurses, raw ANSI, WebSocket, etc.).

---

## Testing

Use `MockBackend` to test components without a real terminal:

```java
MockBackend backend = new MockBackend(80, 24);
AliveJTUI.run(new MyApp(), backend);

// Simulate key presses
backend.sendKey(KeyEvent.of(KeyType.ARROW_DOWN));
backend.sendKey(KeyEvent.ofCharacter('x'));

// Inspect rendered output
String cell = backend.getCell(0, 0); // character at col=0, row=0
```

---

## Demo Application

```bash
java -jar alivejTUI-demo.jar
```

### Navigation

| Key | Action |
|-----|--------|
| `1` – `5` | Switch tab |
| `T` | Toggle Dark / Light theme |
| `D` | Open confirmation dialog |
| `N` | Show notification toast |
| `Tab` | Move focus to next widget |
| `Enter` | Click focused button |
| `↑ ↓` | Navigate table rows / virtual list / scroll viewport (tab 5) |
| `PgUp PgDn` | Page through virtual list / viewport |
| `Home End` | Jump to top / bottom of virtual list |
| `+` / `-` | Increase / decrease progress bar |
| `X` | Toggle checkbox |
| `C` | Expand / collapse section |
| `S` | Cycle color select |
| `ESC` | Quit |

### Tabs

| Tab | Content |
|-----|---------|
| `1:Widgets` | Button, progress bar, checkbox, input, radio, spinner, select |
| `2:Table` | Scrollable data table with keyboard navigation |
| `3:VirtualList` | 10,000 items — only visible rows rendered |
| `4:Text` | All text styles, inline markdown, word wrapping |
| `5:Layout` | BoxNode panels, collapsible section, scrollable viewport |

---

## Building

```bash
# Compile and run tests
mvn test

# Build library jar + demo fat-jar
mvn package
# produces target/alivejTUI-demo.jar
```

---

## Project Structure

```
src/
  main/java/io/github/yehorsyrin/tui/
    core/         AliveJTUI, Component, Node, FocusManager, NotificationManager,
                  TimerManager, UndoManager, AsyncTask, Focusable
    node/         All node types: Text, VBox, HBox, Button, Input, TextArea,
                  Checkbox, RadioGroup, Select, Table, VirtualList, Viewport,
                  ProgressBar, Spinner, Dialog, Collapsible, HelpPanel, ...
    style/        Color, Style, Theme, StyleSheet, Selector
    event/        EventBus, KeyEvent, KeyType
    backend/      TerminalBackend (interface), LanternaBackend, MockBackend,
                  TerminalCapabilities
    render/       Renderer, LayoutEngine, Differ, TreeFlattener
    example/      DemoApp, DemoLanterna, TodoApp, Showcase
  test/           unit tests
```

---

## License

MIT
