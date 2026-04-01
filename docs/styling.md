# Styling

AliveJTUI provides a layered styling system: individual node methods for quick styling, a `Style` object for reusable styles, a `Color` enum/factory for colors, a `Theme` for application-wide semantic colors, and a `StyleSheet` for CSS-like selector-based styling.

---

## Style

`Style` is an immutable value object. Build from `Style.DEFAULT` using `with*` methods:

```java
// Single attribute
Style bold  = Style.DEFAULT.withBold(true);
Style dim   = Style.DEFAULT.withDim(true);
Style under = Style.DEFAULT.withUnderline(true);

// Compound style
Style fancy = Style.DEFAULT
    .withForeground(Color.CYAN)
    .withBackground(Color.BRIGHT_BLACK)
    .withBold(true)
    .withItalic(true);
```

### Applying a Style to a Node

```java
// Full style object
Text.of("hello").style(fancy);

// Shorthand methods (equivalent)
Text.of("hello")
    .bold()
    .italic()
    .color(Color.CYAN)
    .background(Color.BRIGHT_BLACK);
```

!!! tip "Shorthand vs Style object"
    For one-offs, the shorthand methods are cleaner. For reusable styles shared across many nodes, define a `Style` constant and apply it with `.style(myStyle)`.

---

## Color

### Standard ANSI 16 Colors

```java
// Dark variants
Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW,
Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE

// Bright variants
Color.BRIGHT_BLACK, Color.BRIGHT_RED, Color.BRIGHT_GREEN,
Color.BRIGHT_YELLOW, Color.BRIGHT_BLUE, Color.BRIGHT_MAGENTA,
Color.BRIGHT_CYAN, Color.BRIGHT_WHITE
```

### 256-Color Palette

```java
Color.ansi256(202)   // orange
Color.ansi256(93)    // purple
Color.ansi256(46)    // bright green
```

256-color indices follow the standard xterm-256color table.

### True Color (24-bit RGB)

```java
Color.rgb(255, 128, 0)    // orange
Color.rgb(0, 200, 255)    // sky blue
Color.rgb(180, 0, 180)    // purple
```

!!! warning "Terminal support"
    True color (`Color.rgb(...)`) requires a terminal that supports 24-bit color (most modern terminals do). On terminals that don't, AliveJTUI falls back gracefully to the nearest ANSI color.

---

## Theme

The theme system provides semantic color roles so your UI adapts consistently when the user switches between dark and light modes.

### Switching Themes

```java
AliveJTUI.setTheme(Theme.DARK);   // default
AliveJTUI.setTheme(Theme.LIGHT);
```

### Using Theme Colors in render()

```java
@Override
public Node render() {
    Theme t = AliveJTUI.getTheme();
    return VBox.of(
        Text.of("Title").style(t.primary()),
        Text.of("Subtitle").style(t.muted()),
        Text.of("All good").style(t.success()),
        Text.of("Be careful").style(t.warning()),
        Text.of("Something failed").style(t.error()),
        Text.of("Focused item").style(t.focused())
    );
}
```

### Semantic Roles

| Method | Purpose |
|--------|---------|
| `t.foreground()` | Default text |
| `t.muted()` | Secondary / hint text |
| `t.primary()` | Primary highlight (headings, active items) |
| `t.secondary()` | Secondary highlight |
| `t.success()` | Success state (green tones) |
| `t.warning()` | Warning state (yellow tones) |
| `t.error()` | Error state (red tones) |
| `t.focused()` | Focus indicator |

### Custom Theme

```java
Theme myTheme = new Theme.BuiltinTheme(
    Style.DEFAULT,                                              // foreground
    Style.DEFAULT.withDim(true),                               // muted
    Style.DEFAULT.withForeground(Color.rgb(0, 200, 255))
                 .withBold(true),                              // primary
    Style.DEFAULT.withForeground(Color.MAGENTA),               // secondary
    Style.DEFAULT.withForeground(Color.GREEN),                 // success
    Style.DEFAULT.withForeground(Color.YELLOW),                // warning
    Style.DEFAULT.withForeground(Color.RED),                   // error
    Style.DEFAULT.withForeground(Color.CYAN).withBold(true)    // focused
);

AliveJTUI.setTheme(myTheme);
```

!!! info "Theme at runtime"
    You can call `AliveJTUI.setTheme()` at any time — for example, in response to a keyboard shortcut or a settings toggle. The next render cycle picks up the new theme automatically.

---

## StyleSheet (CSS-like selectors)

`StyleSheet` lets you define styles centrally and apply them to a node tree using selectors — similar to CSS.

### Creating a StyleSheet

```java
StyleSheet sheet = new StyleSheet()
    .add(Selector.byId("title"),            Style.DEFAULT.withForeground(Color.CYAN).withBold(true))
    .add(Selector.byClass("muted"),         Style.DEFAULT.withDim(true))
    .add(Selector.byClass("danger"),        Style.DEFAULT.withForeground(Color.RED).withBold(true))
    .add(Selector.byType(ButtonNode.class), Style.DEFAULT.withForeground(Color.YELLOW));
```

### Tagging Nodes

```java
Text.of("My App").withId("title")           // matches Selector.byId("title")
Text.of("Press ESC to quit").withClassName("muted")  // matches Selector.byClass("muted")
Button.of("Delete", action)                  // matches Selector.byType(ButtonNode.class)
```

### Applying to a Tree

```java
@Override
public Node render() {
    Node root = VBox.of(
        Text.of("My App").withId("title"),
        Text.of("Press ESC to quit").withClassName("muted"),
        Button.of("[Delete]", this::deleteAction).withClassName("danger")
    );

    sheet.applyToTree(root);  // walk the tree and apply matching styles
    return root;
}
```

### Selector Types

| Selector | Matches |
|----------|---------|
| `Selector.byId("name")` | Node where `withId("name")` was called |
| `Selector.byClass("name")` | Node where `withClassName("name")` was called |
| `Selector.byType(ButtonNode.class)` | All nodes of the given type |

!!! tip "Selector specificity"
    Styles are applied in declaration order. Later rules override earlier ones, similar to CSS cascade. For important overrides, just add your rule last.

---

## Putting It All Together

```java
public class StyledApp extends Component {

    private static final StyleSheet SHEET = new StyleSheet()
        .add(Selector.byId("header"),    Style.DEFAULT.withForeground(Color.CYAN).withBold(true))
        .add(Selector.byClass("hint"),   Style.DEFAULT.withDim(true))
        .add(Selector.byType(ButtonNode.class), Style.DEFAULT.withForeground(Color.YELLOW));

    @Override
    public Node render() {
        Theme t = AliveJTUI.getTheme();
        Node root = VBox.of(
            Text.of("  My Application").withId("header"),
            Divider.horizontal(),
            Text.of("  Use arrow keys to navigate").withClassName("hint"),
            HBox.of(
                Button.of("[ OK ]",     this::onOk),
                Button.of("[ Cancel ]", this::onCancel)
            )
        );
        SHEET.applyToTree(root);
        return root;
    }
}
```
