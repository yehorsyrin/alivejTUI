# AliveJTUI — Native Backend Task List

> Goal: replace the Lanterna 3.1.2 dependency with a zero-dependency backend that:
>   - On Linux headless (no display): talks directly to the OS terminal via JNA (POSIX raw mode + ANSI I/O).
>   - On all graphical environments (Windows, macOS, Linux with X11/Wayland): opens a Swing
>     terminal window implemented from scratch — no Lanterna.
>
> The dispatch logic mirrors what LanternaBackend already does:
>   `GraphicsEnvironment.isHeadless()` → headless path; otherwise → Swing path.
>
> Reference implementation: `LanternaBackend.java`
> Target interface:         `TerminalBackend.java`
> Package (this branch):    `io.alive.tui.backend`
>
> All tasks are independent unless a "Depends on" line says otherwise.
> Authored by: Jarvis (AI) — 2026-03-29

---

## Architecture overview

```
NativeBackend.init()
       │
       ├─ GraphicsEnvironment.isHeadless() == false
       │       └─► SwingTerminalBackend  (NTASK-08..10)
       │               SwingTerminalPanel  (renders char grid, captures AWT keys)
       │               SwingKeyDecoder     (AWT KeyEvent → our KeyEvent)
       │
       └─ GraphicsEnvironment.isHeadless() == true   (Linux only)
               └─► PosixNativeBackend  (NTASK-01..07)
                       AnsiWriter          (ANSI escape sequences → stdout)
                       PosixRawMode        (tcgetattr/tcsetattr via JNA)
                       TerminalSizeDetector (ioctl TIOCGWINSZ via JNA)
                       PosixResizeDetector  (SIGWINCH via JNA)
                       AnsiKeyDecoder       (stdin bytes → KeyEvent)
```

---

## Status

```
NTASK-01 [ ] JNA dependency + OsPlatform utility
NTASK-02 [ ] AnsiWriter — ANSI/VT escape sequence output          (headless path)
NTASK-03 [ ] PosixRawMode — tcgetattr/tcsetattr via JNA           (headless path)
NTASK-04 [ ] TerminalSizeDetector — ioctl TIOCGWINSZ via JNA      (headless path)
NTASK-05 [ ] PosixResizeDetector — SIGWINCH signal detection      (headless path)
NTASK-06 [ ] AnsiKeyDecoder — stdin bytes → KeyEvent              (headless path)
NTASK-07 [ ] PosixNativeBackend — assembles NTASK-01..06          (headless path)
NTASK-08 [ ] SwingTerminalPanel — Swing character grid UI         (graphical path)
NTASK-09 [ ] SwingKeyDecoder — AWT KeyEvent → KeyEvent            (graphical path)
NTASK-10 [ ] SwingTerminalBackend — assembles NTASK-08..09        (graphical path)
NTASK-11 [ ] Backends factory — dispatch headless vs graphical
NTASK-12 [ ] Unit and integration tests
```

---

## NTASK-01 — JNA dependency + OsPlatform utility

### Goal
Add JNA to the project and create a utility class that detects the current OS at runtime.
Used only by the headless POSIX path; the Swing path uses standard Java AWT.

### Background
JNA (Java Native Access) allows calling native C functions without JNI. We need it for
POSIX `tcgetattr`/`tcsetattr`/`ioctl` on Linux/macOS.

JNA comes in two artifacts:
- `net.java.dev.jna:jna` — core
- `net.java.dev.jna:jna-platform` — pre-built bindings (Libc, etc.)

Both must be added with `<optional>true</optional>` if possible, since end users on
graphical environments will never load the native path.
Use the latest stable release (≥ 5.14.0).

### Files to create / modify
- `pom.xml` — add two `<dependency>` entries
- `src/main/java/io/alive/tui/backend/OsPlatform.java` — new utility class

