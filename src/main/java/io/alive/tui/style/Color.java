package io.alive.tui.style;

/**
 * Terminal color representation supporting ANSI 16, 256-color, and true color (RGB).
 *
 * @author Jarvis (AI)
 */
public final class Color {

    public enum ColorType { ANSI_16, ANSI_256, RGB }

    private final ColorType type;
    private final int ansiIndex;
    private final int r, g, b;

    private Color(ColorType type, int ansiIndex, int r, int g, int b) {
        this.type = type;
        this.ansiIndex = ansiIndex;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    // --- Standard ANSI 16 colors ---
    public static final Color BLACK         = ansi16(0);
    public static final Color RED           = ansi16(1);
    public static final Color GREEN         = ansi16(2);
    public static final Color YELLOW        = ansi16(3);
    public static final Color BLUE          = ansi16(4);
    public static final Color MAGENTA       = ansi16(5);
    public static final Color CYAN          = ansi16(6);
    public static final Color WHITE         = ansi16(7);
    public static final Color BRIGHT_BLACK  = ansi16(8);
    public static final Color BRIGHT_RED    = ansi16(9);
    public static final Color BRIGHT_GREEN  = ansi16(10);
    public static final Color BRIGHT_YELLOW = ansi16(11);
    public static final Color BRIGHT_BLUE   = ansi16(12);
    public static final Color BRIGHT_MAGENTA= ansi16(13);
    public static final Color BRIGHT_CYAN   = ansi16(14);
    public static final Color BRIGHT_WHITE  = ansi16(15);

    private static Color ansi16(int index) {
        return new Color(ColorType.ANSI_16, index, 0, 0, 0);
    }

    public static Color ansi256(int index) {
        if (index < 0 || index > 255) throw new IllegalArgumentException("ANSI 256 index must be 0-255");
        return new Color(ColorType.ANSI_256, index, 0, 0, 0);
    }

    public static Color rgb(int r, int g, int b) {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            throw new IllegalArgumentException("RGB values must be 0-255");
        }
        return new Color(ColorType.RGB, 0, r, g, b);
    }

    public ColorType getType() { return type; }
    public int getAnsiIndex() { return ansiIndex; }
    public int getR() { return r; }
    public int getG() { return g; }
    public int getB() { return b; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Color c)) return false;
        return type == c.type && ansiIndex == c.ansiIndex && r == c.r && g == c.g && b == c.b;
    }

    @Override
    public int hashCode() {
        return type.hashCode() * 31 * 31 * 31 + ansiIndex * 31 * 31 + r * 31 + g + b;
    }

    @Override
    public String toString() {
        return switch (type) {
            case ANSI_16 -> "ANSI_16(" + ansiIndex + ")";
            case ANSI_256 -> "ANSI_256(" + ansiIndex + ")";
            case RGB -> "RGB(" + r + "," + g + "," + b + ")";
        };
    }
}
