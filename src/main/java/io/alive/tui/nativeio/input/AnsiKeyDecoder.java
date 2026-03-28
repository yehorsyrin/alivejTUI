package io.alive.tui.nativeio.input;

import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;

import java.util.HashMap;
import java.util.Map;

/**
 * Standalone ANSI escape sequence → {@link KeyEvent} decoder.
 *
 * <p>Accepts a byte array representing a single key press (which may be a
 * multi-byte escape sequence) and returns the corresponding {@link KeyEvent}.
 *
 * <p>Supported sequence families:
 * <ul>
 *   <li>Plain ASCII printable and control characters</li>
 *   <li>CSI sequences: {@code ESC [ ... final} (arrows, function keys, modifiers)</li>
 *   <li>SS3 sequences: {@code ESC O ...} (numpad, F1–F4 on many terminals)</li>
 *   <li>Linux console F1–F5: {@code ESC [ [ A..E}</li>
 *   <li>Modifier-encoded sequences: {@code ESC [ 1 ; mod final}</li>
 *   <li>Bracketed paste markers: {@code ESC [ 200 ~} / {@code ESC [ 201 ~}</li>
 * </ul>
 *
 * <p>Thread-safety: immutable after construction — safe to share.
 *
 * @author Jarvis (AI)
 */
public final class AnsiKeyDecoder {

    // --- CSI final-byte dispatch tables ---

    /** ESC [ {final}  — simple one-char final sequences */
    private static final Map<Character, KeyType> CSI_SIMPLE = new HashMap<>();

    /** ESC [ {digit} ~  — tilde sequences (VT220 style) */
    private static final Map<Integer, KeyType> CSI_TILDE = new HashMap<>();

    /** ESC O {final}  — SS3 sequences */
    private static final Map<Character, KeyType> SS3 = new HashMap<>();

    static {
        // CSI simple
        CSI_SIMPLE.put('A', KeyType.ARROW_UP);
        CSI_SIMPLE.put('B', KeyType.ARROW_DOWN);
        CSI_SIMPLE.put('C', KeyType.ARROW_RIGHT);
        CSI_SIMPLE.put('D', KeyType.ARROW_LEFT);
        CSI_SIMPLE.put('H', KeyType.HOME);
        CSI_SIMPLE.put('F', KeyType.END);
        CSI_SIMPLE.put('Z', KeyType.SHIFT_TAB);  // ESC [ Z = Shift+Tab (rxvt/xterm)

        // CSI tilde sequences
        CSI_TILDE.put(1,  KeyType.HOME);       // ESC [ 1 ~
        CSI_TILDE.put(2,  KeyType.EOF);        // ESC [ 2 ~ = Insert (no KeyType, map to EOF)
        CSI_TILDE.put(3,  KeyType.DELETE);     // ESC [ 3 ~
        CSI_TILDE.put(4,  KeyType.END);        // ESC [ 4 ~
        CSI_TILDE.put(5,  KeyType.PAGE_UP);    // ESC [ 5 ~
        CSI_TILDE.put(6,  KeyType.PAGE_DOWN);  // ESC [ 6 ~
        CSI_TILDE.put(7,  KeyType.HOME);       // ESC [ 7 ~ (rxvt)
        CSI_TILDE.put(8,  KeyType.END);        // ESC [ 8 ~ (rxvt)
        CSI_TILDE.put(11, KeyType.EOF);        // ESC [ 11 ~ = F1 (no F-key type)
        CSI_TILDE.put(12, KeyType.EOF);        // F2
        CSI_TILDE.put(13, KeyType.EOF);        // F3
        CSI_TILDE.put(14, KeyType.EOF);        // F4
        CSI_TILDE.put(15, KeyType.EOF);        // F5
        CSI_TILDE.put(17, KeyType.EOF);        // F6
        CSI_TILDE.put(18, KeyType.EOF);        // F7
        CSI_TILDE.put(19, KeyType.EOF);        // F8
        CSI_TILDE.put(20, KeyType.EOF);        // F9
        CSI_TILDE.put(21, KeyType.EOF);        // F10
        CSI_TILDE.put(23, KeyType.EOF);        // F11
        CSI_TILDE.put(24, KeyType.EOF);        // F12
        CSI_TILDE.put(200, KeyType.EOF);       // bracketed paste begin
        CSI_TILDE.put(201, KeyType.EOF);       // bracketed paste end

        // SS3 (ESC O)
        SS3.put('A', KeyType.ARROW_UP);
        SS3.put('B', KeyType.ARROW_DOWN);
        SS3.put('C', KeyType.ARROW_RIGHT);
        SS3.put('D', KeyType.ARROW_LEFT);
        SS3.put('H', KeyType.HOME);
        SS3.put('F', KeyType.END);
        SS3.put('P', KeyType.EOF);  // F1
        SS3.put('Q', KeyType.EOF);  // F2
        SS3.put('R', KeyType.EOF);  // F3
        SS3.put('S', KeyType.EOF);  // F4
    }

    private AnsiKeyDecoder() {}

    /** Creates a new {@code AnsiKeyDecoder}. */
    public static AnsiKeyDecoder create() {
        return new AnsiKeyDecoder();
    }

