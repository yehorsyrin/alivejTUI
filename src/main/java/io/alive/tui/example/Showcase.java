package io.alive.tui.example;

import io.alive.tui.core.Component;
import io.alive.tui.core.Node;
import io.alive.tui.event.EventBus;
import io.alive.tui.node.*;
import io.alive.tui.style.Color;
import io.alive.tui.style.Style;

/**
 * Interactive showcase of all AliveJTUI components.
 *
 * Controls:
 *   [+] / [-]  — adjust progress bar
 *   [Space]    — click counter button
 *   [any key]  — tick spinner
 *   [ESC]      — quit
 */
public class Showcase extends Component {

    private double progress = 0.5;
    private int counter = 0;
    private static final String[] SPINNER_FRAMES = {"|", "/", "-", "\\"};
    private int spinnerFrame = 0;
    private boolean mounted = false;

    @Override
    public void mount(Runnable onStateChange, EventBus eventBus) {
        super.mount(onStateChange, eventBus);
        if (mounted) return;
        mounted = true;

        eventBus.registerCharacter(c -> {
            if (c == '+')
                setState(() -> { progress = Math.min(1.0, progress + 0.1); tick(); });
            else if (c == '-')
                setState(() -> { progress = Math.max(0.0, progress - 0.1); tick(); });
            else if (c == ' ')
                setState(() -> { counter++; tick(); });
            else
                setState(this::tick);
        });

        for (io.alive.tui.event.KeyType k : new io.alive.tui.event.KeyType[]{
                io.alive.tui.event.KeyType.ARROW_UP,
                io.alive.tui.event.KeyType.ARROW_DOWN,
                io.alive.tui.event.KeyType.ARROW_LEFT,
                io.alive.tui.event.KeyType.ARROW_RIGHT,
                io.alive.tui.event.KeyType.ENTER,
                io.alive.tui.event.KeyType.TAB
        }) {
            onKey(k, () -> setState(this::tick));
        }
    }

    private void tick() {
        spinnerFrame = (spinnerFrame + 1) % SPINNER_FRAMES.length;
    }

    @Override
    public Node render() {
        int pct = (int) Math.round(progress * 100);

        return VBox.of(
            // ── Header ──────────────────────────────────────────────────────
            Text.of(" AliveJTUI — Component Showcase").bold().color(Color.CYAN),
            Divider.horizontal(),
            Text.of(""),

            // ── Text styles ──────────────────────────────────────────────────
            Text.of(" TEXT STYLES").bold().color(Color.YELLOW),
            HBox.of(
                Text.of("  Normal  "),
                Text.of("Bold  ").bold(),
                Text.of("Italic  ").italic(),
                Text.of("Dim  ").dim(),
                Text.of("Underline  ").underline(),
                Text.of("Strike").strikethrough()
            ),
            HBox.of(
                Text.of("  "),
                Text.of("Red  ").color(Color.RED),
                Text.of("Green  ").color(Color.GREEN),
                Text.of("Yellow  ").color(Color.YELLOW),
                Text.of("Blue  ").color(Color.BLUE),
                Text.of("Cyan  ").color(Color.CYAN),
                Text.of("Magenta  ").color(Color.MAGENTA),
                Text.of("BrightCyan").color(Color.BRIGHT_CYAN)
            ),
            Text.of(""),
            Divider.horizontal(),
            Text.of(""),

            // ── Layout ───────────────────────────────────────────────────────
            Text.of(" LAYOUT  (HBox + BoxNode)").bold().color(Color.YELLOW),
            HBox.of(
                new BoxNode(VBox.of(
                    Text.of(" Left   ").bold().color(Color.BLUE),
                    Text.of(" alpha  ").color(Color.BRIGHT_BLACK),
                    Text.of(" beta   ").color(Color.BRIGHT_BLACK)
                ), true, Style.DEFAULT.withForeground(Color.BLUE)),
                Text.of("  "),
                new BoxNode(VBox.of(
                    Text.of(" Center ").bold().color(Color.CYAN),
                    Text.of(" gamma  ").color(Color.BRIGHT_BLACK),
                    Text.of(" delta  ").color(Color.BRIGHT_BLACK)
                ), true, Style.DEFAULT.withForeground(Color.CYAN)),
                Text.of("  "),
                new BoxNode(VBox.of(
                    Text.of(" Right  ").bold().color(Color.MAGENTA),
                    Text.of(" epsilon").color(Color.BRIGHT_BLACK),
                    Text.of(" zeta   ").color(Color.BRIGHT_BLACK)
                ), true, Style.DEFAULT.withForeground(Color.MAGENTA))
            ),
            Text.of(""),
            Divider.horizontal(),
            Text.of(""),

            // ── Progress bar ─────────────────────────────────────────────────
            HBox.of(
                Text.of(" PROGRESS BAR").bold().color(Color.YELLOW),
                Text.of("  [+] increase  [-] decrease").dim()
            ),
            HBox.of(
                Text.of("  "),
                new ProgressBarNode(progress),
                Text.of("  " + pct + "%")
                    .bold()
                    .color(pct >= 100 ? Color.GREEN : pct <= 0 ? Color.RED : Color.WHITE)
            ),
            Text.of(""),
            Divider.horizontal(),
            Text.of(""),

            // ── Spinner ──────────────────────────────────────────────────────
            HBox.of(
                Text.of(" SPINNER").bold().color(Color.YELLOW),
                Text.of("  [any key] to tick").dim()
            ),
            HBox.of(
                Text.of("  " + SPINNER_FRAMES[spinnerFrame] + "  Processing...").color(Color.BRIGHT_BLACK)
            ),
            Text.of(""),
            Divider.horizontal(),
            Text.of(""),

            // ── Button + Counter ─────────────────────────────────────────────
            HBox.of(
                Text.of(" BUTTON  +  COUNTER").bold().color(Color.YELLOW),
                Text.of("  [Space] to click").dim()
            ),
            HBox.of(
                Text.of("  "),
                Button.of("Increment", () -> {}),
                Text.of("   Count: "),
                Text.of(String.valueOf(counter)).bold().color(Color.GREEN)
            ),
            Text.of(""),
            Divider.horizontal(),

            // ── Footer ───────────────────────────────────────────────────────
            Text.of("  [ESC] Quit").dim()
        );
    }
}
