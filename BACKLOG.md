# AliveJTUI — Backlog покращень

> Складено: 2026-03-27 | Jarvis (AI)

---

## Пріоритет: КРИТИЧНИЙ (потрібно для інтерактивних додатків)

### TASK-11: Focus Management System
**Суть:** Централізований `FocusManager` — реєструє focusable-компоненти (Button, Input),
зберігає список у порядку додавання, надає `focusNext()` / `focusPrev()` / `focusById(id)`.
`ButtonNode.isFocused` і `InputNode.isFocused` керуються автоматично фреймворком.
**Файли:** новий `core/FocusManager.java`, зміни в `AliveJTUI`, `Component`, `ButtonNode`, `InputNode`
**Тести:** FocusManagerTest — реєстрація, цикл focus, null safety

---

### TASK-12: Tab / Shift+Tab Navigation
**Суть:** TAB → `focusManager.focusNext()`, Shift+TAB → `focusPrev()`.
TAB вже є в `KeyType`. Shift+TAB потребує або нового KeyType, або modifier-flag в KeyEvent.
Потребує TASK-11.
**Файли:** `AliveJTUI` (реєстрація TAB-обробника), `KeyEvent`, можливо `KeyType`
**Тести:** інтеграційний тест з 3+ focusable компонентами

---

### TASK-13: Cursor Position Management
**Суть:** Коли `InputNode` у фокусі — `Renderer` позиціонує курсор у поточну позицію символу
(`inputNode.x + cursorPos`, `inputNode.y`). `showCursor()` / `hideCursor()` залежить від фокусу.
Поточна проблема: `showCursor()` є no-op у Lanterna.
Workaround: Lanterna `setCursor(col, row)` — позиціонує + показує.
**Файли:** `Renderer`, `LanternaBackend` (реалізувати showCursor через setCursor), `TerminalBackend`
**Тести:** Renderer тест з фіктивним InputNode у фокусі

---

### TASK-14: Event Consumption (Stop Propagation)
**Суть:** Зараз всі хендлери глобальні. Потрібен механізм "спожити подію":
`KeyHandler` повертає `boolean` (true = consumed, зупинити chain).
`EventBus.dispatch()` зупиняється при першому `true`.
**Файли:** `EventBus`, `KeyEvent`, інтерфейс `KeyHandler`
**Тести:** EventBusTest — consumption chain

---

## Пріоритет: СЕРЕДНІЙ (якість і виправлення)

### TASK-15: Fix — Dim Style (Lanterna SGR.FAINT відсутній)
**Суть:** `Style.dim()` силентно ігнорується. Опції:
a) Lanterna 3.1.3+ — перевірити чи додали FAINT
b) Workaround: dim = темніший кастомний колір
c) Javadoc-попередження + `TerminalCapabilities.supportsDim()`
**Файли:** `LanternaBackend`, `Style` (Javadoc)

---

### TASK-16: Fix — Duplicate Key Handler Registration
**Суть:** `EventBus.register()` не перевіряє дублікати. При повторному mount/unmount
один і той же обробник може накопичуватись.
**Рішення:** Використовувати `LinkedHashSet` замість `ArrayList`, або перевіряти перед додаванням.
**Файли:** `EventBus`
**Тести:** EventBusTest — double-register should not double-fire

---

### TASK-17: Scrollable Generic Container
**Суть:** `VBoxNode` з `maxHeight` і `scrollOffset` — прокручує дочірні елементи вертикально.
Аналогічно `HBoxNode` — горизонтально. `ListNode` вже має логіку — винести в `ScrollableContainer`.
**Файли:** новий `node/ScrollableVBoxNode.java`, зміни `LayoutEngine`, `TreeFlattener`
**Тести:** ScrollableContainerTest

---

