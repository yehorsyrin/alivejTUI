# AliveJTUI — Native Backend Task List

> Goal: replace the Lanterna 3.1.2 dependency with a zero-dependency native terminal backend
> that talks directly to the OS via JNA (Java Native Access).
>
> Reference implementation: `LanternaBackend.java`
> Target interface:        `TerminalBackend.java`
> Package (this branch):   `io.alive.tui.backend`
>
> All tasks are independent unless a "Depends on" line says otherwise.
> Authored by: Jarvis (AI) — 2026-03-29

---

## Status

```
NTASK-01 [ ] JNA dependency + platform detection utility
NTASK-02 [ ] AnsiWriter — ANSI/VT escape sequence output
NTASK-03 [ ] POSIX raw mode (Linux / macOS)
NTASK-04 [ ] Windows raw mode + VT output enable
NTASK-05 [ ] Terminal size detection (POSIX + Windows)
NTASK-06 [ ] Resize detection (POSIX SIGWINCH + Windows polling)
NTASK-07 [ ] ANSI key decoder — stdin bytes → KeyEvent
NTASK-08 [ ] NativeBackend — assembles NTASK-02..07
NTASK-09 [ ] Backends factory — platform-aware creation methods
NTASK-10 [ ] Unit and integration tests
```

---

## NTASK-01 — JNA dependency + platform detection utility

### Goal
Add JNA to the project and create a utility class that the rest of the native backend
components use to detect the current OS at runtime.

### Background
Java Native Access (JNA) allows calling native C/Win32 functions without writing JNI glue code.
We need it to call POSIX `tcgetattr`/`tcsetattr`/`ioctl` on Linux and macOS, and Win32
`GetConsoleMode`/`SetConsoleMode`/`GetConsoleScreenBufferInfo` on Windows.

JNA comes in two artifacts:
- `net.java.dev.jna:jna` — core
- `net.java.dev.jna:jna-platform` — pre-built bindings for Kernel32, Libc, etc.

Both must be added. Use the latest stable release (≥ 5.14.0).

### Files to create / modify
- `pom.xml` — add two `<dependency>` entries
- `src/main/java/io/alive/tui/backend/OsPlatform.java` — new utility class

### Implementation notes

**pom.xml** — add inside `<dependencies>`:
```xml
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.14.0</version>
</dependency>
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna-platform</artifactId>
    <version>5.14.0</version>
</dependency>
```

**OsPlatform.java**:
```java
package io.alive.tui.backend;

public final class OsPlatform {
    public enum OS { WINDOWS, LINUX, MACOS, OTHER }

    private static final OS CURRENT;

    static {
        String name = System.getProperty("os.name", "").toLowerCase();
        if (name.contains("win"))    CURRENT = OS.WINDOWS;
        else if (name.contains("mac")) CURRENT = OS.MACOS;
        else if (name.contains("nux") || name.contains("nix")) CURRENT = OS.LINUX;
        else CURRENT = OS.OTHER;
    }

    public static OS get() { return CURRENT; }
    public static boolean isWindows() { return CURRENT == OS.WINDOWS; }
    public static boolean isPosix()   { return CURRENT == OS.LINUX || CURRENT == OS.MACOS; }
    private OsPlatform() {}
}
```

### Tests
- `OsPlatformTest` — assert that `get()` returns a non-null value and `isWindows()` /
  `isPosix()` are mutually exclusive on any given platform (one must be true, not both).

---

## NTASK-02 — AnsiWriter — ANSI/VT escape sequence output

### Goal
Create a class that writes ANSI/VT100 escape sequences to `System.out` (or a provided
`OutputStream`). This is the only output layer; all rendering goes through it.

### Background
The terminal is controlled via escape sequences written to stdout. VT100/ANSI sequences are:
- `ESC[{row};{col}H`   — move cursor to (row, col), 1-based
- `ESC[?25l`           — hide cursor
- `ESC[?25h`           — show cursor
- `ESC[?1049h`         — enter alternate screen buffer
- `ESC[?1049l`         — exit alternate screen buffer
- `ESC[2J`             — clear entire screen
- `ESC[0m`             — reset all SGR attributes
- `ESC[{...}m`         — set SGR attributes (see implementation notes)

