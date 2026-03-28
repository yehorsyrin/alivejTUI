# alivejTUI-native — Backlog

Окремий модуль `alivejTUI-native`, що повністю замінює Lanterna власною реалізацією
низькорівневого вводу/виводу терміналу. Після реалізації залежність `lanterna` можна
прибрати з `pom.xml`.

---

## Архітектура модуля

```
alivejTUI-native/
  src/main/java/io/alive/tui/native/
    raw/          — сирий режим термінала (POSIX + Windows)
    input/        — декодування послідовностей клавіш і миші
    size/         — визначення розміру терміналу
    signal/       — SIGWINCH / resize events
    backend/      — NativeBackend (реалізує TerminalBackend)
```

**Залежності:**
- `alivejTUI` (основний модуль) — `provided` scope
- `net.java.dev.jna:jna` (необов'язково, але рекомендовано для Platform layer)
  Альтернатива без JNA — `ProcessBuilder` + `stty` (Unix) / Win32 Console API через
  JDK 22+ Foreign Function & Memory API.

**Entrypoint після реалізації:**
```java
AliveJTUI.run(new MyApp(), NativeBackend.create()); // замість LanternaBackend
```

---

## Задачі

---

### NTASK-01: Raw Terminal Mode — POSIX (Linux / macOS)
**Суть:** Переключити stdin у raw mode (no echo, no line buffering).
На Unix це `tcgetattr()` / `tcsetattr()` через libc.

**Підходи (від простого до правильного):**
- A) `ProcessBuilder("stty", "-F", "/dev/tty", "raw", "-echo")` — без залежностей,
  але повільно і не повертає старі налаштування надійно.
- B) JNA → `LibC.INSTANCE.tcgetattr / tcsetattr` — чисто, правильно, рекомендовано.

**Файли:** `raw/PosixRawMode.java`
**API:**
```java
public class PosixRawMode {
    public static void enable();   // зберегти termios, застосувати raw mode
    public static void disable();  // відновити збережений termios
    public static boolean isSupported(); // false на Windows
}
```
**Тести:** `PosixRawModeTest` (mock JNA / skip on Windows)
**Пріоритет:** HIGH | **Складність:** MED

---

### NTASK-02: Raw Terminal Mode — Windows
**Суть:** Windows Console API — `SetConsoleMode` / `GetConsoleMode` через
`kernel32.dll`. Потрібно вимкнути `ENABLE_ECHO_INPUT` і `ENABLE_LINE_INPUT`,
увімкнути `ENABLE_VIRTUAL_TERMINAL_INPUT` (VT sequences in input).

**Файли:** `raw/WindowsRawMode.java`
**API:** (аналогічно NTASK-01)
```java
public class WindowsRawMode {
    public static void enable();
    public static void disable();
    public static boolean isSupported(); // true тільки на Windows
}
```
**Тести:** `WindowsRawModeTest` (skip on non-Windows)
**Пріоритет:** HIGH | **Складність:** MED

---

### NTASK-03: Alternate Screen Buffer + Cursor
**Суть:** ANSI escape sequences для переключення alternate screen, hide/show cursor.
На відміну від Lanterna, AnsiBackend вже частково це робить (TASK-41),
але потрібна повна реалізація:
- `\033[?1049h` / `\033[?1049l` — alternate screen on/off
- `\033[?25l` / `\033[?25h` — hide/show cursor
- `\033[2J` `\033[1;1H` — clear screen
- `\033[r;cH` — cursor position (вже є в AnsiBackend)

**Файли:** `backend/AnsiWriter.java` — виділити ANSI output у окремий клас,
щоб використовувати і в `NativeBackend`, і окремо в тестах.
**Пріоритет:** MED | **Складність:** LOW

---

### NTASK-04: Terminal Size Detection
**Суть:** Отримати ширину і висоту терміналу в колонках/рядках.

**Підходи:**
- A) POSIX: `ioctl(TIOCGWINSZ)` через JNA → надійно, без зовнішніх процесів.
- B) Unix fallback: `stty size` через ProcessBuilder.
- C) Windows: `GetConsoleScreenBufferInfo` через kernel32.dll.
- D) ANSI cursor position hack: `\033[999;999H\033[6n` → відповідь `\033[r;cR`.

**Файли:** `size/TerminalSizeDetector.java`
**API:**
```java
public record TerminalSize(int cols, int rows) {}

public class TerminalSizeDetector {
    public static TerminalSize detect();        // автовибір стратегії
    public static TerminalSize detectPosix();   // ioctl
    public static TerminalSize detectWindows(); // kernel32
    public static TerminalSize detectAnsi(InputStream in, OutputStream out); // escape hack
}
```
**Тести:** `TerminalSizeDetectorTest` (з мокованим stdin/stdout для ANSI-варіанту)
**Пріоритет:** HIGH | **Складність:** MED

---

### NTASK-05: SIGWINCH / Resize Detection
**Суть:** На Unix, зміна розміру терміналу надсилає SIGWINCH процесу.
Потрібно зловити сигнал і викликати `resizeListener`.

**Підходи:**
- A) JNA `signal(SIGWINCH, handler)` → точно, але складно з threading.
- B) Polling: в окремому daemon thread раз на 200ms перевіряти `TerminalSizeDetector.detect()`.
  Менш елегантно, але не потребує signal handling і працює скрізь.
- C) Windows: polling або `ReadConsoleInput` (WINDOW_BUFFER_SIZE_EVENT).

Рекомендація: почати з polling (B), потім SIGWINCH (A) як оптимізація.

