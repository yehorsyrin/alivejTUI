# AliveJTUI — Declarative TUI Library for Java

## Concept

AliveJTUI is a Java library for building terminal user interfaces in a declarative style.
Inspired by React (components, state, re-render) and Bubble Tea (Go), but for the Java ecosystem,
where no real equivalent exists.

The developer describes **what** to show, not **how** to draw it in the terminal.
The library computes the diff between the previous and new state and redraws only what changed.

---

## Core Ideas

### Components
UI is built from components — independent blocks with their own state and a `render()` method.
Components can be nested inside each other.

```java
public class Counter extends Component {
    private int count = 0;

    @Override
    public Node render() {
        return VBox.of(
            Text.of("Count: " + count),
            Button.of("[+]", () -> setState(() -> count++))
        );
    }
}
```

### State and Re-rendering
Changing state via `setState()` triggers a re-render of only that component and its children.
The library compares the new virtual tree with the old one and updates only the changed terminal cells.

### Layout
Built-in layout engine with two primitives:
- `VBox` — vertical layout (top to bottom)
- `HBox` — horizontal layout (side by side)

Sizes can be fixed or relative (percentage of parent / fill).

### Events
Subscribe to keyboard events via a simple API:

```java
onKey(Key.ARROW_UP, () -> setState(() -> selected--));
onKey(Key.ENTER, () -> onSelect(items.get(selected)));
```

---

## Built-in Components

| Component     | Description                                      |
|---------------|--------------------------------------------------|
| `Text`        | Plain text with color and style support          |
| `Box`         | Container with or without a border               |
| `VBox`        | Vertical layout container                        |
| `HBox`        | Horizontal layout container                      |
| `Button`      | Button with click/Enter handler                  |
| `Input`       | Text input field                                 |
| `List`        | Scrollable list of items                         |
| `ProgressBar` | Progress bar                                     |
| `Spinner`     | Animated loading indicator                       |
| `Divider`     | Horizontal or vertical separator                 |

---

## Styling

Fluent API for colors and styles:

```java
Text.of("Hello")
    .color(Color.GREEN)
    .bold()
    .underline()
```

Supported:
- 16 standard terminal colors
- 256-color mode
- True color (RGB) where supported by the terminal
- Bold, italic, underline, dim, strikethrough

---

## Architecture

```
App (entry point, event loop)
 └── Root Component
      └── Virtual Tree (Node)
           └── Renderer (diff + terminal output)
                └── Terminal Backend (Lanterna or Jansi)
```

1. **App** runs the event loop — reads keyboard events and triggers re-renders
2. **Component** builds a Virtual Tree via `render()`
3. **Differ** compares the new and old trees
4. **Renderer** outputs only the changed terminal cells

---

## Out of Scope

- Mouse support (for now)
- Tabs as a built-in primitive (can be built with components)
- Timer-based animations (may be added later)
- Full CSS-like styling engine

---

## Tech Stack

- **Java 17+**
- **Lanterna** or **Jansi** as the terminal backend (abstracted behind an interface)
- **Maven** or **Gradle** for build
- No external dependencies for the end user beyond the backend library

---

## Example App

```java
public class App {
    public static void main(String[] args) {
        AliveJTUI.run(new MyApp());
    }
}

public class MyApp extends Component {
    private String input = "";
    private List<String> items = new ArrayList<>();

    @Override
    public Node render() {
        return VBox.of(
            Text.of("Todo List").bold().color(Color.CYAN),
            Divider.horizontal(),
            new ItemList(items),
            HBox.of(
                Input.of(input, val -> setState(() -> input = val)),
                Button.of("Add", () -> setState(() -> {
                    items.add(input);
                    input = "";
                }))
            )
        );
    }
}
```

---

## Goal

Make terminal applications in Java as pleasant to write as web components —
no manual cursor control, no `System.out.print("\033[2J")`, no pain.