SGR (Select Graphic Rendition) codes for `Style`:
- Bold:          `1`
- Italic:        `3`
- Underline:     `4`
- Dim (faint):   `2`   ← Lanterna did NOT support this; we do
- Strikethrough: `9`
- Foreground color, ANSI 16: `30`–`37` (normal), `90`–`97` (bright)
- Background color, ANSI 16: `40`–`47` (normal), `100`–`107` (bright)
- Foreground color, 256: `38;5;{n}`
- Background color, 256: `48;5;{n}`
- Foreground color, RGB:  `38;2;{r};{g};{b}`
- Background color, RGB:  `48;2;{r};{g};{b}`
- Reset: `0`

### Files to create
- `src/main/java/io/alive/tui/backend/AnsiWriter.java`

### Implementation notes

```java
package io.alive.tui.backend;

import io.alive.tui.style.Color;
import io.alive.tui.style.Style;

import java.io.OutputStream;
import java.io.PrintStream;

public final class AnsiWriter {

    private final PrintStream out;

    public AnsiWriter() { this(System.out); }
    public AnsiWriter(OutputStream out) { this.out = new PrintStream(out, false, java.nio.charset.StandardCharsets.UTF_8); }

    public void enterAlternateScreen() { esc("[?1049h"); }
    public void exitAlternateScreen()  { esc("[?1049l"); }
    public void hideCursor()           { esc("[?25l");  }
    public void showCursor()           { esc("[?25h");  }
    public void clearScreen()          { esc("[2J");    }

    /** Move cursor to (col, row), both 0-based. Internally converts to 1-based. */
    public void moveTo(int col, int row) {
        esc("[" + (row + 1) + ";" + (col + 1) + "H");
    }

    /** Apply style. Call before writing the character. Emit ESC[0m first to reset. */
    public void applyStyle(Style style) {
        StringBuilder sb = new StringBuilder();
        sb.append("0");  // reset
        if (style != null) {
            if (style.isBold())          sb.append(";1");
            if (style.isDim())           sb.append(";2");
            if (style.isItalic())        sb.append(";3");
            if (style.isUnderline())     sb.append(";4");
            if (style.isStrikethrough()) sb.append(";9");
            appendColor(sb, style.getForeground(), true);
            appendColor(sb, style.getBackground(), false);
        }
        esc("[" + sb + "m");
    }

    public void writeChar(char c) { out.print(c); }

    public void flush() { out.flush(); }

    private void esc(String seq) { out.print('\033' + seq); }

    private void appendColor(StringBuilder sb, Color c, boolean fg) {
        if (c == null) return;
        switch (c.getType()) {
            case ANSI_16 -> {
                int base = fg ? 30 : 40;
                int idx  = c.getAnsiIndex();
                sb.append(";").append(idx < 8 ? base + idx : (base + 60 + idx - 8));
            }
            case ANSI_256 -> sb.append(";").append(fg ? "38;5;" : "48;5;").append(c.getAnsiIndex());
            case RGB      -> sb.append(";").append(fg ? "38;2;" : "48;2;")
                               .append(c.getR()).append(";").append(c.getG()).append(";").append(c.getB());
        }
    }
}
```

### API contract
- `enterAlternateScreen()` / `exitAlternateScreen()` — must be called in `NativeBackend.init()`
  and `shutdown()` respectively.
- `applyStyle(null)` must emit only `ESC[0m` (reset, no other codes).
- `moveTo(0, 0)` must emit `ESC[1;1H`.
- `flush()` flushes the underlying stream; call at the end of every render cycle.

### Tests
- Capture stdout via `ByteArrayOutputStream`.
- Assert exact escape sequences for: moveTo(0,0), moveTo(5,3), hideCursor, showCursor,
  clearScreen, enterAlternateScreen, exitAlternateScreen.
- Assert `applyStyle(null)` emits only `\033[0m`.
- Assert bold+red foreground emits `\033[0;1;31m`.
- Assert RGB foreground emits `\033[0;38;2;255;128;0m`.
- Assert dim/italic/underline/strikethrough map to codes 2/3/4/9.

---

## NTASK-03 — POSIX raw mode (Linux / macOS)