### TASK-18: Modal / Overlay Support
**Суть:** Шар поверх основного дерева. `AliveJTUI.pushOverlay(node)` / `popOverlay()`.
Рендерер малює overlay поверх основного вмісту.
**Файли:** `AliveJTUI`, `Renderer`, `LayoutEngine`
**Тести:** RendererTest з overlay

---

### TASK-19: Покращення тестового покриття
**Суть (підзадачі):**
- InputNode — симуляція друку символів, backspace, курсор
- Differ — глибоке дерево, unicode символи
- LayoutEngine — мала консоль (< 10 символів ширина), вузли більші за екран
- Повний пайплайн: layout → diff → render (інтеграційний)
- TodoList/Showcase — coverage gap
**Файли:** нові тести у відповідних пакетах

---

## Пріоритет: НИЗЬКИЙ (майбутнє)

### TASK-20: Timer / Animation System
**Суть:** `AliveJTUI.schedule(delay, Runnable)` — ставить задачу у чергу event loop.
Без зовнішніх потоків — poll у циклі. Потрібно для Spinner автоанімації.
**Файли:** `AliveJTUI` (event loop зміна)

---

### TASK-21: Modifier Keys (Ctrl, Alt combinations)
**Суть:** `KeyEvent` додає поля `ctrl: boolean`, `alt: boolean`, `shift: boolean`.
Lanterna `KeyStroke.isCtrlDown()` / `isAltDown()` / `isShiftDown()` — вже доступні.
**Файли:** `KeyEvent`, `LanternaBackend`, `KeyType`
**Тести:** KeyEventTest, LanternaBackendConversionTest

---

### TASK-22: shouldUpdate() / Component Memoization
**Суть:** `Component.shouldUpdate(oldState, newState): boolean` — дозволяє пропустити re-render.
За замовчуванням `true`. Для оптимізації великих дерев.
**Файли:** `Component`, `AliveJTUI` (перевірка перед рендером)

---

### TASK-23: Error Boundaries
**Суть:** `Component.onError(Exception): Node` — catch у render(), повертає fallback вузол.
Без цього будь-яке виключення вбиває весь event loop.
**Файли:** `AliveJTUI` (try-catch навколо render), `Component`

---

### TASK-24: Async State Updates
**Суть:** `Component.setStateAsync(supplier)` — виконує постачальника стану в іншому потоці,
результат застосовується в event loop потоці через чергу. Потрібно для мережевих запитів.
**Файли:** `Component`, `AliveJTUI` (async queue у event loop)

---

## Порядок виконання (рекомендований)

```
TASK-16 (quick fix, ізольований)             ✅ DONE
  → TASK-11 (FocusManager — фундамент)        ✅ DONE
    → TASK-12 (Tab nav — залежить від TASK-11) ✅ DONE
    → TASK-13 (Cursor — залежить від TASK-11)  ✅ DONE
  → TASK-14 (Event consumption — ізольований) ✅ DONE
  → TASK-15 (Dim fix — ізольований)           ✅ DONE
  → TASK-19 (Тести — паралельно з будь-чим)   ✅ DONE
  → TASK-17 (Scrollable container)             ✅ DONE
  → TASK-18 (Modal)                            ✅ DONE
  → TASK-20 (Timer/Animation)                  ✅ DONE
  → TASK-21 (Modifier keys)                    ✅ DONE
  → TASK-22 (shouldUpdate/Memoization)          ✅ DONE
  → TASK-23 (Error Boundaries)                  ✅ DONE
  → TASK-24 (Async State)                       ✅ DONE
```

---

## Аналіз: AliveJTUI vs Bubble Tea (Go) vs Textual (Python)
> Складено: 2026-03-28 | Jarvis (AI)

### Матриця порівняння

