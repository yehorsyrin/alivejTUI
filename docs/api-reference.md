# API Reference

Quick-reference tables for the most commonly used classes and methods. For detailed explanations and code examples, see the relevant documentation pages.

---

## AliveJTUI (entry point)

`io.github.yehorsyrin.tui.core.AliveJTUI`

| Method | Description |
|--------|-------------|
| `AliveJTUI.run(component)` | Start the app with the default `LanternaBackend` |
| `AliveJTUI.run(component, backend)` | Start the app with a specific backend |
| `AliveJTUI.stop()` | Stop the event loop and exit |
| `AliveJTUI.setTheme(theme)` | Change the active theme at runtime |
| `AliveJTUI.getTheme()` | Get the current `Theme` |
| `AliveJTUI.pushOverlay(node)` | Push a node as a full-screen overlay |
| `AliveJTUI.popOverlay()` | Remove the topmost overlay |
| `AliveJTUI.schedule(delayMs, task)` | Run `task` once after `delayMs` milliseconds |
| `AliveJTUI.scheduleRepeating(intervalMs, task)` | Run `task` every `intervalMs` milliseconds |
| `AliveJTUI.cancelTimer(task)` | Cancel a previously scheduled task |
| `AliveJTUI.runAsync(asyncTask)` | Execute an `AsyncTask` on a background thread |

---

## Component

`io.github.yehorsyrin.tui.core.Component`

| Method | Override? | Description |
|--------|-----------|-------------|
| `mount(onStateChange, eventBus)` | Yes | Called when component enters the UI tree. Register key handlers here. Always call `super.mount(...)` first. |
| `render()` | **Required** | Return a `Node` tree representing the current state. Must be pure. |
| `unmount()` | Optional | Called when component leaves the tree. Key handlers auto-removed. |
| `onError(Exception)` | Optional | Return a fallback node if `render()` throws. |
| `shouldUpdate()` | Optional | Return `false` to skip re-render. Default: `true`. |
| `setState(mutation)` | No | Apply mutation and queue re-render. |
| `setStateAsync(task)` | No | Run task on background thread, apply result on event loop. |
| `onKey(keyType, handler)` | No | Register a key handler (auto-removed on unmount). |
| `registerFocusable(node)` | No | Add node to the Tab focus cycle. |

---

## Node Factories

`io.github.yehorsyrin.tui.node.*`

### Text & Display

| Factory | Returns |
|---------|---------|
| `Text.of(string)` | `TextNode` |
| `Text.ofMarkdown(string)` | `TextNode` with inline markdown |
| `Paragraph.of(string)` | `ParagraphNode` (word-wrapped) |
| `Paragraph.ofMarkdown(string)` | `ParagraphNode` with markdown |
| `Divider.horizontal()` | Horizontal rule node |
| `Divider.vertical()` | Vertical rule node |

### Layout

| Factory | Returns |
|---------|---------|
| `VBox.of(Node...)` | `VBoxNode` |
| `VBox.of(Node...).gap(n)` | `VBoxNode` with n-row gap between children |
| `HBox.of(Node...)` | `HBoxNode` |
| `HBox.of(Node...).gap(n)` | `HBoxNode` with n-column gap between children |
| `new BoxNode(child, border, style)` | Bordered container |

### Interactive

| Factory | Returns |
|---------|---------|
| `Button.of(label, action)` | `ButtonNode` |
| `Input.of(value, onChange)` | `InputNode` |
| `TextArea.of(value, rows)` | `TextAreaNode` |
| `Checkbox.of(label, checked, toggle)` | `CheckboxNode` |
| `RadioGroup.of(option...)` | `RadioGroupNode` |
| `Select.of(option...)` | `SelectNode` |

### Lists & Tables

| Factory | Returns |
|---------|---------|
| `Table.of(headers, rows, visibleRows)` | `TableNode` |
| `VirtualList.of(items, visibleRows)` | `VirtualListNode` |
| `Viewport.of(content, visibleRows)` | `ViewportNode` |

### Other

| Factory | Returns |
|---------|---------|
| `new ProgressBarNode(progress)` | `ProgressBarNode` |
| `Spinner.of()` | `SpinnerNode` (default frames) |
| `Spinner.of(frames[])` | `SpinnerNode` (custom frames) |
| `Collapsible.of(title, children...)` | `CollapsibleNode` (collapsed) |
| `Collapsible.expanded(title, children...)` | `CollapsibleNode` (expanded) |
| `Dialog.of(title, content)` | `DialogNode` |
| `HelpPanel.of(KeyBinding...)` | `HelpPanelNode` |

