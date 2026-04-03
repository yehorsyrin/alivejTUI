package io.github.yehorsyrin.tui.style;

import io.github.yehorsyrin.tui.core.AliveJTUI;
import io.github.yehorsyrin.tui.style.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Theme} and {@link AliveJTUI} theme integration.
 *
 * @author Jarvis (AI)
 */
class ThemeTest {

    @AfterEach
    void resetTheme() {
        AliveJTUI.setTheme(Theme.DARK); // restore default
    }

    // --- Built-in themes return non-null styles ---

    @Test
    void darkTheme_allStylesNonNull() {
        Theme t = Theme.DARK;
        assertNotNull(t.foreground());
        assertNotNull(t.muted());
        assertNotNull(t.primary());
        assertNotNull(t.secondary());
        assertNotNull(t.success());
        assertNotNull(t.warning());
        assertNotNull(t.error());
        assertNotNull(t.focused());
    }

    @Test
    void lightTheme_allStylesNonNull() {
        Theme t = Theme.LIGHT;
        assertNotNull(t.foreground());
        assertNotNull(t.muted());
        assertNotNull(t.primary());
        assertNotNull(t.secondary());
        assertNotNull(t.success());
        assertNotNull(t.warning());
        assertNotNull(t.error());
        assertNotNull(t.focused());
    }

    @Test
    void darkTheme_primary_isBold() {
        assertTrue(Theme.DARK.primary().isBold());
    }

    @Test
    void darkTheme_muted_isDim() {
        assertTrue(Theme.DARK.muted().isDim());
    }

    @Test
    void darkTheme_and_lightTheme_primaryDiffer() {
        assertNotEquals(Theme.DARK.primary(), Theme.LIGHT.primary());
    }

    // --- AliveJTUI.setTheme / getTheme ---

    @Test
    void defaultTheme_isDark() {
        assertEquals(Theme.DARK, AliveJTUI.getTheme());
    }

    @Test
    void setTheme_light_returnedByGetTheme() {
        AliveJTUI.setTheme(Theme.LIGHT);
        assertEquals(Theme.LIGHT, AliveJTUI.getTheme());
    }

    @Test
    void setTheme_null_resetsToDefault() {
        AliveJTUI.setTheme(Theme.LIGHT);
        AliveJTUI.setTheme(null);
        assertEquals(Theme.DARK, AliveJTUI.getTheme());
    }

    @Test
    void setTheme_custom_works() {
        Theme custom = new Theme.BuiltinTheme(
                Style.DEFAULT, Style.DEFAULT, Style.DEFAULT, Style.DEFAULT,
                Style.DEFAULT, Style.DEFAULT, Style.DEFAULT, Style.DEFAULT
        );
        AliveJTUI.setTheme(custom);
        assertSame(custom, AliveJTUI.getTheme());
    }

    // --- BuiltinTheme ---

    @Test
    void builtinTheme_foreground_returnsConfiguredStyle() {
        Style fg = Style.DEFAULT.withBold(true);
        Theme.BuiltinTheme t = new Theme.BuiltinTheme(
                fg, Style.DEFAULT, Style.DEFAULT, Style.DEFAULT,
                Style.DEFAULT, Style.DEFAULT, Style.DEFAULT, Style.DEFAULT
        );
        assertEquals(fg, t.foreground());
    }

    // --- background() ---

    @Test
    void darkTheme_background_isNull() {
        assertNull(Theme.DARK.background());
    }

    @Test
    void lightTheme_background_isNotNull() {
        assertNotNull(Theme.LIGHT.background());
    }

    @Test
    void builtinTheme_9argConstructor_storesBackground() {
        Theme.BuiltinTheme t = new Theme.BuiltinTheme(
                Style.DEFAULT, Style.DEFAULT, Style.DEFAULT, Style.DEFAULT,
                Style.DEFAULT, Style.DEFAULT, Style.DEFAULT, Style.DEFAULT,
                Color.CYAN
        );
        assertEquals(Color.CYAN, t.background());
    }

    @Test
    void themeInterface_defaultBackground_isNull() {
        // Anonymous implementation that uses the default background() method
        Theme minimal = new Theme() {
            @Override public Style foreground() { return Style.DEFAULT; }
            @Override public Style muted()      { return Style.DEFAULT; }
            @Override public Style primary()    { return Style.DEFAULT; }
            @Override public Style secondary()  { return Style.DEFAULT; }
            @Override public Style success()    { return Style.DEFAULT; }
            @Override public Style warning()    { return Style.DEFAULT; }
            @Override public Style error()      { return Style.DEFAULT; }
            @Override public Style focused()    { return Style.DEFAULT; }
        };
        assertNull(minimal.background());
    }
}