### Goal
Create a class that puts the terminal into raw mode on POSIX systems (Linux, macOS) using
JNA to call `tcgetattr` and `tcsetattr` from libc.

### Background
By default, the terminal is in "cooked" mode: the OS buffers input line-by-line and echoes
it back. For a TUI, we need:
- **No echo**: keystrokes must not appear on screen automatically.
- **No line buffering (ICANON off)**: each byte is available immediately.
- **No signal generation (ISIG off)** *(optional but clean)*: Ctrl+C should not kill the JVM;
  instead it should be delivered as a `KeyEvent(CHARACTER, 'c', ctrl=true)`.
- **VMIN=1, VTIME=0**: `read()` blocks until at least 1 byte is available.

The struct `termios` layout used by `tcgetattr`/`tcsetattr` differs between Linux and macOS,
so use `jna-platform`'s `com.sun.jna.platform.unix.LibC` which provides a cross-platform binding,
or define a minimal JNA `Structure` manually (safer across JNA versions).

### Files to create
- `src/main/java/io/alive/tui/backend/PosixRawMode.java`

### Implementation notes

JNA mapping for libc:
```java
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

interface LibC extends Library {
    LibC INSTANCE = Native.load("c", LibC.class);
    int tcgetattr(int fd, Termios termios);
    int tcsetattr(int fd, int action, Termios termios);
}
```

`Termios` structure — use `com.sun.jna.platform.unix.LibCUtil.Termios` if available,
or define manually with fields `c_iflag`, `c_oflag`, `c_cflag`, `c_lflag`, `c_cc[]`.

Key flag constants:
- `ECHO   = 0x00000008`
- `ICANON = 0x00000002`
- `ISIG   = 0x00000001` (optional; disable to handle Ctrl+C as character)
- `VMIN   = 6`  (index into `c_cc`)
- `VTIME  = 5`  (index into `c_cc`)
- `TCSANOW = 0`

Pseudocode for `enter()`:
```
save = tcgetattr(STDIN=0, &saved)
raw = copy of saved
raw.c_lflag &= ~(ECHO | ICANON | ISIG)
raw.c_cc[VMIN]  = 1
raw.c_cc[VTIME] = 0
tcsetattr(STDIN=0, TCSANOW, &raw)
```

Register a JVM shutdown hook that calls `restore()` so the terminal is always restored
even if the application crashes without calling `shutdown()`.

```java
public final class PosixRawMode {
    private Termios savedState;
    public void enter() { /* save + configure */ }
    public void restore() { /* tcsetattr with savedState */ }
}
```

### Tests
- Tests for this class cannot run on Windows CI; annotate with `@DisabledOnOs(OS.WINDOWS)`
  (JUnit 5) or guard with `Assumptions.assumeTrue(OsPlatform.isPosix())`.
- Verify: after `enter()`, `savedState` is non-null.
- Verify: `restore()` after `enter()` does not throw.
- Do NOT test actual terminal flags in unit tests (that requires a real PTY); just verify
  that JNA calls are made without exceptions.

---

## NTASK-04 — Windows raw mode + VT output enable

### Goal
Create a class that configures Windows console handles for raw input and VT/ANSI output.

### Background
On Windows, console I/O is controlled via `GetConsoleMode` / `SetConsoleMode` from `Kernel32`.
Two handles need configuration:

**stdin** (`STD_INPUT_HANDLE = -10`):
- Disable: `ENABLE_ECHO_INPUT (0x0004)`, `ENABLE_LINE_INPUT (0x0002)`,
  `ENABLE_PROCESSED_INPUT (0x0001)` *(prevents Ctrl+C killing JVM)*
- Enable:  `ENABLE_VIRTUAL_TERMINAL_INPUT (0x0200)` *(receive escape sequences for arrows etc.)*

**stdout** (`STD_OUTPUT_HANDLE = -11`):
- Enable: `ENABLE_PROCESSED_OUTPUT (0x0001)`, `ENABLE_VIRTUAL_TERMINAL_PROCESSING (0x0004)`
  *(allows writing ANSI escape sequences)*

Use `com.sun.jna.platform.win32.Kernel32` from `jna-platform`.

### Files to create
- `src/main/java/io/alive/tui/backend/WindowsRawMode.java`