**Файли:** `signal/ResizePoller.java`, `signal/SigwinchWatcher.java`
**Пріоритет:** MED | **Складність:** MED

---

### NTASK-06: Key Input Decoder — повний парсер ANSI-послідовностей
**Суть:** Розширити `AnsiBackend.decodeInput()` (TASK-41) до повного парсера.

AnsiBackend вже декодує базові клавіші. Потрібно додати:
- `\033[1;5A` → Ctrl+↑, `\033[1;2A` → Shift+↑ (modifier в escape sequence)
- `\033[3;2~` → Shift+Delete, `\033[5;5~` → Ctrl+PageUp тощо
- `\033[200~` / `\033[201~` — bracketed paste mode
- `\033O...` — SS3 sequences (numpad, F1–F4 в деяких терміналах)
- `\033[[A`..`\033[[E` — F1–F5 в Linux console
- Numpad в різних терміналах (різні encoding)

**Файли:** `input/AnsiKeyDecoder.java` — standalone, тестовано незалежно від I/O
**API:**
```java
public class AnsiKeyDecoder {
    public KeyEvent decode(byte[] sequence);
    public static AnsiKeyDecoder create();
}
```
**Тести:** `AnsiKeyDecoderTest` — таблиця ~50 тестових векторів (sequence → KeyEvent)
**Пріоритет:** HIGH | **Складність:** MED

---

### NTASK-07: Mouse Input (SGR Protocol)
**Суть:** Увімкнути SGR mouse reporting через ANSI:
- `\033[?1000h` — enable mouse button events
- `\033[?1002h` — enable mouse motion (button pressed)
- `\033[?1006h` — SGR extended mode (підтримує великі координати)
- Декодувати SGR mouse sequences: `\033[<Cb;Cx;CyM` (press) / `...m` (release)

**Файли:** `input/MouseDecoder.java`, інтеграція в `NativeBackend`
**API:**
```java
public class MouseDecoder {
    public static MouseEvent decode(byte[] sgrSequence); // nullable
}
```
**Тести:** `MouseDecoderTest` — вектори SGR byte sequences → MouseEvent
**Пріоритет:** MED | **Складність:** MED

---

### NTASK-08: Windows — VT Processing
**Суть:** На Windows 10+ ANSI escape codes працюють, але потрібно явно увімкнути
`ENABLE_VIRTUAL_TERMINAL_PROCESSING` в stdout через `SetConsoleMode`.
Без цього всі `\033[...` виводяться як текст.

**Файли:** `raw/WindowsVtOutput.java`
**API:**
```java
public class WindowsVtOutput {
    public static boolean enable();   // повертає false якщо не підтримується
    public static void disable();
}
```
**Пріоритет:** HIGH (без цього Windows взагалі не працює) | **Складність:** LOW

---

### NTASK-09: NativeBackend — фінальна збірка
**Суть:** `NativeBackend implements TerminalBackend` — використовує всі компоненти
NTASK-01..08 і надає той самий інтерфейс, що й `LanternaBackend`.

**Файли:** `backend/NativeBackend.java`
**API:**
```java
public class NativeBackend implements TerminalBackend {
    public static NativeBackend create();   // автовибір POSIX / Windows
    // ... всі методи TerminalBackend
}
```
**init():** raw mode + alternate screen + cursor hide + mouse enable (якщо є)
**shutdown():** відновити terminal state + main screen + cursor show

**Тести:** `NativeBackendIntegrationTest` (з PipedInputStream/PipedOutputStream,
без реального терміналу — аналогічно AnsiBackendTest)
**Пріоритет:** HIGH | **Складність:** MED (збірка готових блоків)

---

### NTASK-10: Платформо-незалежний Factory + OS Detection
**Суть:** `TerminalBackend.create()` або `Backends.createNative()` — детектує ОС
і повертає правильну реалізацію.

```java
public final class Backends {
    public static TerminalBackend createNative() {
        if (isWindows()) return NativeBackend.createWindows();
        return NativeBackend.createPosix();
    }
    public static TerminalBackend createLanterna() { return new LanternaBackend(); }
    public static TerminalBackend createMock(int w, int h) { return new MockBackend(w, h); }
}
```

**Пріоритет:** MED | **Складність:** LOW

---

## Порядок виконання

```
NTASK-08 (Windows VT — швидко, без цього Windows не починає)
NTASK-03 (ANSI output writer — основа для всього)
NTASK-01 (POSIX raw mode)
NTASK-02 (Windows raw mode)
NTASK-04 (terminal size)
NTASK-06 (key decoder)
NTASK-09 (NativeBackend — збирає 01..06)
NTASK-05 (resize detection — можна після 09)
NTASK-07 (mouse — optional, після 09)
NTASK-10 (factory — фінал)
```

**Мінімально робочий результат** (запускається на Windows Terminal):
NTASK-08 → NTASK-03 → NTASK-02 → NTASK-04 → NTASK-06 → NTASK-09

---

## Оцінка обсягу

| Задача   | Складність | ~LOC |
|----------|-----------|------|
| NTASK-01 | MED       | 120  |
| NTASK-02 | MED       | 100  |
| NTASK-03 | LOW       |  80  |
| NTASK-04 | MED       | 150  |
| NTASK-05 | MED       | 100  |
| NTASK-06 | MED       | 200  |
| NTASK-07 | MED       | 120  |
| NTASK-08 | LOW       |  60  |
| NTASK-09 | MED       | 180  |
| NTASK-10 | LOW       |  50  |
| **Total**|           |**~1160** |

**Тести:** ~150 unit тестів (key sequence vectors, size detection, mock I/O).
