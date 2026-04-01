package io.alive.tui.platform.input;

import io.alive.tui.event.KeyEvent;
import io.alive.tui.event.KeyType;

/**
 * Decodes raw ANSI/VT byte sequences into {@link KeyEvent} objects.
 *
 * <p>Supports:
 * <ul>
 *   <li>Printable characters (including non-ASCII UTF-8 code points decoded
 *       before calling this class).</li>
 *   <li>Control characters: Enter, Backspace, Tab, Escape, EOF signals.</li>
 *   <li>Ctrl+letter combinations ({@code \001}–{@code \032}).</li>
 *   <li>CSI sequences: {@code ESC [ ... final} — arrows, Home, End, Delete,
 *       Page Up/Down, Shift+Tab, and all variants with modifier parameters.</li>
 *   <li>SS3 sequences: {@code ESC O A/B/C/D/H/F} — alternate arrow encoding
 *       used by some terminal emulators.</li>
 * </ul>
 *
 * <p>This class is stateless and thread-safe.  Each call to {@link #decode}
 * is independent.
 *
 * @author Jarvis (AI)
 */
public final class AnsiKeyDecoder {

    // ── Modifier bit-mask (1-based from ANSI spec, subtract 1 for bit index) ─

    private static final int MOD_SHIFT  = 1; // modifier param bit 0
    private static final int MOD_ALT    = 2; // modifier param bit 1
    private static final int MOD_CTRL   = 4; // modifier param bit 2

    private AnsiKeyDecoder() {}

    /**
     * Decodes a byte sequence read from the terminal into a {@link KeyEvent}.
     *
     * @param seq raw bytes as read from stdin
     * @return the decoded event, never {@code null}
     */
    public static KeyEvent decode(byte[] seq) {
        if (seq == null || seq.length == 0) return KeyEvent.of(KeyType.EOF);

        byte first = seq[0];

        // ── Single byte ──────────────────────────────────────────────────────

        if (seq.length == 1) {
            return decodeSingle(first);
        }

        // ── Multi-byte starting with ESC (0x1B) ───────────────────────────────

        if (first == 0x1B && seq.length >= 2) {
            byte second = seq[1];

            // ESC alone (seq[0]=ESC, rest is a separate sequence that arrived
            // simultaneously) — treat as bare ESCAPE
            if (seq.length == 1) return KeyEvent.of(KeyType.ESCAPE);

            // CSI: ESC [
            if (second == '[' && seq.length >= 3) {
                return decodeCsi(seq);
            }

            // SS3: ESC O
            if (second == 'O' && seq.length >= 3) {
                return decodeSs3(seq[2]);
            }

            // ESC alone (only two bytes where second was appended as timing artefact)
            if (seq.length == 2 && second == 0x1B) {
                return KeyEvent.of(KeyType.ESCAPE);
            }

            // Alt+key: ESC + printable
            if (seq.length == 2) {
                KeyEvent inner = decodeSingle(second);
                return new KeyEvent(inner.type(), inner.character(),
                        inner.ctrl(), true /* alt */, inner.shift());
            }
        }

        // ── Fallback: treat as character sequence ─────────────────────────────
        return KeyEvent.ofCharacter((char) (first & 0xFF));
    }

    // ── Single-byte decoder ───────────────────────────────────────────────────

    private static KeyEvent decodeSingle(byte b) {
        return switch (b) {
            case 0x0D, 0x0A -> KeyEvent.of(KeyType.ENTER);        // CR or LF
            case 0x7F, 0x08 -> KeyEvent.of(KeyType.BACKSPACE);    // DEL or BS
            case 0x09       -> KeyEvent.of(KeyType.TAB);           // HT
            case 0x1B       -> KeyEvent.of(KeyType.ESCAPE);        // ESC
            case 0x00       -> KeyEvent.of(KeyType.EOF);           // NUL
            case 0x04       -> KeyEvent.of(KeyType.EOF);           // Ctrl+D
            default -> {
                if (b >= 0x01 && b <= 0x1A) {
                    // Ctrl+A through Ctrl+Z → ctrl=true, character='a'-'z'
                    char ch = (char) ('a' + b - 1);
                    yield KeyEvent.ofCharacter(ch, true, false, false);
                }
                if (b >= 0x20) {
                    yield KeyEvent.ofCharacter((char) (b & 0xFF));
                }
                yield KeyEvent.of(KeyType.EOF);
            }
        };
    }

    // ── CSI decoder (ESC [ ...) ───────────────────────────────────────────────