### Implementation notes

```java
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

public final class WindowsRawMode {

    private static final int STD_INPUT_HANDLE  = -10;
    private static final int STD_OUTPUT_HANDLE = -11;

    private static final int ENABLE_ECHO_INPUT                = 0x0004;
    private static final int ENABLE_LINE_INPUT                = 0x0002;
    private static final int ENABLE_PROCESSED_INPUT           = 0x0001;
    private static final int ENABLE_VIRTUAL_TERMINAL_INPUT    = 0x0200;
    private static final int ENABLE_PROCESSED_OUTPUT          = 0x0001;
    private static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

    private int savedInputMode  = -1;
    private int savedOutputMode = -1;

    public void enter() {
        Kernel32 k32 = Kernel32.INSTANCE;
        HANDLE hIn  = k32.GetStdHandle(STD_INPUT_HANDLE);
        HANDLE hOut = k32.GetStdHandle(STD_OUTPUT_HANDLE);

        IntByReference inMode  = new IntByReference();
        IntByReference outMode = new IntByReference();
        k32.GetConsoleMode(hIn,  inMode);
        k32.GetConsoleMode(hOut, outMode);

        savedInputMode  = inMode.getValue();
        savedOutputMode = outMode.getValue();

        int newIn = (savedInputMode
                & ~ENABLE_ECHO_INPUT & ~ENABLE_LINE_INPUT & ~ENABLE_PROCESSED_INPUT)
                | ENABLE_VIRTUAL_TERMINAL_INPUT;
        int newOut = savedOutputMode | ENABLE_PROCESSED_OUTPUT | ENABLE_VIRTUAL_TERMINAL_PROCESSING;

        k32.SetConsoleMode(hIn,  newIn);
        k32.SetConsoleMode(hOut, newOut);
    }

    public void restore() {
        if (savedInputMode < 0) return;
        Kernel32 k32 = Kernel32.INSTANCE;
        k32.SetConsoleMode(k32.GetStdHandle(STD_INPUT_HANDLE),  savedInputMode);
        k32.SetConsoleMode(k32.GetStdHandle(STD_OUTPUT_HANDLE), savedOutputMode);
    }
}
```

Register a JVM shutdown hook in `enter()` that calls `restore()`.

### Tests
- Annotate with `@EnabledOnOs(OS.WINDOWS)` (JUnit 5).
- Verify: `enter()` + `restore()` completes without exceptions.
- Verify: `restore()` before `enter()` is a safe no-op (savedInputMode check).

---

## NTASK-05 — Terminal size detection (POSIX + Windows)

### Goal
Create a class that returns the current terminal width (columns) and height (rows).

### Background
Terminal dimensions must be read from the OS:
- **POSIX**: `ioctl(STDOUT_FILENO=1, TIOCGWINSZ=0x5413, struct winsize*)` fills a struct with
  `ws_col` and `ws_row`.
- **Windows**: `GetConsoleScreenBufferInfo(hStdOut, &csbi)` → width = `csbi.srWindow.Right -
  csbi.srWindow.Left + 1`, height = `csbi.srWindow.Bottom - csbi.srWindow.Top + 1`.
- **Fallback**: read `COLUMNS` and `LINES` environment variables. Default to 80×24 if nothing else works.

### Files to create
- `src/main/java/io/alive/tui/backend/TerminalSizeDetector.java`

### Implementation notes

POSIX — JNA ioctl mapping:
```java
interface LibC extends Library {
    LibC INSTANCE = Native.load("c", LibC.class);
    int ioctl(int fd, long request, WinSize winsize);
}

// WinSize is a JNA Structure with fields: ws_row (short), ws_col (short),
// ws_xpixel (short), ws_ypixel (short). Field order matters.
```
`TIOCGWINSZ` value: `0x5413` on Linux, `0x40087468` on macOS.
Use `OsPlatform.get()` to pick the right constant.

Windows — use `com.sun.jna.platform.win32.Kernel32`:
```java
CONSOLE_SCREEN_BUFFER_INFO csbi = new CONSOLE_SCREEN_BUFFER_INFO();
Kernel32.INSTANCE.GetConsoleScreenBufferInfo(hStdOut, csbi);
int w = csbi.srWindow.Right  - csbi.srWindow.Left + 1;
int h = csbi.srWindow.Bottom - csbi.srWindow.Top  + 1;
```

