package io.alive.tui.nativeio.input;

import io.alive.tui.event.MouseEvent;
import io.alive.tui.event.MouseType;

/**
 * Decodes SGR mouse sequences into {@link MouseEvent} objects.
 *
 * <p>SGR (parameter 1006) mouse encoding:
 * <pre>
 *   Press:   ESC [ &lt; Cb ; Cx ; Cy M
 *   Release: ESC [ &lt; Cb ; Cx ; Cy m
 * </pre>
 * where:
 * <ul>
 *   <li>{@code Cb} — button bitmask (0=left, 1=middle, 2=right, 64=scroll-up, 65=scroll-down)</li>
 *   <li>{@code Cx} — 1-based column</li>
 *   <li>{@code Cy} — 1-based row</li>
 *   <li>Final {@code M} = press, {@code m} = release</li>
 * </ul>
 *
 * <p>Usage: call {@link #decode(byte[])} with the raw bytes of the escape sequence
 * (starting with {@code ESC [ <}).
 *
 * @author Jarvis (AI)
 */
public final class MouseDecoder {

    private MouseDecoder() {}

    /**
     * Decodes a raw SGR mouse sequence.
     *
     * @param sgrSequence the complete sequence bytes starting with {@code ESC [ <}.
     *                    Must not be {@code null}.
     * @return the decoded {@link MouseEvent}, or {@code null} if the sequence is
     *         not a valid SGR mouse sequence.
     */
    public static MouseEvent decode(byte[] sgrSequence) {
        if (sgrSequence == null || sgrSequence.length < 6) return null;

        // Expect: ESC '[' '<' params... ('M' | 'm')
        if ((sgrSequence[0] & 0xFF) != 0x1B) return null;
        if ((sgrSequence[1] & 0xFF) != '[')  return null;
        if ((sgrSequence[2] & 0xFF) != '<')  return null;

        char finalByte = (char)(sgrSequence[sgrSequence.length - 1] & 0xFF);
        if (finalByte != 'M' && finalByte != 'm') return null;

        // Extract parameter string between '<' and final byte
        byte[] paramBytes = new byte[sgrSequence.length - 4]; // skip ESC '[' '<' and final
        System.arraycopy(sgrSequence, 3, paramBytes, 0, paramBytes.length);
        String params = new String(paramBytes, java.nio.charset.StandardCharsets.US_ASCII);

        String[] parts = params.split(";");
        if (parts.length != 3) return null;

        try {
            int cb = Integer.parseInt(parts[0].trim());
            int cx = Integer.parseInt(parts[1].trim()) - 1; // convert to 0-based
            int cy = Integer.parseInt(parts[2].trim()) - 1;

            boolean press = finalByte == 'M';
            return buildEvent(cb, cx, cy, press);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Decodes an SGR mouse sequence from a plain string representation
     * (for convenience in tests and logging).
     *
     * <p>Accepts strings like {@code "\033[<0;10;5M"} or {@code "\033[<64;1;1M"}.
     *
     * @param sgrString the sequence as a string
     * @return the decoded event or {@code null}
     */
    public static MouseEvent decodeString(String sgrString) {
        if (sgrString == null) return null;
        return decode(sgrString.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
    }

    // --- Private ---

    private static MouseEvent buildEvent(int cb, int col, int row, boolean press) {
        // Scroll events: Cb bit 6 set (64 = scroll up, 65 = scroll down)
        if ((cb & 64) != 0) {
            int direction = cb & 1;
            return direction == 0
                    ? MouseEvent.scrollUp(col, row)
                    : MouseEvent.scrollDown(col, row);
        }

        // Button: lower 2 bits (0=left, 1=middle, 2=right, 3=none/release)
        int button = cb & 3;
        MouseType type = press ? MouseType.PRESS : MouseType.RELEASE;
        return new MouseEvent(type, col, row, button);
    }
}