    /**
     * Decodes a byte sequence into a {@link KeyEvent}.
     *
     * @param sequence raw bytes as read from the terminal input stream.
     *                 Must not be {@code null}.
     * @return the decoded key event; never {@code null}.
     *         Returns {@link KeyEvent#of(KeyType) KeyEvent.of(EOF)} for unknown sequences.
     */
    public KeyEvent decode(byte[] sequence) {
        if (sequence == null || sequence.length == 0) return KeyEvent.of(KeyType.EOF);

        int first = sequence[0] & 0xFF;

        // --- ESC sequences ---
        if (first == 0x1B) {
            if (sequence.length == 1) return KeyEvent.of(KeyType.ESCAPE);
            int second = sequence[1] & 0xFF;

            // SS3: ESC O {x}
            if (second == 'O' && sequence.length >= 3) {
                char final_ = (char)(sequence[2] & 0xFF);
                KeyType kt = SS3.get(final_);
                return kt != null ? KeyEvent.of(kt) : KeyEvent.of(KeyType.ESCAPE);
            }

            // CSI: ESC [ ...
            if (second == '[' && sequence.length >= 3) {
                return decodeCsi(sequence);
            }

            // Alt + key: ESC {printable}
            if (second >= 0x20 && second < 0x7F) {
                return KeyEvent.ofCharacter((char) second, false, true, false);
            }

            return KeyEvent.of(KeyType.ESCAPE);
        }

        // --- Plain ASCII ---
        return switch (first) {
            case 13  -> KeyEvent.of(KeyType.ENTER);
            case 127 -> KeyEvent.of(KeyType.BACKSPACE);
            case 9   -> KeyEvent.of(KeyType.TAB);
            default  -> first >= 32
                    ? KeyEvent.ofCharacter((char) first)
                    : KeyEvent.of(KeyType.EOF);
        };
    }

    // --- CSI decoder ---

    private KeyEvent decodeCsi(byte[] seq) {
        // seq[0]=ESC, seq[1]='[', seq[2..n-1]=params, seq[n-1]=final byte

        // Linux console F1–F5: ESC [ [ {A..E}
        if (seq.length == 4 && (seq[2] & 0xFF) == '[') {
            // These are all function keys — no KeyType for them
            return KeyEvent.of(KeyType.EOF);
        }

        char finalByte = (char)(seq[seq.length - 1] & 0xFF);
        String params = extractParams(seq); // everything between '[' and final byte

        // --- No params: ESC [ {final} ---
        if (params.isEmpty()) {
            KeyType kt = CSI_SIMPLE.get(finalByte);
            return kt != null ? KeyEvent.of(kt) : KeyEvent.of(KeyType.ESCAPE);
        }

        // --- Modifier sequences: ESC [ 1 ; {mod} {final} ---
        // e.g. ESC[1;5A = Ctrl+Up, ESC[1;2A = Shift+Up, ESC[1;3A = Alt+Up
        if (params.contains(";")) {
            String[] parts = params.split(";");
            if (parts.length == 2) {
                int mod = parseIntSafe(parts[1]);
                KeyType baseKey = CSI_SIMPLE.get(finalByte);
                if (baseKey != null) {
                    return applyModifier(baseKey, mod);
                }
                // Tilde with modifier: ESC [ {n} ; {mod} ~
                if (finalByte == '~') {
                    int n = parseIntSafe(parts[0]);
                    KeyType kt = CSI_TILDE.getOrDefault(n, KeyType.EOF);
                    return applyModifier(kt, mod);
                }
            }
            return KeyEvent.of(KeyType.ESCAPE);
        }

        // --- Tilde sequences: ESC [ {n} ~ ---
        if (finalByte == '~') {
            int n = parseIntSafe(params);
            KeyType kt = CSI_TILDE.getOrDefault(n, KeyType.EOF);
            return KeyEvent.of(kt);
        }

        // --- Simple CSI with numeric prefix (e.g. ESC [ 2 H) ---
        KeyType kt = CSI_SIMPLE.get(finalByte);
        return kt != null ? KeyEvent.of(kt) : KeyEvent.of(KeyType.ESCAPE);
    }

    /** Extracts the parameter substring (between '[' and the final byte). */
    private static String extractParams(byte[] seq) {
        // seq = ESC '[' params... finalByte
        // params start at index 2, end at index seq.length-2
        if (seq.length <= 3) return "";
        byte[] params = new byte[seq.length - 3];
        System.arraycopy(seq, 2, params, 0, params.length);
        return new String(params, java.nio.charset.StandardCharsets.US_ASCII);
    }

    /**
     * Applies a modifier bitmask to a key type.
     *
     * <p>Modifier encoding (xterm standard):
     * <pre>
     *   mod = shift + 2*alt + 4*ctrl - 1
     *   1 = none, 2 = shift, 3 = alt, 4 = alt+shift,
     *   5 = ctrl, 6 = ctrl+shift, 7 = ctrl+alt, 8 = ctrl+alt+shift
     * </pre>
     */
    private static KeyEvent applyModifier(KeyType kt, int mod) {
        boolean shift = ((mod - 1) & 1) != 0;
        boolean alt   = ((mod - 1) & 2) != 0;
        boolean ctrl  = ((mod - 1) & 4) != 0;
        return new KeyEvent(kt, '\0', ctrl, alt, shift);
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return -1; }
    }
}