```java
public final class TerminalSizeDetector {
    public int getWidth()  { /* platform branch */ }
    public int getHeight() { /* platform branch */ }
}
```

Both methods must return ≥ 1. If the OS call fails, fall through to env vars, then to 80/24.

### Tests
- `TerminalSizeDetectorTest` — instantiate and call `getWidth()` / `getHeight()`.
  Assert both return ≥ 1 (do not assert specific values; size varies by terminal).
- Mock `OsPlatform` or test fallback path: set `COLUMNS=100`, `LINES=30` env variables
  and assert fallback returns those values.

---

## NTASK-06 — Resize detection (POSIX SIGWINCH + Windows polling)

### Goal
Detect terminal resize events and notify `NativeBackend` so it can invoke the registered
resize listener.

### Background
- **POSIX**: the OS sends `SIGWINCH` to the process when the terminal is resized. Install a
  signal handler via JNA that sets a volatile flag. `NativeBackend.flush()` checks this flag
  and, if set, re-reads the terminal size and calls the resize listener.
- **Windows**: there is no equivalent signal. Instead, run a daemon background thread that
  polls `TerminalSizeDetector.getWidth()/getHeight()` every 200 ms. If the size changes,
  set a flag that `NativeBackend.flush()` checks.

### Files to create
- `src/main/java/io/alive/tui/backend/ResizeDetector.java`

### Implementation notes

```java
public interface ResizeDetector {
    /** Start detection. Must be idempotent. */
    void start(Runnable onChange);
    /** Stop detection. Must be idempotent. */
    void stop();
    /** Returns true if a resize was detected since the last call to consumeResize(). */
    boolean consumeResize();
}
```

**PosixResizeDetector** implements `ResizeDetector`:
- Uses JNA `Signal` or `SignalHandler` from `com.sun.jna.platform.unix` if available.
- If not available, use `sun.misc.Signal` (internal, but broadly supported on JDK 17+).
  Guard with a try/catch `Throwable` so it fails gracefully on restricted JVMs.
- Sets a `volatile boolean resized = true` in the handler.
- `consumeResize()` returns-and-clears `resized` atomically (use `AtomicBoolean`).

**WindowsResizeDetector** implements `ResizeDetector`:
- Constructor takes a `TerminalSizeDetector`.
- Stores `lastWidth`, `lastHeight` from `TerminalSizeDetector` at `start()` time.
- Background daemon thread (Thread.setDaemon(true)) polls every 200 ms.
  If `getWidth() != lastWidth || getHeight() != lastHeight`, set `AtomicBoolean resized = true`.
- `stop()` interrupts the thread and joins it with a short timeout.

`ResizeDetectorFactory` (package-private, static factory):
```java
static ResizeDetector create(TerminalSizeDetector sizeDetector) {
    return OsPlatform.isPosix()
        ? new PosixResizeDetector()
        : new WindowsResizeDetector(sizeDetector);
}
```

### Tests
- `WindowsResizeDetectorTest` — simulate size change by providing a `TerminalSizeDetector`
  stub that first returns 80×24, then returns 100×30. Assert `consumeResize()` returns true.
- `PosixResizeDetectorTest` — annotated `@DisabledOnOs(OS.WINDOWS)`. Directly set the
  `AtomicBoolean` via reflection (or make it package-visible) and assert `consumeResize()`.

---

## NTASK-07 — ANSI key decoder (stdin bytes → KeyEvent)

### Goal
Create a class that reads raw bytes from `System.in` and converts them to `KeyEvent` objects,
parsing ANSI/VT escape sequences for special keys (arrows, function keys, modifiers).

### Background
In raw mode, `System.in` delivers keystrokes as raw bytes:
- Printable ASCII: single byte (e.g., `'a'` = `0x61`).
- Ctrl+letter: byte `0x01`–`0x1A` (Ctrl+A=1, Ctrl+C=3, Ctrl+Z=26).
- Enter: `0x0D` or `0x0A`.
- Backspace: `0x7F` (most terminals) or `0x08`.
- Tab: `0x09`.
- Escape alone: `0x1B` followed by a timeout (no more bytes within ~50 ms).
- Special keys: ESC sequence, e.g., `ESC [ A` = Arrow Up.