| Функція | AliveJTUI | Bubble Tea | Textual | TASK |
|---------|:---------:|:----------:|:-------:|------|
| Mouse support | ✗ | ✓ | ✓ | TASK-25 |
| Rich Text / Markdown | ✗ | ✓ | ✓ | TASK-26 |
| Multi-line / Word Wrap | ✗ | ✓ | ✓ | TASK-27 |
| Table / DataGrid | ✗ | ✓ | ✓ | TASK-28 |
| Dialog / Alert | ✗ | custom | ✓ | TASK-29 |
| TextArea (multi-line input) | ✗ | ✓ | ✓ | TASK-30 |
| Viewport / ScrollContainer | partial | ✓ | ✓ | TASK-31 |
| Help / Keybindings panel | ✗ | ✓ | ✓ | TASK-32 |
| Checkbox | ✗ | ✓ | ✓ | TASK-33 |
| Radio Button Group | ✗ | custom | ✓ | TASK-34 |
| Dropdown / Select | ✗ | custom | ✓ | TASK-35 |
| Paragraph / Flow Text | ✗ | ✓ | ✓ | TASK-36 |
| Collapsible / Disclosure | ✗ | custom | ✓ | TASK-37 |
| Async / Background Tasks | planned | ✓ | ✓ | TASK-38 |
| Theme (Light/Dark) | ✗ | custom | ✓ | TASK-39 |
| CSS-like Styling | ✗ | partial | ✓ | TASK-40 |
| Alt Backends (ncurses etc) | ✗ | ✓ | ✓ | TASK-41 |
| Logging / Debug Mode | ✗ | ✓ | ✓ | TASK-42 |
| Undo / Redo Stack | ✗ | custom | ✓ | TASK-43 |
| Clipboard (copy/paste) | ✗ | custom | custom | TASK-44 |
| Notifications / Toasts | ✗ | custom | ✓ | TASK-45 |
| Virtual Scrolling | ✗ | ✗ | ✓ | TASK-46 |

---

## Пріоритет: КРИТИЧНИЙ (порівняно з Bubble Tea / Textual)

### TASK-25: Mouse Support
**Суть:** Обробка натиснень, руху миші та скролювання. `MouseEvent(type, col, row, button)`.
`EventBus` отримує `registerMouse(MouseHandler)`. Renderer робить hit-test для знаходження
компонента за координатами. Button/Input реагують на клік мишею.
**Є у:** Bubble Tea ✓, Textual ✓
**Файли:** `event/MouseEvent.java` (новий), `event/MouseHandler.java` (новий),
`event/EventBus.java`, `backend/TerminalBackend.java`, `backend/LanternaBackend.java`,
`render/Renderer.java` (hit-test)
**Тести:** MouseEventTest, EventBusMouseTest, RendererHitTestTest
**Пріоритет:** HIGH | **Складність:** HIGH

---

### TASK-26: Rich Text / Markdown Rendering
**Суть:** `Text.of("**bold** and *italic*")` — inline markdown-like синтаксис у текстових
вузлах. `MarkdownParser` повертає список `StyledSegment(text, style)`. `TreeFlattener`
рендерить кожен сегмент окремо зі своїм Style.
**Є у:** Bubble Tea ✓ (lipgloss), Textual ✓
**Файли:** `node/StyledSegment.java` (новий), `node/MarkdownParser.java` (новий),
`node/TextNode.java`, `diff/TreeFlattener.java`
**Тести:** MarkdownParserTest, StyledSegmentTest, TextNodeMarkdownTest
**Пріоритет:** HIGH | **Складність:** MED

---

### TASK-27: Multi-line Text / Word Wrap
**Суть:** `TextNode` зараз — один рядок. Додати `TextNode.wrap(true)` або окремий
`MultilineTextNode`. `LayoutEngine` розбиває текст по ширині контейнера. `\n` обробляється.
**Є у:** Bubble Tea ✓, Textual ✓
**Файли:** `node/TextNode.java` (або `node/MultilineTextNode.java`),
`layout/LayoutEngine.java`, `diff/TreeFlattener.java`
**Тести:** TextNodeWordWrapTest, LayoutEngineMultilineTest
**Пріоритет:** HIGH | **Складність:** MED

