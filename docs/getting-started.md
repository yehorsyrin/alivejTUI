# Getting Started

This guide takes you from zero to a running AliveJTUI application in a few minutes.

---

## Requirements

- **Java 17+** — AliveJTUI uses modern Java features (records, sealed classes, pattern matching previews)
- **Maven 3.8+** — the library is available on Maven Central

!!! tip "JDK recommendations"
    OpenJDK 21 LTS is recommended. AliveJTUI is tested against JDK 17 and 21.

---

## Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.yehorsyrin</groupId>
    <artifactId>alivejTUI</artifactId>
    <version>0.1.1</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.yehorsyrin:alivejTUI:0.1.1'
```

```kotlin
implementation("io.github.yehorsyrin:alivejTUI:0.1.1")
```

!!! info "Latest version"
    Check [Maven Central](https://central.sonatype.com/artifact/io.github.yehorsyrin/alivejTUI) for the latest release.

---

## Your First Application

### Step 1 — Create the component class

Every AliveJTUI app is a `Component`. Subclass it, declare your state as fields, register key handlers in `mount()`, and return a `Node` tree from `render()`.

```java
import io.github.yehorsyrin.tui.core.*;
import io.github.yehorsyrin.tui.event.*;
import io.github.yehorsyrin.tui.node.*;
import io.github.yehorsyrin.tui.style.Color;

public class HelloApp extends Component {

    private String message = "Hello, terminal!";

    @Override
    public void mount(Runnable onStateChange, EventBus eventBus) {
        super.mount(onStateChange, eventBus);

        // Press 'R' to change the message
        onKey(KeyType.CHARACTER, () -> setState(() -> message = "You pressed a key!"));
    }

    @Override
    public Node render() {
        return VBox.of(
            Text.of("  AliveJTUI — Hello World").bold().color(Color.CYAN),
            Divider.horizontal(),
            Text.of("  " + message).color(Color.GREEN),
            Text.of(""),
            Text.of("  Press any key to change message. ESC to quit.").dim()
        );
    }

    public static void main(String[] args) {
        AliveJTUI.run(new HelloApp());
    }
}
```

### Step 2 — Run it

```bash
mvn compile exec:java -Dexec.mainClass="HelloApp"
```

!!! tip "On graphical systems"
    When a display is available (X11, Wayland, macOS, Windows), AliveJTUI opens a Swing window.
    On headless Linux servers it renders directly in the terminal.

---

## A More Complete Example: Counter

This example demonstrates `setState`, `HBox` layout, and multiple key bindings:

```java
public class CounterApp extends Component {

    private int count = 0;

    @Override
    public void mount(Runnable onStateChange, EventBus eventBus) {
        super.mount(onStateChange, eventBus);
        onKey(KeyType.ARROW_UP,   () -> setState(() -> count++));
        onKey(KeyType.ARROW_DOWN, () -> setState(() -> count--));
        onKey(KeyType.CHARACTER, () -> {
            // Press 'R' to reset
        });
    }

    @Override
    public Node render() {
        String color = count > 0 ? "GREEN" : count < 0 ? "RED" : "WHITE";
        return VBox.of(
            Text.of("  Counter Demo").bold().color(Color.CYAN),
            Divider.horizontal(),
            HBox.of(
                Text.of("  Count: ").dim(),
                Text.of(String.valueOf(count))
                    .bold()
                    .color(count >= 0 ? Color.GREEN : Color.RED)
            ),
            Text.of(""),
            Text.of("  ↑ increment   ↓ decrement   ESC quit").dim()
        );
    }

    public static void main(String[] args) {
        AliveJTUI.run(new CounterApp());
    }
}
```

---

## Running the Demo Application

The project ships with a fully-featured demo showing all widgets:

```bash
java -jar alivejTUI-demo.jar
```

### Demo Navigation

| Key | Action |
|-----|--------|
| `1` – `5` | Switch between demo tabs |
| `T` | Toggle Dark / Light theme |
| `D` | Open a confirmation dialog |
| `N` | Show a notification toast |
| `Tab` | Move focus to next widget |
| `Enter` | Activate focused button |
| `↑` / `↓` | Navigate table rows / virtual list / scroll |
| `PgUp` / `PgDn` | Page through lists / viewports |
| `Home` / `End` | Jump to top / bottom of virtual list |
| `+` / `-` | Increase / decrease progress bar |
| `X` | Toggle checkbox |
| `C` | Expand / collapse section |
| `S` | Cycle color select |
| `ESC` | Quit |

### Demo Tabs

| Tab | What you'll see |
|-----|-----------------|
| `1:Widgets` | Button, progress bar, checkbox, input, radio group, spinner, select |
| `2:Table` | Scrollable data table with keyboard navigation |
| `3:VirtualList` | 10,000 items — only visible rows rendered |
| `4:Text` | All text styles, inline markdown, word wrapping |
| `5:Layout` | BoxNode panels, collapsible section, scrollable viewport |

---

## Building from Source

```bash
git clone https://github.com/yehorsyrin/alivejTUI.git
cd alivejTUI

# Compile and run all tests
mvn test

# Build library jar + demo fat-jar
mvn package
# Produces: target/alivejTUI-demo.jar
```

---

## Next Steps

- **[Component Model](component-model.md)** — learn how `setState`, async, and lifecycle work
- **[Node Reference](nodes.md)** — browse all available UI nodes
- **[Styling](styling.md)** — apply colors, themes, and CSS-like selectors