---

## Style

`io.github.yehorsyrin.tui.style.Style`

| Method | Description |
|--------|-------------|
| `Style.DEFAULT` | Base style with no attributes set |
| `.withForeground(Color)` | Set foreground color |
| `.withBackground(Color)` | Set background color |
| `.withBold(boolean)` | Set bold |
| `.withItalic(boolean)` | Set italic |
| `.withUnderline(boolean)` | Set underline |
| `.withStrikethrough(boolean)` | Set strikethrough |
| `.withDim(boolean)` | Set dim/muted |

---

## Color

`io.github.yehorsyrin.tui.style.Color`

| Expression | Description |
|------------|-------------|
| `Color.RED` / `Color.BRIGHT_RED` / ... | Standard ANSI 16 colors |
| `Color.ansi256(n)` | 256-color palette (0–255) |
| `Color.rgb(r, g, b)` | 24-bit true color |

---

## Theme

`io.github.yehorsyrin.tui.style.Theme`

| Expression | Description |
|------------|-------------|
| `Theme.DARK` | Built-in dark theme |
| `Theme.LIGHT` | Built-in light theme |
| `theme.foreground()` | Default text style |
| `theme.muted()` | Secondary / hint text style |
| `theme.primary()` | Primary highlight style |
| `theme.secondary()` | Secondary highlight style |
| `theme.success()` | Success state style |
| `theme.warning()` | Warning state style |
| `theme.error()` | Error state style |
| `theme.focused()` | Focus indicator style |

---

## StyleSheet & Selector

`io.github.yehorsyrin.tui.style.StyleSheet` / `Selector`

| Expression | Description |
|------------|-------------|
| `new StyleSheet()` | Create an empty stylesheet |
| `.add(selector, style)` | Add a style rule |
| `.applyToTree(rootNode)` | Apply rules to a node tree |
| `Selector.byId("name")` | Match by node ID |
| `Selector.byClass("name")` | Match by CSS class name |
| `Selector.byType(NodeClass.class)` | Match by node type |

---

## UndoManager

`io.github.yehorsyrin.tui.core.UndoManager`

| Method | Description |
|--------|-------------|
| `new UndoManager()` | Create with default capacity (100) |
| `new UndoManager(n)` | Create with capacity n |
| `.record(undoAction, redoAction)` | Push an undo/redo pair onto the stack |
| `.undo()` | Execute the undo action |
| `.redo()` | Execute the redo action |
| `.canUndo()` | Whether the undo stack is non-empty |
| `.canRedo()` | Whether the redo stack is non-empty |
| `.clear()` | Wipe both stacks |

---

## NotificationManager

`io.github.yehorsyrin.tui.core.NotificationManager`

| Method | Description |
|--------|-------------|
| `new NotificationManager(stateChange)` | Create with a state-change trigger |
| `.show(message, durationMs)` | Show an INFO notification |
| `.show(message, durationMs, type)` | Show a typed notification |
| `.buildOverlay()` | Build the overlay node (or `null` if none active) |

**Notification types:** `NotificationType.INFO`, `SUCCESS`, `WARNING`, `ERROR`

---

## MockBackend (testing)

`io.github.yehorsyrin.tui.backend.MockBackend`

| Method | Description |
|--------|-------------|
| `new MockBackend(cols, rows)` | Create a virtual terminal |
| `.sendKey(KeyEvent)` | Inject a key event |
| `.getCell(col, row)` | Get the character at the given position |

---

## KeyEvent

`io.github.yehorsyrin.tui.event.KeyEvent`

| Expression | Description |
|------------|-------------|
| `KeyEvent.of(KeyType)` | Create a key event for a named key |
| `KeyEvent.ofCharacter(char)` | Create a CHARACTER key event |
| `event.keyType()` | Get the `KeyType` |
| `event.character()` | Get the character (for `CHARACTER` events) |
| `event.ctrl()` | Whether Ctrl modifier was held |

---

## KeyType Values

`io.github.yehorsyrin.tui.event.KeyType`

`CHARACTER`, `ENTER`, `BACKSPACE`, `DELETE`, `ARROW_UP`, `ARROW_DOWN`, `ARROW_LEFT`, `ARROW_RIGHT`, `ESCAPE`, `TAB`, `SHIFT_TAB`, `HOME`, `END`, `PAGE_UP`, `PAGE_DOWN`, `EOF`