---

### TASK-28: Table / Grid Component
**Суть:** `TableNode(headers, rows)` з auto/fixed шириною колонок, vertical scroll,
row selection. Rendering із border-символами (`─ │ ┼`).
**Є у:** Bubble Tea ✓ (table bubble), Textual ✓ (DataTable)
**Файли:** `node/TableNode.java` (новий), `layout/LayoutEngine.java`, `diff/TreeFlattener.java`
**Тести:** TableNodeTest, TableLayoutTest, TableSelectionTest
**Залежить від:** TASK-17 (Scrollable)
**Пріоритет:** HIGH | **Складність:** HIGH

---

### TASK-29: Dialog / Alert Box
**Суть:** `DialogNode(title, message, buttons)` — модальне вікно, центроване на екрані.
ESC / Enter закриває. `AliveJTUI.pushOverlay(node)` для відображення.
**Є у:** Bubble Tea (custom), Textual ✓
**Файли:** `node/DialogNode.java` (новий), `core/AliveJTUI.java`
**Тести:** DialogNodeTest, DialogLayoutTest
**Залежить від:** TASK-18 (Modal overlay)
**Пріоритет:** HIGH | **Складність:** MED

---

## Пріоритет: СЕРЕДНІЙ (widget library)

### TASK-30: TextArea Component (Multi-line Input)
**Суть:** `TextAreaNode(text, onChange)` — багаторядковий ввід з row:col курсором.
Arrow keys, Home/End, Del/Backspace, Page Up/Down.
**Є у:** Bubble Tea ✓ (textarea bubble), Textual ✓
**Файли:** `node/TextAreaNode.java` (новий), `layout/LayoutEngine.java`, `diff/TreeFlattener.java`
**Тести:** TextAreaNodeTest, TextAreaCursorTest
**Залежить від:** TASK-12, TASK-13
**Пріоритет:** MED | **Складність:** HIGH

---

### TASK-31: Viewport / Generic ScrollContainer
**Суть:** `ViewportNode(content, height)` — скролювання будь-якого вмісту з scroll-bar
рендерингом (`▓` / `│`). Keyboard: arrows, Page Up/Down.
**Є у:** Bubble Tea ✓ (viewport bubble), Textual ✓
**Файли:** `node/ViewportNode.java` (новий), `layout/LayoutEngine.java`, `diff/TreeFlattener.java`
**Тести:** ViewportNodeTest, ViewportScrollTest
**Залежить від:** TASK-17
**Пріоритет:** MED | **Складність:** MED

---

### TASK-32: Help Panel / Keybindings Display
**Суть:** `HelpPanelNode` відображає зареєстровані keybindings. `EventBus` надає API
для introspection. `Component.describeBindings()` — optional override.
**Є у:** Bubble Tea ✓ (help bubble), Textual ✓
**Файли:** `node/HelpPanelNode.java` (новий), `event/EventBus.java`
**Тести:** HelpPanelNodeTest, EventBusIntrospectionTest
**Пріоритет:** MED | **Складність:** LOW

---

### TASK-33: Checkbox Component
**Суть:** `CheckboxNode(label, isChecked, onChange)`. Вигляд: `[✓] Label` / `[ ] Label`.
Space/Enter toggles. Implements `Focusable`.
**Є у:** Bubble Tea ✓ (checkbox bubble), Textual ✓
**Файли:** `node/CheckboxNode.java` (новий)
**Тести:** CheckboxNodeTest, CheckboxToggleTest
**Залежить від:** TASK-11
**Пріоритет:** MED | **Складність:** LOW

---