ANSI CSI sequences start with `ESC [ ` (`0x1B 0x5B`):
| Sequence          | KeyType        | Notes                              |
|-------------------|----------------|------------------------------------|
| `ESC[A`           | ARROW_UP       |                                    |
| `ESC[B`           | ARROW_DOWN     |                                    |
| `ESC[C`           | ARROW_RIGHT    |                                    |
| `ESC[D`           | ARROW_LEFT     |                                    |
| `ESC[H`           | HOME           |                                    |
| `ESC[F`           | END            |                                    |
| `ESC[Z`           | SHIFT_TAB      |                                    |
| `ESC[1~`          | HOME           | alternate                          |
| `ESC[4~`          | END            | alternate                          |
| `ESC[5~`          | PAGE_UP        |                                    |
| `ESC[6~`          | PAGE_DOWN      |                                    |
| `ESC[3~`          | DELETE         |                                    |
| `ESC OA`          | ARROW_UP       | SS3 (application mode)             |
| `ESC OB`          | ARROW_DOWN     | SS3                                |
| `ESC OC`          | ARROW_RIGHT    | SS3                                |
| `ESC OD`          | ARROW_LEFT     | SS3                                |
| `ESC[1;2A`        | ARROW_UP       | shift=true                         |
| `ESC[1;5A`        | ARROW_UP       | ctrl=true                          |
| `ESC[1;3A`        | ARROW_UP       | alt=true                           |

Modifier byte meaning (second parameter after `;`):
`1`=none, `2`=shift, `3`=alt, `4`=alt+shift, `5`=ctrl, `6`=ctrl+shift, `7`=ctrl+alt, `8`=all.

### Files to create
- `src/main/java/io/alive/tui/backend/AnsiKeyDecoder.java`

### Implementation notes

```java
public final class AnsiKeyDecoder {

    private final InputStream in;

    public AnsiKeyDecoder() { this(System.in); }
    public AnsiKeyDecoder(InputStream in) { this.in = in; }

    /**
     * Blocks until a key event is available.
     * Returns KeyEvent(EOF) on end-of-stream.
     */
    public KeyEvent readKey() throws InterruptedException { ... }

    /**
     * Waits up to timeoutMs milliseconds for a key event.
     * Returns null if no key arrived in time.
     */
    public KeyEvent readKey(long timeoutMs) throws InterruptedException { ... }
}
```

Internal algorithm for `readKey()`:
1. Read one byte from `in`. On `IOException`, throw `TerminalRenderException`. On -1, return EOF.
2. If byte is `0x1B` (ESC):
   - Attempt to read more bytes with a 50 ms timeout.
   - If no byte follows: return `KeyEvent(ESCAPE)`.
   - If next byte is `[` (CSI): read CSI sequence.
   - If next byte is `O` (SS3): read SS3 sequence.
   - Otherwise: treat as Alt+character.
3. CSI sequence reading:
   - Read bytes until a byte in range `0x40`–`0x7E` (the "final byte").
   - Collect intermediate bytes as the "parameter string".
   - Parse parameter string and final byte into a `KeyType` + modifier flags using the table above.
4. If byte is `0x0D` or `0x0A`: return `KeyEvent(ENTER)`.
5. If byte is `0x7F` or `0x08`: return `KeyEvent(BACKSPACE)`.
6. If byte is `0x09`: return `KeyEvent(TAB)`.
7. If byte is `0x01`–`0x1A`: return `KeyEvent(CHARACTER, (char)('a' + byte - 1), ctrl=true)`.
8. If byte is printable (`0x20`–`0x7E`): return `KeyEvent(CHARACTER, (char)byte)`.
9. Fallback: return `KeyEvent(EOF)`.

For timed reads: use `InputStream.available()` + a short sleep loop with deadline check.
On Windows, `System.in` in raw mode should have `available() > 0` immediately when a byte
arrives. If `available()` is unreliable, fall back to a dedicated reader thread with a
blocking queue — see note in code.

