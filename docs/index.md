# AliveJTUI

[![Maven Central](https://img.shields.io/maven-central/v/io.github.yehorsyrin/alivejTUI)](https://central.sonatype.com/artifact/io.github.yehorsyrin/alivejTUI)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=yehorsyrin_alivejTUI&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=yehorsyrin_alivejTUI)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=yehorsyrin_alivejTUI&metric=coverage)](https://sonarcloud.io/summary/new_code?id=yehorsyrin_alivejTUI)
[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-MIT-green)](https://github.com/yehorsyrin/alivejTUI/blob/master/LICENSE)

**A declarative TUI (Terminal User Interface) library for Java.**
Build terminal UIs as component trees — like React, but for the terminal.

---

## What is AliveJTUI?

AliveJTUI brings a React-style component model to the Java terminal. You describe your UI as a tree of nodes, call `setState()` when data changes, and the framework diffs and redraws only the cells that actually changed — no full-screen flicker, no manual cursor positioning.

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

## Quick Install

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.yehorsyrin</groupId>
    <artifactId>alivejTUI</artifactId>
    <version>0.1.1</version>
</dependency>
```

!!! info "Requirements"
    - Java 17 or later
    - Maven 3.8 or later

---

## Hello World

```java
import io.github.yehorsyrin.tui.core.*;
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
            Text.of("  Up/Down: +/-   ESC: quit").dim()
        );
    }

    public static void main(String[] args) {
        AliveJTUI.run(new CounterApp());
    }
}
```

---

## Features at a Glance

| Feature | Description |
|---------|-------------|
| **Declarative rendering** | Describe UI as a `Node` tree; diff engine redraws only changed cells |
| **React-style components** | Subclass `Component`, call `setState()`, framework re-renders |
| **Rich node library** | Text, Button, Input, Checkbox, RadioGroup, Select, Table, VirtualList, Dialog, and more |
| **Focus management** | `Tab` / `Shift+Tab` cycle through focusable nodes |
| **Theme system** | Swap `Theme.DARK` / `Theme.LIGHT` at runtime, or implement your own |
| **CSS-like styling** | `StyleSheet` with `#id`, `.class`, and type selectors |
| **Overlay API** | Push/pop dialogs and toast notifications on top of any UI |
| **Async state** | Run background work and apply results safely on the event loop thread |
| **Timers** | One-shot and repeating callbacks with automatic re-render |
| **Virtual lists** | Render 10,000+ items with only visible rows drawn |
| **Undo/Redo** | Built-in `UndoManager` for reversible operations |
| **Pluggable backends** | `LanternaBackend` (default), `MockBackend` (testing), or bring your own |

---

## Where to Go Next

<div class="grid cards" markdown>

- **[Getting Started](getting-started.md)**
  Installation, first app, running the demo

- **[Component Model](component-model.md)**
  `setState`, lifecycle, async, key handling

- **[Node Reference](nodes.md)**
  Every node type with code examples

- **[Styling](styling.md)**
  Colors, themes, CSS-like StyleSheet

- **[Advanced Features](advanced.md)**
  Focus, timers, undo/redo, overlays, virtual lists

- **[Testing](testing.md)**
  `MockBackend`, testing patterns

</div>
