# Testing

AliveJTUI is designed to be testable without a real terminal. The `MockBackend` provides a virtual 2D screen buffer and a key event queue, so you can write deterministic unit tests for your components.

---

## MockBackend

`MockBackend` simulates a terminal of a given size. It captures rendered output in a cell buffer and lets you inject key events programmatically.

### Basic Setup

```java
import io.github.yehorsyrin.tui.backend.MockBackend;
import io.github.yehorsyrin.tui.core.AliveJTUI;
import io.github.yehorsyrin.tui.event.*;

MockBackend backend = new MockBackend(80, 24);  // 80 cols × 24 rows
AliveJTUI.run(new MyApp(), backend);
```

### Simulating Key Presses

```java
// Named key
backend.sendKey(KeyEvent.of(KeyType.ARROW_DOWN));
backend.sendKey(KeyEvent.of(KeyType.ENTER));
backend.sendKey(KeyEvent.of(KeyType.ESCAPE));

// Printable character
backend.sendKey(KeyEvent.ofCharacter('x'));
backend.sendKey(KeyEvent.ofCharacter('A'));
```

### Inspecting Rendered Output

```java
// Character at column=0, row=0
String cell = backend.getCell(0, 0);

// Check a specific cell
assertEquals("H", backend.getCell(0, 1));
```

---

## Writing Unit Tests

### JUnit 5 Example

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CounterAppTest {

    private MockBackend backend;

    @BeforeEach
    void setUp() {
        backend = new MockBackend(80, 24);
        AliveJTUI.run(new CounterApp(), backend);
    }

    @Test
    void initialRenderShowsZero() {
        // The counter starts at 0
        // Find "0" somewhere in the rendered output
        boolean found = false;
        for (int col = 0; col < 80; col++) {
            if ("0".equals(backend.getCell(col, 2))) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected '0' to appear in the initial render");
    }

    @Test
    void arrowUpIncrementsCounter() {
        backend.sendKey(KeyEvent.of(KeyType.ARROW_UP));
        backend.sendKey(KeyEvent.of(KeyType.ARROW_UP));
        backend.sendKey(KeyEvent.of(KeyType.ARROW_UP));

        // Counter should now be 3
        boolean found = false;
        for (int col = 0; col < 80; col++) {
            if ("3".equals(backend.getCell(col, 2))) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected '3' after three ARROW_UP presses");
    }

    @Test
    void arrowDownDecrementsCounter() {
        backend.sendKey(KeyEvent.of(KeyType.ARROW_DOWN));

        boolean found = false;
        for (int col = 0; col < 80; col++) {
            if ("-1".equals(backend.getCell(col, 2))) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected '-1' after one ARROW_DOWN press");
    }
}
```

---

## Testing Patterns

### Test State Changes via Key Events

The most reliable way to test a component is to simulate key presses and verify that the rendered output changes accordingly.

```java
@Test
void checkboxToggles() {
    MockBackend backend = new MockBackend(80, 24);
    AliveJTUI.run(new SettingsApp(), backend);

    // Verify initially unchecked: look for ☐
    // Press X to toggle
    backend.sendKey(KeyEvent.ofCharacter('x'));
    // Now look for ☑
}
```

### Test Async Operations

`setStateAsync` uses a background thread. In tests, you may need to wait for the state to settle:

```java
@Test
void asyncDataLoads() throws InterruptedException {
    MockBackend backend = new MockBackend(80, 24);
    AliveJTUI.run(new DataApp(), backend);

    // Trigger async load
    backend.sendKey(KeyEvent.of(KeyType.ENTER));

    // Give the background thread time to complete
    Thread.sleep(500);

    // Verify the data appeared in the render
    // ...
}
```

!!! tip "Deterministic async tests"
    For more deterministic async tests, inject a fake data source via constructor or setter that returns immediately, rather than relying on `Thread.sleep`.

### Test Focus Navigation

```java
@Test
void tabCyclesFocus() {
    MockBackend backend = new MockBackend(80, 24);
    AliveJTUI.run(new FormApp(), backend);

    // Press Tab to move focus
    backend.sendKey(KeyEvent.of(KeyType.TAB));
    // Verify the focused element changed
    // (look for focus indicator character in the rendered output)

    backend.sendKey(KeyEvent.of(KeyType.TAB));
    // Second Tab moves to next focusable

    backend.sendKey(KeyEvent.of(KeyType.SHIFT_TAB));
    // Shift+Tab moves back
}
```

### Test Dialog Interactions

```java
@Test
void dialogConfirmationDeletesItem() {
    MockBackend backend = new MockBackend(80, 24);
    AliveJTUI.run(new ListApp(), backend);

    // Open dialog with D
    backend.sendKey(KeyEvent.ofCharacter('d'));

    // Confirm with Enter (assumes [Yes] button is focused)
    backend.sendKey(KeyEvent.of(KeyType.ENTER));

    // Verify item was deleted — check rendered output
}
```

---

## Available Backends

| Backend | Description |
|---------|-------------|
| `LanternaBackend` | Default. Opens a Swing window when a display is available; falls back to in-terminal mode on headless Linux. |
| `MockBackend` | For unit testing. No real terminal required. Captures output in a cell buffer. |
| Custom | Implement `TerminalBackend` for any other rendering target. |

```java
// Default
AliveJTUI.run(new MyApp());

// Testing
AliveJTUI.run(new MyApp(), new MockBackend(80, 24));

// Custom
AliveJTUI.run(new MyApp(), new MyCustomBackend());
```

---

## Tips for Testable Components

!!! tip "Keep render() pure"
    `render()` should only read state fields — no I/O, no side effects. Pure render functions are easy to reason about in tests.

!!! tip "Inject dependencies"
    Pass services (data loaders, clocks, etc.) via constructor. Replace them with test doubles in unit tests.

!!! tip "Small components"
    Large monolithic components are harder to test. Break complex UIs into smaller components, each responsible for one part of the screen.

!!! tip "Test the component, not the pixels"
    Where possible, test behavior (state transitions, callback invocations) rather than exact pixel positions in the cell buffer.