### Tests
- `AnsiKeyDecoder` accepts an `InputStream`, so pass a `ByteArrayInputStream` with pre-loaded bytes.
- Test each entry in the table above: feed the exact byte sequence, assert the resulting `KeyEvent`.
- Test modifier combinations: `ESC[1;2A` → ARROW_UP with shift=true, ctrl=false.
- Test Ctrl+C: byte `0x03` → `KeyEvent(CHARACTER, 'c', ctrl=true)`.
- Test bare ESC: single `0x1B` in stream → `KeyEvent(ESCAPE)`.
- Test EOF: empty stream → `KeyEvent(EOF)`.
- Test `readKey(timeoutMs)` with an empty stream: assert returns null within ~100 ms.

---

## NTASK-08 — NativeBackend — assembles NTASK-02..07

### Goal
Implement `TerminalBackend` by combining `AnsiWriter`, the platform raw-mode handler,
`TerminalSizeDetector`, `ResizeDetector`, and `AnsiKeyDecoder`.

### Background
`NativeBackend` is the main user-facing class. It wires all components together and handles
the lifecycle (`init` → event loop → `shutdown`).

The class must use the correct package (`io.alive.tui.backend`) and implement the interface
`TerminalBackend` exactly as defined in `TerminalBackend.java`.

Note: the existing stub in this branch uses the correct class name and implements the interface;
the implementation here replaces all `throw new UnsupportedOperationException(...)` bodies.

### Files to modify
- `src/main/java/io/alive/tui/backend/NativeBackend.java` (replace stub implementation)

### Implementation notes

```java
public final class NativeBackend implements TerminalBackend {

    private final AnsiWriter         writer;
    private final TerminalSizeDetector sizeDetector;
    private final ResizeDetector     resizeDetector;
    private final AnsiKeyDecoder     keyDecoder;

    // Platform raw mode: one of PosixRawMode or WindowsRawMode
    private final Object rawMode;

    private Runnable resizeListener;
    private boolean  initialized = false;

    // Package-private constructor for testing; public factory via Backends.createNative()
    NativeBackend(AnsiWriter w, TerminalSizeDetector s, ResizeDetector r, AnsiKeyDecoder k, Object rm) { ... }
}
```

**`init()`**:
1. Call `rawMode.enter()` (dispatch by type or use a common interface `RawMode { enter(); restore(); }`).
2. Call `writer.enterAlternateScreen()`.
3. Call `writer.hideCursor()`.
4. Call `writer.clearScreen()`.
5. Call `writer.flush()`.
6. Call `resizeDetector.start(this::notifyResize)`.
7. Set `initialized = true`.

**`shutdown()`**:
1. Set `initialized = false`.
2. Call `resizeDetector.stop()`.
3. Call `writer.showCursor()`.
4. Call `writer.exitAlternateScreen()`.
5. Call `writer.flush()`.
6. Call `rawMode.restore()`.

**`putChar(col, row, char, Style)`**:
1. Call `writer.moveTo(col, row)`.
2. Call `writer.applyStyle(style)`.
3. Call `writer.writeChar(c)`.
(Do NOT call `writer.flush()` here — flush is deferred to `flush()`.)

**`flush()`**:
1. Check `resizeDetector.consumeResize()`. If true, call `resizeListener.run()` (if non-null).
2. Call `writer.flush()`.

**`getWidth()` / `getHeight()`**: delegate to `sizeDetector`.

**`hideCursor()` / `showCursor()` / `setCursor(col, row)`**: delegate to `writer`.

**`clear()`**: delegate to `writer.clearScreen()` + `writer.flush()`.

**`readKey()` / `readKey(long)`**: delegate to `keyDecoder`.

**`setResizeListener(Runnable)`**: store in field.

Create a package-private interface:
```java
interface RawMode {
    void enter();
    void restore();
}
```
Both `PosixRawMode` and `WindowsRawMode` implement `RawMode`.

### Tests
- Use `MockAnsiWriter` (captures escape sequences as strings) and a `MockRawMode` (records calls).
- Test `init()`: assert raw mode was entered, alternate screen and hide-cursor sequences were emitted.
- Test `shutdown()`: assert raw mode was restored, alternate screen exit and show-cursor emitted.
- Test `putChar()`: assert correct sequence of moveTo + applyStyle + writeChar with no flush.
- Test `flush()` with resize flag set: assert resize listener is called once.
- Test `flush()` with no resize: assert resize listener is NOT called.