    /**
     * Decodes CSI sequences of the form {@code ESC [ [param[;param]] final}.
     * seq[0]=ESC, seq[1]='['
     */
    private static KeyEvent decodeCsi(byte[] seq) {
        // Extract the body: bytes from index 2 to end
        // Final byte is the last byte; parameters are everything before it
        byte finalByte = seq[seq.length - 1];
        String params = extractParams(seq, 2, seq.length - 1);

        // ESC [ Z → Shift+Tab
        if (finalByte == 'Z') {
            return KeyEvent.of(KeyType.SHIFT_TAB);
        }

        // Modifier (present when params contains ';')
        int[] modParts = parseModifier(params);
        int n       = modParts[0]; // first numeric param (or 0)
        int modCode = modParts[1]; // modifier param (1 = no modifier)
        boolean shift = hasModifierFlag(modCode, MOD_SHIFT);
        boolean alt   = hasModifierFlag(modCode, MOD_ALT);
        boolean ctrl  = hasModifierFlag(modCode, MOD_CTRL);

        // ESC [ A/B/C/D/H/F — arrows and home/end (with optional 1;mod prefix)
        return switch (finalByte) {
            case 'A' -> KeyEvent.of(KeyType.ARROW_UP,    ctrl, alt, shift);
            case 'B' -> KeyEvent.of(KeyType.ARROW_DOWN,  ctrl, alt, shift);
            case 'C' -> KeyEvent.of(KeyType.ARROW_RIGHT, ctrl, alt, shift);
            case 'D' -> KeyEvent.of(KeyType.ARROW_LEFT,  ctrl, alt, shift);
            case 'H' -> KeyEvent.of(KeyType.HOME,        ctrl, alt, shift);
            case 'F' -> KeyEvent.of(KeyType.END,         ctrl, alt, shift);
            case '~' -> decodeTilde(n, ctrl, alt, shift);
            default  -> KeyEvent.ofCharacter((char) finalByte);
        };
    }

    /** Decodes {@code ESC [ n ~} tilde sequences. */
    private static KeyEvent decodeTilde(int n, boolean ctrl, boolean alt, boolean shift) {
        return switch (n) {
            case 1, 7 -> KeyEvent.of(KeyType.HOME,      ctrl, alt, shift);
            case 4, 8 -> KeyEvent.of(KeyType.END,       ctrl, alt, shift);
            case 3    -> KeyEvent.of(KeyType.DELETE,     ctrl, alt, shift);
            case 5    -> KeyEvent.of(KeyType.PAGE_UP,    ctrl, alt, shift);
            case 6    -> KeyEvent.of(KeyType.PAGE_DOWN,  ctrl, alt, shift);
            case 2    -> KeyEvent.ofCharacter('\0');   // Insert — no KeyType, ignore
            default   -> KeyEvent.of(KeyType.ESCAPE);
        };
    }

    // ── SS3 decoder (ESC O ...) ───────────────────────────────────────────────

    private static KeyEvent decodeSs3(byte b) {
        return switch (b) {
            case 'A' -> KeyEvent.of(KeyType.ARROW_UP);
            case 'B' -> KeyEvent.of(KeyType.ARROW_DOWN);
            case 'C' -> KeyEvent.of(KeyType.ARROW_RIGHT);
            case 'D' -> KeyEvent.of(KeyType.ARROW_LEFT);
            case 'H' -> KeyEvent.of(KeyType.HOME);
            case 'F' -> KeyEvent.of(KeyType.END);
            default  -> KeyEvent.ofCharacter((char) b);
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Extracts the parameter string from {@code seq[from..to)}. */
    private static String extractParams(byte[] seq, int from, int to) {
        if (from >= to) return "";
        StringBuilder sb = new StringBuilder(to - from);
        for (int i = from; i < to; i++) sb.append((char) seq[i]);
        return sb.toString();
    }

    /**
     * Parses a CSI parameter string into {@code [n, modCode]}.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code ""} → [0, 1]</li>
     *   <li>{@code "3"} → [3, 1]</li>
     *   <li>{@code "1;5"} → [1, 5]</li>
     *   <li>{@code "3;2"} → [3, 2]</li>
     * </ul>
     */
    private static int[] parseModifier(String params) {
        if (params.isEmpty()) return new int[]{0, 1};
        int semicolon = params.indexOf(';');
        if (semicolon < 0) {
            return new int[]{parseInt(params, 0), 1};
        }
        int n   = parseInt(params.substring(0, semicolon), 0);
        int mod = parseInt(params.substring(semicolon + 1), 1);
        return new int[]{n, mod};
    }

    private static int parseInt(String s, int defaultValue) {
        try {
            return s.isEmpty() ? defaultValue : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns true if the given modifier flag is set in {@code modCode}.
     *
     * <p>ANSI modifier code is 1-based: the actual bitmask is {@code modCode - 1}.
     * Bit 0 = Shift, bit 1 = Alt, bit 2 = Ctrl.
     */
    private static boolean hasModifierFlag(int modCode, int flag) {
        if (modCode <= 1) return false;
        return ((modCode - 1) & flag) != 0;
    }
}