### TASK-34: Radio Button Group
**Суть:** `RadioGroupNode(options, selectedIndex, onChange)`. `(●) Opt1  ( ) Opt2`.
Arrows для вибору між options. Фокусується як одна група.
**Є у:** Bubble Tea (custom), Textual ✓
**Файли:** `node/RadioGroupNode.java` (новий)
**Тести:** RadioGroupNodeTest, RadioSelectionTest
**Залежить від:** TASK-11
**Пріоритет:** MED | **Складність:** MED

---

### TASK-35: Dropdown / Select Component
**Суть:** `SelectNode(options, selectedIndex, onChange)`. `[Option ▾]` — відкривається
overlay-списком. Arrows/Enter для вибору, ESC для закриття.
**Є у:** Bubble Tea (custom), Textual ✓
**Файли:** `node/SelectNode.java` (новий), `core/AliveJTUI.java`
**Тести:** SelectNodeTest, SelectKeyboardTest
**Залежить від:** TASK-18, TASK-17
**Пріоритет:** MED | **Складність:** HIGH

---

### TASK-36: Paragraph / Flow Text
**Суть:** `ParagraphNode(text)` з auto word-wrap і justify (left/center/right).
**Є у:** Bubble Tea ✓ (lipgloss), Textual ✓
**Файли:** `node/ParagraphNode.java` (новий), `layout/LayoutEngine.java`
**Тести:** ParagraphNodeTest, ParagraphJustifyTest
**Залежить від:** TASK-27
**Пріоритет:** MED | **Складність:** MED

---

### TASK-37: Collapsible / Disclosure Component
**Суть:** `CollapsibleNode(title, children, isExpanded)`. `▶ Title` / `▼ Title`.
Space/Enter toggles. Приховує/показує дочірні вузли.
**Є у:** Bubble Tea (custom), Textual ✓
**Файли:** `node/CollapsibleNode.java` (новий), `layout/LayoutEngine.java`
**Тести:** CollapsibleNodeTest, CollapsibleToggleTest
**Залежить від:** TASK-11
**Пріоритет:** MED | **Складність:** LOW

---

## Пріоритет: НИЗЬКИЙ (системний рівень)

### TASK-38: Async Task Runner / Background Jobs
**Суть:** `AliveJTUI.runAsync(callable)` — виконує задачу в thread pool, результат
push'ується в event loop як повідомлення.
**Є у:** Bubble Tea ✓ (tea.Batch), Textual ✓ (Workers)
**Файли:** `core/AsyncTask.java` (новий), `core/AliveJTUI.java`
**Тести:** AsyncTaskTest
**Пріоритет:** LOW | **Складність:** HIGH

---

### TASK-39: Theme System (Light / Dark Mode)
**Суть:** `Theme` interface з palette. `AliveJTUI.setTheme(theme)`. Dark/Light presets.
**Є у:** Bubble Tea (custom), Textual ✓
**Файли:** `style/Theme.java` (новий), `core/AliveJTUI.java`
**Тести:** ThemeTest
**Пріоритет:** LOW | **Складність:** MED

---

### TASK-40: CSS-like Selectors / Class System
**Суть:** `Node.withClassName()`, `Node.withId()`. `StyleSheet` — map selector → Style.
**Є у:** Bubble Tea (partial), Textual ✓
**Файли:** `style/StyleSheet.java` (новий), `style/Selector.java` (новий), `core/Node.java`
**Тести:** StyleSheetTest, SelectorMatchTest
**Пріоритет:** LOW | **Складність:** HIGH

---

### TASK-41: Alternative Backends (ncurses, ANSI, Mock)
**Суть:** `JCursesBackend` для Linux/Mac, `AnsiBackend` для Windows ANSI mode,
`MockBackend` для тестування з захопленням output.
**Є у:** Bubble Tea ✓ (tcell), Textual ✓
**Файли:** `backend/AnsiBackend.java`, `backend/MockBackend.java`
**Тести:** BackendConversionTest для кожного
**Пріоритет:** LOW | **Складність:** HIGH

---