---

## NTASK-09 — Backends factory

### Goal
Create a static factory class `Backends` that provides convenient creation methods for all
three backend implementations.

### Background
Currently `LanternaBackend` is instantiated directly (`new LanternaBackend()`). The factory
standardizes creation and allows the native backend to be selected without knowing its
internal construction details.

### Files to create
- `src/main/java/io/alive/tui/backend/Backends.java`

### Implementation notes

```java
public final class Backends {

    private Backends() {}

    /**
     * Creates a native backend using direct POSIX/Windows I/O.
     * Does not depend on Lanterna.
     * Throws {@link UnsupportedOperationException} on unsupported platforms (OS.OTHER).
     */
    public static TerminalBackend createNative() {
        TerminalSizeDetector sizeDetector = new TerminalSizeDetector();
        ResizeDetector resizeDetector = ResizeDetectorFactory.create(sizeDetector);
        AnsiWriter writer = new AnsiWriter();
        AnsiKeyDecoder keyDecoder = new AnsiKeyDecoder();
        RawMode rawMode = OsPlatform.isPosix()
            ? new PosixRawMode()
            : new WindowsRawMode();
        return new NativeBackend(writer, sizeDetector, resizeDetector, keyDecoder, rawMode);
    }

    /**
     * Creates a Lanterna-based backend (current default).
     */
    public static TerminalBackend createLanterna() {
        return new LanternaBackend();
    }

    /**
     * Creates an in-memory mock backend for testing.
     *
     * @param width  terminal width in columns
     * @param height terminal height in rows
     */
    public static TerminalBackend createMock(int width, int height) {
        return new MockBackend(width, height);
    }
}
```

### Tests
- `BackendsTest`:
  - `createLanterna()` returns a non-null `LanternaBackend` instance.
  - `createMock(80, 24)` returns a non-null `MockBackend` with correct dimensions.
  - `createNative()` returns a non-null `NativeBackend` on the current platform
    (skip if `OsPlatform.get() == OS.OTHER`).

---

## NTASK-10 — Unit and integration tests

### Goal
Achieve full unit test coverage of all NTASK-01..09 classes and add one end-to-end
integration test that runs the AliveJTUI engine against `NativeBackend` on a real terminal.

### Background
`LanternaBackend` is currently excluded from SonarCloud coverage because it cannot run
headless (it opens a Swing window). `NativeBackend` should be fully unit-testable via mocks
and partially integration-testable in CI with a PTY.

### Files to create
All under `src/test/java/io/alive/tui/backend/`:
- `OsPlatformTest.java`                — NTASK-01
- `AnsiWriterTest.java`                — NTASK-02
- `PosixRawModeTest.java`              — NTASK-03 (skip on Windows)
- `WindowsRawModeTest.java`            — NTASK-04 (skip on non-Windows)
- `TerminalSizeDetectorTest.java`      — NTASK-05
- `WindowsResizeDetectorTest.java`     — NTASK-06
- `PosixResizeDetectorTest.java`       — NTASK-06 (skip on Windows)
- `AnsiKeyDecoderTest.java`            — NTASK-07
- `NativeBackendTest.java`             — NTASK-08
- `BackendsTest.java`                  — NTASK-09
- `NativeBackendIntegrationTest.java`  — integration (annotated @Tag("integration"), skip in CI unless PTY available)

### Integration test notes
The integration test should:
1. Detect whether stdin is a real terminal (e.g., `System.console() != null`).
2. If not, skip with `Assumptions.assumeTrue(false, "Not a real terminal")`.
3. If yes: create a `NativeBackend`, call `init()`, write a few characters, call `flush()`,
   call `shutdown()`, and assert no exceptions.

### Coverage requirements
All new classes (NTASK-01..09) must have ≥ 90% line coverage measured by JaCoCo.
Classes that contain only JNA calls to native code may be excluded via `@ExcludeFromJacocoGeneratedReport`
or an explicit exclusion rule in `pom.xml`, with a comment explaining why.