### Implementation

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
        if      (name.contains("win"))                          CURRENT = OS.WINDOWS;
        else if (name.contains("mac"))                          CURRENT = OS.MACOS;
        else if (name.contains("nux") || name.contains("nix")) CURRENT = OS.LINUX;
        else                                                    CURRENT = OS.OTHER;
    }

    public static OS  get()       { return CURRENT; }
    public static boolean isWindows() { return CURRENT == OS.WINDOWS; }
    public static boolean isMac()     { return CURRENT == OS.MACOS;   }
    public static boolean isLinux()   { return CURRENT == OS.LINUX;   }
    public static boolean isPosix()   { return isLinux() || isMac();  }

    private OsPlatform() {}
}
```

### Tests
- `OsPlatformTest`: assert `get()` is non-null; assert `isWindows()` and `isPosix()` are
  mutually exclusive.

---

## NTASK-02 — AnsiWriter — ANSI/VT escape sequence output

> **Headless POSIX path only.**

### Goal
Create a class that writes ANSI/VT100 escape sequences to `System.out`.
This is the sole output layer for the headless Linux backend.

### Background
The terminal is controlled via escape sequences written to stdout. Key sequences needed:

| Sequence               | Meaning                        |
|------------------------|--------------------------------|
| `ESC[?1049h`           | Enter alternate screen buffer  |
| `ESC[?1049l`           | Exit alternate screen buffer   |
| `ESC[2J`               | Clear entire screen            |
| `ESC[?25l`             | Hide cursor                    |
| `ESC[?25h`             | Show cursor                    |
| `ESC[{row};{col}H`     | Move cursor (1-based)          |
| `ESC[{sgr}m`           | Set graphic rendition (style)  |

SGR codes for `Style`:
| Code | Meaning         | Notes                                    |
|------|-----------------|------------------------------------------|
| `0`  | Reset all       | Always emit first                        |
| `1`  | Bold            |                                          |
| `2`  | Dim/faint       | Lanterna lacked this — we support it     |
| `3`  | Italic          |                                          |
| `4`  | Underline       |                                          |
| `9`  | Strikethrough   |                                          |
| `30`–`37` | FG ANSI 16 normal  |                                   |
| `90`–`97` | FG ANSI 16 bright  |                                   |
| `40`–`47` | BG ANSI 16 normal  |                                   |
| `100`–`107` | BG ANSI 16 bright |                                  |
| `38;5;{n}` | FG 256-color   |                                   |
| `48;5;{n}` | BG 256-color   |                                   |
| `38;2;{r};{g};{b}` | FG RGB    |                                   |
| `48;2;{r};{g};{b}` | BG RGB    |                                   |

### Files to create
- `src/main/java/io/alive/tui/backend/AnsiWriter.java`

### Implementation

```java
public final class AnsiWriter {
    private final PrintStream out;

    public AnsiWriter()                 { this(System.out); }
    public AnsiWriter(OutputStream out) { this.out = new PrintStream(out, false, StandardCharsets.UTF_8); }

    public void enterAlternateScreen()  { esc("[?1049h"); }
    public void exitAlternateScreen()   { esc("[?1049l"); }
    public void clearScreen()           { esc("[2J");     }
    public void hideCursor()            { esc("[?25l");   }
    public void showCursor()            { esc("[?25h");   }

    /** Move cursor to (col, row), both 0-based. Converts to 1-based internally. */
    public void moveTo(int col, int row) { esc("[" + (row + 1) + ";" + (col + 1) + "H"); }

    public void applyStyle(Style style) {
        StringBuilder sb = new StringBuilder("0");  // always reset first
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
    public void flush()           { out.flush();  }

    private void esc(String seq)  { out.print('\033' + seq); }

    private void appendColor(StringBuilder sb, Color c, boolean fg) {
        if (c == null) return;
        switch (c.getType()) {
            case ANSI_16  -> {
                int idx  = c.getAnsiIndex();
                int base = fg ? (idx < 8 ? 30 : 82) : (idx < 8 ? 40 : 92);
                sb.append(";").append(base + (idx % 8));
            }
            case ANSI_256 -> sb.append(";").append(fg ? "38;5;" : "48;5;").append(c.getAnsiIndex());
            case RGB      -> sb.append(";").append(fg ? "38;2;" : "48;2;")
                               .append(c.getR()).append(";").append(c.getG()).append(";").append(c.getB());
        }
    }
}
```

### Contract
- `applyStyle(null)` must emit only `ESC[0m`.
- `moveTo(0, 0)` must emit `ESC[1;1H`.
- `flush()` flushes the underlying stream; call once per render cycle, not per character.

### Tests (AnsiWriterTest)
- Capture stdout via `ByteArrayOutputStream` and assert exact sequences for:
  `enterAlternateScreen`, `exitAlternateScreen`, `clearScreen`, `hideCursor`, `showCursor`,
  `moveTo(0,0)`, `moveTo(5,3)`.
- `applyStyle(null)` → `\033[0m` only.
- Bold + red FG → `\033[0;1;31m`.
- RGB FG → `\033[0;38;2;255;128;0m`.
- Dim → code `2`; italic → `3`; underline → `4`; strikethrough → `9`.

---

## NTASK-03 — PosixRawMode — tcgetattr/tcsetattr via JNA

> **Headless POSIX path only.**

### Goal
Put the terminal into raw mode on Linux/macOS using JNA to call libc's `tcgetattr`/`tcsetattr`.

### Background
Default terminal ("cooked") mode buffers input line-by-line and echoes keystrokes.
Raw mode configures:
- `ECHO` off — keystrokes do not auto-appear on screen.
- `ICANON` off — bytes available immediately, no line buffering.
- `ISIG` off — Ctrl+C delivered as `KeyEvent(CHARACTER, 'c', ctrl=true)`, not SIGINT.
- `VMIN=1, VTIME=0` — `read()` blocks until at least 1 byte is available.

The `termios` struct layout differs between Linux and macOS. Use `jna-platform`'s
`com.sun.jna.platform.unix.LibC` if it exposes `tcgetattr`; otherwise define a minimal JNA
`Structure` manually with fields `c_iflag`, `c_oflag`, `c_cflag`, `c_lflag`, `byte[] c_cc`.

Key flag constants (Linux values; macOS differs — use `OsPlatform.isMac()` to select):
```
ECHO    = 0x00000008
ICANON  = 0x00000002
ISIG    = 0x00000001
VMIN    = 6   (index into c_cc)
VTIME   = 5   (index into c_cc)
TCSANOW = 0
STDIN_FD = 0
```

### Files to create
- `src/main/java/io/alive/tui/backend/PosixRawMode.java`
- Implements the `RawMode` interface defined in NTASK-07.

### Implementation

```java
public final class PosixRawMode implements RawMode {
    private Termios savedState;

    @Override
    public void enter() {
        savedState = new Termios();
        LibC.INSTANCE.tcgetattr(0, savedState);
        Termios raw = savedState.copy();
        raw.c_lflag &= ~(ECHO | ICANON | ISIG);
        raw.c_cc[VMIN]  = 1;
        raw.c_cc[VTIME] = 0;
        LibC.INSTANCE.tcsetattr(0, TCSANOW, raw);
        Runtime.getRuntime().addShutdownHook(new Thread(this::restore));
    }

    @Override
    public void restore() {
        if (savedState != null) LibC.INSTANCE.tcsetattr(0, TCSANOW, savedState);
    }
}
```

The shutdown hook ensures the terminal is always restored even if the JVM exits without
calling `shutdown()`.

### Tests
- Annotate with `@DisabledOnOs(OS.WINDOWS)`.
- Verify: after `enter()`, `savedState` is non-null.
- Verify: `restore()` after `enter()` does not throw.
- Do not assert actual terminal flags in unit tests (requires a real PTY).

---

## NTASK-04 — TerminalSizeDetector — ioctl TIOCGWINSZ

> **Headless POSIX path only.**

### Goal
Detect the current terminal width and height on Linux/macOS via `ioctl(TIOCGWINSZ)`.

### Background
`ioctl(STDOUT_FILENO, TIOCGWINSZ, &ws)` fills a `struct winsize` with `ws_col` and `ws_row`.
`TIOCGWINSZ` constant: `0x5413` on Linux, `0x40087468` on macOS.

Fallback chain if `ioctl` fails:
1. Read `COLUMNS` / `LINES` environment variables (parse as int).
2. Default to `80` columns × `24` rows.

### Files to create
- `src/main/java/io/alive/tui/backend/TerminalSizeDetector.java`

### Implementation

JNA interface inside the class (package-private static inner):
```java
interface LibC extends Library {
    LibC INSTANCE = Native.load("c", LibC.class);
    int ioctl(int fd, long request, WinSize ws);
}

// WinSize: JNA Structure with fields ws_row(short), ws_col(short),
// ws_xpixel(short), ws_ypixel(short). Field declaration order matters for JNA.
```

```java
public final class TerminalSizeDetector {
    private static final long TIOCGWINSZ =
        OsPlatform.isMac() ? 0x40087468L : 0x5413L;

    public int getWidth()  { return query()[0]; }
    public int getHeight() { return query()[1]; }

    private int[] query() {
        try {
            WinSize ws = new WinSize();
            if (LibC.INSTANCE.ioctl(1, TIOCGWINSZ, ws) == 0
                    && ws.ws_col > 0 && ws.ws_row > 0) {
                return new int[]{ws.ws_col, ws.ws_row};
            }
        } catch (Throwable ignored) {}
        return new int[]{parseEnv("COLUMNS", 80), parseEnv("LINES", 24)};
    }

    private static int parseEnv(String key, int def) {
        try { return Integer.parseInt(System.getenv(key)); }
        catch (Exception e) { return def; }
    }
}
```

### Tests
- `TerminalSizeDetectorTest`: call `getWidth()` / `getHeight()`, assert both ≥ 1.
- Fallback test: make `ioctl` unavailable (stub/subclass), set `COLUMNS=100 LINES=40` via
  a system-property shim, assert values returned correctly.

---

## NTASK-05 — PosixResizeDetector — SIGWINCH signal detection

> **Headless POSIX path only.**

### Goal
Detect terminal resize events on POSIX by handling the `SIGWINCH` signal.

### Background
When the user resizes the terminal window, the OS sends `SIGWINCH` to the foreground process.
We install a handler that sets an `AtomicBoolean`. `PosixNativeBackend.flush()` calls
`consumeResize()`, and if true, fires the resize listener.

Signal handling options (in order of preference):
1. `sun.misc.Signal` + `sun.misc.SignalHandler` — available on JDK 17+.
2. If unavailable (restricted JVM), log a warning and degrade gracefully (no resize detection).

### Files to create
- `src/main/java/io/alive/tui/backend/PosixResizeDetector.java`

### Implementation

```java
public final class PosixResizeDetector {
    private final AtomicBoolean resized = new AtomicBoolean(false);

    public void install() {
        try {
            sun.misc.Signal.handle(
                new sun.misc.Signal("WINCH"),
                sig -> resized.set(true)
            );
        } catch (Throwable t) {
            // Resize detection unavailable — degrade gracefully
        }
    }

    /** Returns true and clears the flag if a resize was detected since last call. */
    public boolean consumeResize() {
        return resized.getAndSet(false);
    }
}
```

### Tests
- Annotate with `@DisabledOnOs(OS.WINDOWS)`.
- Set `resized` to `true` directly (make field package-visible or use reflection).
- Assert `consumeResize()` returns `true`, then `false` on second call.

---

## NTASK-06 — AnsiKeyDecoder — stdin bytes → KeyEvent

> **Headless POSIX path only.**

### Goal
Read raw bytes from `System.in` and convert them to `KeyEvent` objects by parsing
ANSI/VT escape sequences.

### Background
In raw mode, each keystroke arrives as raw bytes on stdin. Most keys are a single byte;
special keys are multi-byte ANSI sequences starting with `ESC` (`0x1B`).

| Byte(s)           | KeyType      | Modifiers |
|-------------------|--------------|-----------|
| `0x0D` or `0x0A`  | ENTER        |           |
| `0x7F` or `0x08`  | BACKSPACE    |           |
| `0x09`            | TAB          |           |
| `0x01`–`0x1A`     | CHARACTER    | ctrl=true |
| `0x20`–`0x7E`     | CHARACTER    |           |
| `ESC[A`           | ARROW_UP     |           |
| `ESC[B`           | ARROW_DOWN   |           |
| `ESC[C`           | ARROW_RIGHT  |           |
| `ESC[D`           | ARROW_LEFT   |           |
| `ESC[H`           | HOME         |           |
| `ESC[F`           | END          |           |
| `ESC[Z`           | SHIFT_TAB    | shift     |
| `ESC[1~`          | HOME         |           |
| `ESC[4~`          | END          |           |
| `ESC[5~`          | PAGE_UP      |           |
| `ESC[6~`          | PAGE_DOWN    |           |
| `ESC[3~`          | DELETE       |           |
| `ESC OA`          | ARROW_UP     |           |
| `ESC OB`          | ARROW_DOWN   |           |
| `ESC OC`          | ARROW_RIGHT  |           |
| `ESC OD`          | ARROW_LEFT   |           |
| `ESC[1;2A`        | ARROW_UP     | shift     |
| `ESC[1;5A`        | ARROW_UP     | ctrl      |
| `ESC[1;3A`        | ARROW_UP     | alt       |

Modifier encoding in the second CSI parameter:
`1`=none, `2`=shift, `3`=alt, `4`=alt+shift, `5`=ctrl, `6`=ctrl+shift, `7`=ctrl+alt, `8`=all.

### Files to create
- `src/main/java/io/alive/tui/backend/AnsiKeyDecoder.java`

### Implementation

```java
public final class AnsiKeyDecoder {
    private final InputStream in;

    public AnsiKeyDecoder()              { this(System.in); }
    public AnsiKeyDecoder(InputStream in){ this.in = in;    }

    /** Blocks until a key is available. Returns KeyEvent(EOF) on stream end. */
    public KeyEvent readKey() throws InterruptedException { ... }

    /** Waits up to timeoutMs for a key. Returns null on timeout. */
    public KeyEvent readKey(long timeoutMs) throws InterruptedException { ... }
}
```

Internal algorithm:
1. Read one byte; if -1 → return `EOF`.
2. If `0x1B`: try to read the next byte within 50 ms.
   - No byte → `ESCAPE`.
   - `[` (CSI) → read bytes until a final byte in `0x40`–`0x7E`; parse params + final byte.
   - `O` (SS3) → read one more byte, dispatch `A`–`D`.
   - Other byte `b` → `KeyEvent(CHARACTER, (char)b, alt=true)`.
3. `0x0D`/`0x0A` → `ENTER`. `0x7F`/`0x08` → `BACKSPACE`. `0x09` → `TAB`.
4. `0x01`–`0x1A` → `KeyEvent(CHARACTER, (char)('a' + b - 1), ctrl=true)`.
5. `0x20`–`0x7E` → `KeyEvent(CHARACTER, (char)b)`.
6. Default → `EOF`.

For timed reads use `in.available()` + a short sleep loop with a deadline.

### Tests (AnsiKeyDecoderTest)
- Feed each sequence from the table above via `ByteArrayInputStream`, assert the exact `KeyEvent`.
- Test Ctrl+C: `0x03` → `KeyEvent(CHARACTER, 'c', ctrl=true)`.
- Test bare ESC (only `0x1B` in stream) → `KeyEvent(ESCAPE)`.
- Test `readKey(50)` on empty stream: returns `null` within ~100 ms.
- Test EOF: empty stream → `KeyEvent(EOF)`.

---

## NTASK-07 — PosixNativeBackend — assembles NTASK-01..06

> **Headless POSIX path only.**

### Goal
Implement `TerminalBackend` for headless Linux by wiring together all headless components.

### Files to create
- `src/main/java/io/alive/tui/backend/RawMode.java` — shared interface (used here and in NTASK-03)
- `src/main/java/io/alive/tui/backend/PosixNativeBackend.java`

### RawMode interface
```java
interface RawMode {
    void enter();
    void restore();
}
```
`PosixRawMode` (NTASK-03) implements this interface.

### PosixNativeBackend implementation

```java
public final class PosixNativeBackend implements TerminalBackend {

    private final AnsiWriter           writer;
    private final PosixRawMode         rawMode;
    private final TerminalSizeDetector sizeDetector;
    private final PosixResizeDetector  resizeDetector;
    private final AnsiKeyDecoder       keyDecoder;
    private Runnable resizeListener;

    // Package-private for testing; use Backends.createNative() in production.
    PosixNativeBackend(AnsiWriter w, PosixRawMode rm,
                       TerminalSizeDetector sd, PosixResizeDetector rd,
                       AnsiKeyDecoder kd) { ... }
}
```

**`init()`**:
1. `rawMode.enter()`
2. `writer.enterAlternateScreen()`
3. `writer.hideCursor()`
4. `writer.clearScreen()`
5. `writer.flush()`
6. `resizeDetector.install()`

**`shutdown()`**:
1. `writer.showCursor()`
2. `writer.exitAlternateScreen()`
3. `writer.flush()`
4. `rawMode.restore()`

**`putChar(col, row, c, style)`**: `moveTo` → `applyStyle` → `writeChar`. Do NOT flush here.

**`flush()`**: If `resizeDetector.consumeResize()` and listener non-null → call listener. Then `writer.flush()`.

**Remaining methods**: delegate to `sizeDetector` (width/height), `writer` (cursor/clear),
`keyDecoder` (readKey).

### Tests (PosixNativeBackendTest)
- Skip on Windows: `Assumptions.assumeTrue(OsPlatform.isPosix())`.
- Use a mock `AnsiWriter` backed by `ByteArrayOutputStream`; stub all other collaborators.
- Test `init()`: assert alternate-screen + hide-cursor sequences emitted, `rawMode.enter()` called.
- Test `shutdown()`: assert show-cursor + exit-screen emitted, `rawMode.restore()` called.
- Test `putChar()`: assert moveTo + applyStyle + writeChar, no flush.
- Test `flush()` with resize flag set: assert listener called once.

---

## NTASK-08 — SwingTerminalPanel — Swing character grid

> **Graphical path (all OSes with a display).**

### Goal
Create a Swing `JPanel` subclass that renders a fixed-size character grid using a monospaced
font and delivers keyboard input via a blocking queue.

### Background
Mirrors the role of Lanterna's `SwingTerminalFrame` inner canvas, but implemented from
scratch so Lanterna is no longer a dependency.

Key design decisions:
- Grid size (cols × rows) is fixed at construction. Default: `120 × 35` (same as LanternaBackend).
- Each cell stores a `char` and a `Style`.
- `paintComponent(Graphics g)` paints each cell as: background rect + character glyph.
- A `KeyListener` pushes decoded `KeyEvent`s into a `LinkedBlockingQueue`.
- A `ComponentListener` fires a resize callback when the JFrame is resized.

### Files to create
- `src/main/java/io/alive/tui/backend/SwingTerminalPanel.java`

### Implementation notes

```java
public final class SwingTerminalPanel extends JPanel {
    private final int cols;
    private final int rows;
    private final char[][]  charGrid;
    private final Style[][] styleGrid;
    private final LinkedBlockingQueue<KeyEvent> keyQueue = new LinkedBlockingQueue<>();
    private final SwingKeyDecoder keyDecoder;
    private int   cursorCol = -1, cursorRow = -1;
    private boolean cursorVisible = false;
    private int cellWidth, cellHeight;

    public SwingTerminalPanel(int cols, int rows, Font font) {
        this.cols = cols;
        this.rows = rows;
        this.charGrid  = new char[rows][cols];
        this.styleGrid = new Style[rows][cols];
        this.keyDecoder = new SwingKeyDecoder();
        setFont(font);
        FontMetrics fm = getFontMetrics(font);
        cellWidth  = fm.charWidth('W');
        cellHeight = fm.getHeight();
        setPreferredSize(new Dimension(cols * cellWidth, rows * cellHeight));
        setBackground(java.awt.Color.BLACK);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                KeyEvent ke = keyDecoder.decode(e);
                if (ke != null) keyQueue.offer(ke);
            }
        });
    }
}
```

**`putChar(col, row, c, style)`**: store into grid arrays; do not repaint yet.

**`flush()`**: call `repaint()`. For fully synchronous rendering (needed in tests):
optionally `SwingUtilities.invokeAndWait(this::repaint)`.

**`paintComponent(Graphics g)`**:
```
for each row r:
  for each col c:
    bg = toAwtColor(styleGrid[r][c].getBackground(), DEFAULT_BG)
    fg = toAwtColor(styleGrid[r][c].getForeground(), DEFAULT_FG)
    fill rect (c*cellWidth, r*cellHeight, cellWidth, cellHeight) with bg
    set font variant (bold/italic) if style requires
    draw char charGrid[r][c] at baseline position with fg
if cursorVisible && cursorCol >= 0:
    draw 2px bottom rect at cursor position with DEFAULT_FG
```

Default background: `Color.BLACK`. Default foreground: `Color.LIGHT_GRAY`.

**`readKey()` / `readKey(long)`**: delegate to `keyQueue.take()` / `keyQueue.poll(ms, MILLISECONDS)`.

**`hideCursor()` / `showCursor()` / `setCursor(col,row)`**: update fields, call `repaint()`.

**`clear()`**: zero-fill both grids, call `repaint()`.

**Font resolution** (same logic as `LanternaBackend.resolveMonospacedFonts()`):
Try candidates: `"Consolas"`, `"Courier New"`, `"DejaVu Sans Mono"`, `"Liberation Mono"`,
`"Ubuntu Mono"`, `"Noto Mono"`, fallback to `Font.MONOSPACED`. Pick the first available.

### Tests (SwingTerminalPanelTest)
```java
static boolean isHeadless() { return GraphicsEnvironment.isHeadless(); }
```
- Annotate class with `@DisabledIf("isHeadless")`.
- Create panel, call `putChar(0,0,'A',Style.of())` + `flush()`, assert `charGrid[0][0] == 'A'`.
- Simulate key: `panel.dispatchEvent(new java.awt.event.KeyEvent(panel, KEY_PRESSED, 0, 0, VK_ENTER, '\n'))`.
  Assert `readKey(100)` returns `KeyEvent(ENTER)`.

---

## NTASK-09 — SwingKeyDecoder — AWT KeyEvent → KeyEvent

> **Graphical path (all OSes with a display).**

### Goal
Convert AWT `java.awt.event.KeyEvent` objects (from a Swing `KeyListener`) to our
`io.alive.tui.event.KeyEvent` records.

### Background
AWT delivers keyboard events via `VK_*` constants for special keys and `getKeyChar()` for
printable characters. Modifier flags (`isControlDown()` etc.) are available directly — no
escape-sequence parsing needed.

### Files to create
- `src/main/java/io/alive/tui/backend/SwingKeyDecoder.java`

### Implementation

```java
public final class SwingKeyDecoder {

    /** Returns null if the event should be ignored (modifier-only key, etc.). */
    public KeyEvent decode(java.awt.event.KeyEvent e) {
        boolean ctrl  = e.isControlDown();
        boolean alt   = e.isAltDown();
        boolean shift = e.isShiftDown();

        return switch (e.getKeyCode()) {
            case VK_ENTER      -> KeyEvent.of(KeyType.ENTER,       ctrl, alt, shift);
            case VK_BACK_SPACE -> KeyEvent.of(KeyType.BACKSPACE,   ctrl, alt, shift);
            case VK_DELETE     -> KeyEvent.of(KeyType.DELETE,      ctrl, alt, shift);
            case VK_ESCAPE     -> KeyEvent.of(KeyType.ESCAPE,      ctrl, alt, shift);
            case VK_TAB        -> shift
                                   ? KeyEvent.of(KeyType.SHIFT_TAB, ctrl, alt, true)
                                   : KeyEvent.of(KeyType.TAB,       ctrl, alt, false);
            case VK_UP         -> KeyEvent.of(KeyType.ARROW_UP,    ctrl, alt, shift);
            case VK_DOWN       -> KeyEvent.of(KeyType.ARROW_DOWN,  ctrl, alt, shift);
            case VK_LEFT       -> KeyEvent.of(KeyType.ARROW_LEFT,  ctrl, alt, shift);
            case VK_RIGHT      -> KeyEvent.of(KeyType.ARROW_RIGHT, ctrl, alt, shift);
            case VK_HOME       -> KeyEvent.of(KeyType.HOME,        ctrl, alt, shift);
            case VK_END        -> KeyEvent.of(KeyType.END,         ctrl, alt, shift);
            case VK_PAGE_UP    -> KeyEvent.of(KeyType.PAGE_UP,     ctrl, alt, shift);
            case VK_PAGE_DOWN  -> KeyEvent.of(KeyType.PAGE_DOWN,   ctrl, alt, shift);
            default -> {
                char c = e.getKeyChar();
                if (c == CHAR_UNDEFINED || (Character.isISOControl(c) && !ctrl)) yield null;
                yield KeyEvent.ofCharacter(c, ctrl, alt, shift);
            }
        };
    }
}
```

### Tests (SwingKeyDecoderTest)
- Create AWT `KeyEvent` for each `VK_*` constant; assert the resulting `KeyEvent` type and modifiers.
- Test `VK_TAB` + shift → `SHIFT_TAB`.
- Test printable char 'a' → `KeyEvent(CHARACTER, 'a')`.
- Test Ctrl+C (`VK_C` + `isControlDown()`) → `KeyEvent(CHARACTER, 'c', ctrl=true)`.
- Test modifier-only key (`VK_SHIFT`) → returns `null`.

---

## NTASK-10 — SwingTerminalBackend — assembles NTASK-08..09

> **Graphical path (all OSes with a display).**

### Goal
Implement `TerminalBackend` for graphical environments by embedding `SwingTerminalPanel`
in a `JFrame`.

### Background
Mirrors how `LanternaBackend` uses `SwingTerminalFrame`. Key difference: no Lanterna.

### Files to create
- `src/main/java/io/alive/tui/backend/SwingTerminalBackend.java`

### Implementation

```java
public final class SwingTerminalBackend implements TerminalBackend {

    private static final int DEFAULT_COLS = 120;
    private static final int DEFAULT_ROWS = 35;

    private JFrame             frame;
    private SwingTerminalPanel panel;
    private Runnable           resizeListener;

    @Override
    public void init() {
        Font font  = resolveFont(16);   // same font resolution as NTASK-08
        panel = new SwingTerminalPanel(DEFAULT_COLS, DEFAULT_ROWS, font);
        frame = new JFrame("AliveJTUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
        panel.requestFocusInWindow();

        frame.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                if (resizeListener != null) resizeListener.run();
            }
        });
    }

    @Override
    public void shutdown() {
        if (frame != null) { frame.setVisible(false); frame.dispose(); }
    }

    @Override public int getWidth()  { return panel != null ? panel.getCols() : DEFAULT_COLS; }
    @Override public int getHeight() { return panel != null ? panel.getRows() : DEFAULT_ROWS; }

    @Override public void putChar(int col, int row, char c, Style style) { panel.putChar(col,row,c,style); }
    @Override public void flush()                                        { panel.flush(); }
    @Override public void hideCursor()                                   { panel.hideCursor(); }
    @Override public void showCursor()                                   { panel.showCursor(); }
    @Override public void setCursor(int col, int row)                    { panel.setCursor(col, row); }
    @Override public void clear()                                        { panel.clear(); }
    @Override public KeyEvent readKey() throws InterruptedException      { return panel.readKey(); }
    @Override public KeyEvent readKey(long ms) throws InterruptedException { return panel.readKey(ms); }
    @Override public void setResizeListener(Runnable r)                  { this.resizeListener = r; }
}
```

### Tests (SwingTerminalBackendTest)
- Annotate with `@DisabledIf("isHeadless")`.
- `init()` → assert frame is visible.
- `shutdown()` → assert frame is not displayable.
- `putChar(0,0,'X',null)` + `flush()` → assert char stored in panel grid.

---

## NTASK-11 — Backends factory

### Goal
A static factory class that dispatches to the correct backend based on the runtime environment.

### Files to create
- `src/main/java/io/alive/tui/backend/Backends.java`

### Implementation

```java
public final class Backends {

    private Backends() {}

    /**
     * Creates the appropriate native backend for the current environment:
     *   - Non-headless (Windows / macOS / Linux+display): SwingTerminalBackend
     *   - Headless Linux: PosixNativeBackend
     *
     * @throws UnsupportedOperationException on headless non-POSIX systems
     */
    public static TerminalBackend createNative() {
        if (!GraphicsEnvironment.isHeadless()) {
            return new SwingTerminalBackend();
        }
        if (OsPlatform.isPosix()) {
            return new PosixNativeBackend(
                new AnsiWriter(),
                new PosixRawMode(),
                new TerminalSizeDetector(),
                new PosixResizeDetector(),
                new AnsiKeyDecoder()
            );
        }
        throw new UnsupportedOperationException(
            "NativeBackend not supported on headless non-POSIX systems");
    }

    /** Creates the Lanterna-based backend (current default). */
    public static TerminalBackend createLanterna() {
        return new LanternaBackend();
    }

    /** Creates an in-memory mock backend for unit testing. */
    public static TerminalBackend createMock(int width, int height) {
        return new MockBackend(width, height);
    }
}
```

### Tests (BackendsTest)
- `createLanterna()` → non-null `LanternaBackend`.
- `createMock(80,24)` → `MockBackend` with width=80, height=24.
- `createNative()` → non-null; is `SwingTerminalBackend` if graphical, `PosixNativeBackend` if headless POSIX.

---

## NTASK-12 — Unit and integration tests

### Goal
Achieve ≥ 90% line coverage on all NTASK-01..11 classes via JaCoCo.
Add one end-to-end smoke test per path.

### Test files

All under `src/test/java/io/alive/tui/backend/`:

| File                              | Covers    | Platform guard                        |
|-----------------------------------|-----------|---------------------------------------|
| `OsPlatformTest.java`             | NTASK-01  | none                                  |
| `AnsiWriterTest.java`             | NTASK-02  | none (uses ByteArrayOutputStream)     |
| `PosixRawModeTest.java`           | NTASK-03  | `@DisabledOnOs(OS.WINDOWS)`           |
| `TerminalSizeDetectorTest.java`   | NTASK-04  | `@DisabledOnOs(OS.WINDOWS)`           |
| `PosixResizeDetectorTest.java`    | NTASK-05  | `@DisabledOnOs(OS.WINDOWS)`           |
| `AnsiKeyDecoderTest.java`         | NTASK-06  | none (uses ByteArrayInputStream)      |
| `PosixNativeBackendTest.java`     | NTASK-07  | `@DisabledOnOs(OS.WINDOWS)`           |
| `SwingTerminalPanelTest.java`     | NTASK-08  | `@DisabledIf("isHeadless")`           |
| `SwingKeyDecoderTest.java`        | NTASK-09  | none (pure Java)                      |
| `SwingTerminalBackendTest.java`   | NTASK-10  | `@DisabledIf("isHeadless")`           |
| `BackendsTest.java`               | NTASK-11  | none                                  |
| `NativeBackendIntegrationTest.java` | smoke   | `@Tag("integration")`                 |

### Integration smoke tests

**Graphical smoke test** (skip if headless):
1. `Backends.createNative()` on a graphical host.
2. `init()` → `putChar(0,0,'X',Style.of())` → `flush()` → `shutdown()`.
3. Assert no exceptions.

**Headless POSIX smoke test** (skip if not headless POSIX, or `System.console() == null`):
1. `Backends.createNative()` on headless Linux.
2. `init()` → `clear()` → `flush()` → `shutdown()`.
3. Assert no exceptions.

### Coverage exclusions
Inner `Library` interface declarations and JNA `Structure` subclasses consist entirely of
native bindings and cannot be meaningfully unit-tested. Exclude them from JaCoCo:
```xml
<!-- pom.xml, inside <configuration><excludes> of jacoco-maven-plugin -->
<exclude>io/alive/tui/backend/PosixRawMode$LibC.class</exclude>
<exclude>io/alive/tui/backend/TerminalSizeDetector$LibC.class</exclude>
<exclude>io/alive/tui/backend/TerminalSizeDetector$WinSize.class</exclude>
```