### TASK-42: Logging / Debug Mode
**Суть:** `AliveJTUI.setDebugLogging(true)` — логує events, renders, diffs у файл.
`DebugOverlay` — опціональний HUD з FPS, node count, memory.
**Є у:** Bubble Tea ✓ (tea.LogWriter), Textual ✓
**Файли:** `debug/DebugLogger.java` (новий), `debug/DebugOverlay.java` (новий)
**Тести:** DebugLoggerTest
**Пріоритет:** LOW | **Складність:** MED

---

### TASK-43: Undo / Redo Stack
**Суть:** `UndoManager` відстежує setState-мутації. `Ctrl+Z` / `Ctrl+Y` — undo/redo.
**Є у:** Bubble Tea (via commands), Textual ✓
**Файли:** `core/UndoManager.java` (новий), `core/Component.java`
**Тести:** UndoManagerTest
**Пріоритет:** LOW | **Складність:** MED

---

### TASK-44: Clipboard Support (Copy / Paste)
**Суть:** `Clipboard` utility для системного clipboard. Input/TextArea підтримують Ctrl+C/V.
**Є у:** Bubble Tea (custom), Textual (custom)
**Файли:** `util/Clipboard.java` (новий), `node/InputNode.java`
**Тести:** ClipboardTest
**Залежить від:** TASK-30
**Пріоритет:** LOW | **Складність:** LOW

---

### TASK-45: Notification / Toast System
**Суть:** `Notification(message, duration, type)` — тимчасові повідомлення у куті.
Auto-dismiss через duration. Stack з кількох одночасних.
**Є у:** Bubble Tea (custom), Textual ✓
**Файли:** `node/NotificationNode.java` (новий), `core/NotificationManager.java` (новий)
**Тести:** NotificationManagerTest
**Залежить від:** TASK-20 (Timer)
**Пріоритет:** LOW | **Складність:** MED

---

## Пріоритет: МАЙБУТНЄ (experimental)

### TASK-46: Virtual Scrolling (Lazy Rendering)
**Суть:** `VirtualListNode` — рендер тільки видимих рядків для 10k+ items.
**Є у:** Bubble Tea ✗, Textual ✓ (lazy)
**Залежить від:** TASK-17, TASK-28
**Пріоритет:** FUTURE | **Складність:** VERY_HIGH

### TASK-47: Reactive State Bindings
**Суть:** `@Reactive` на Component fields → auto setState().
**Є у:** Bubble Tea ✗, Textual ✓
**Пріоритет:** FUTURE | **Складність:** HIGH

### TASK-48: Hot Reload (Development Mode)
**Суть:** Перекомпіляція + перезавантаження без рестарту. Dev-only feature.
**Є у:** Bubble Tea ✗, Textual ✗
**Пріоритет:** FUTURE | **Складність:** VERY_HIGH

### TASK-49: Async Network / Web Component
**Суть:** `WebNode(url)` — fetch + TUI-render зовнішнього ресурсу.
**Є у:** Bubble Tea ✗, Textual ✗
**Залежить від:** TASK-38
**Пріоритет:** FUTURE | **Складність:** VERY_HIGH

---

## Порядок виконання (TASK-25 і далі)

```
TASK-26 (Rich Text — самостійна)
TASK-27 (Multi-line — фундамент)
  → TASK-30 (TextArea)
  → TASK-36 (Paragraph)
TASK-25 (Mouse — critical UX)
TASK-28 (Table — high impact)
  → TASK-35 (Dropdown — залежить від TASK-18)
TASK-29 (Dialog — залежить від TASK-18)
TASK-31 (Viewport — розширює TASK-17)
TASK-32 (Help Panel)
TASK-33 (Checkbox)
TASK-34 (Radio Group)
TASK-37 (Collapsible)
TASK-38 (Async tasks)
TASK-39 (Theme)
TASK-40 (CSS selectors)
TASK-41..45 (Системний рівень)
TASK-46..49 (Experimental)
```
