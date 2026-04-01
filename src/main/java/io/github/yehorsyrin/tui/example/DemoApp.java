package io.github.yehorsyrin.tui.example;

import io.github.yehorsyrin.tui.core.*;
import io.github.yehorsyrin.tui.event.EventBus;
import io.github.yehorsyrin.tui.event.KeyType;
import io.github.yehorsyrin.tui.node.*;
import io.github.yehorsyrin.tui.style.Color;
import io.github.yehorsyrin.tui.style.Style;
import io.github.yehorsyrin.tui.style.Theme;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * Comprehensive AliveJTUI demo — showcases every major component and feature.
 *
 * <h2>Navigation</h2>
 * <pre>
 *   1-5   Switch tab
 *   T     Toggle Dark/Light theme
 *   D     Show dialog
 *   N     Show notification
 *   Up/Down  Navigate lists / tables
 *   +/-   Progress bar
 *   ESC   Quit
 * </pre>
 *
 * @author Jarvis (AI)
 */
public class DemoApp extends Component {

    // --- Tabs ---
    private static final String[] TAB_NAMES = {
        "1:Widgets", "2:Table", "3:VirtualList", "4:Text", "5:Layout"
    };
    private int activeTab = 0;

    // --- Tab 1: Widgets ---
    private double  progress   = 0.4;
    private int     clickCount = 0;
    private int     spinFrame  = 0;
    private boolean cbChecked  = true;
    private int     radioIdx   = 0;
    private int     selectIdx  = 0;
    private String  inputText  = "";

    private static final String[] SPIN = { "|", "/", "-", "\\" };
    private static final String[] COLORS_OPT = { "Red", "Green", "Blue", "Cyan", "Magenta" };

    // --- Tab 2: Table ---
    private int tableRow = 0;
    private static final List<List<String>> TABLE_DATA = List.of(
        List.of("Alice",   "Engineering", "Senior",   "Berlin"),
        List.of("Bob",     "Design",      "Lead",     "London"),
        List.of("Carol",   "Product",     "Director", "NYC"),
        List.of("Dave",    "Engineering", "Junior",   "Tokyo"),
        List.of("Eve",     "Marketing",   "Manager",  "Paris"),
        List.of("Frank",   "Engineering", "Senior",   "Seoul"),
        List.of("Grace",   "Design",      "Junior",   "Sydney"),
        List.of("Heidi",   "Product",     "Senior",   "Toronto")
    );

    // --- Tab 3: VirtualList ---
    private final List<String> bigList = IntStream.range(1, 10_001)
            .mapToObj(i -> String.format("  %5d  |  Item number %d", i, i))
            .collect(toList());
    private final VirtualListNode vList = VirtualList.of(bigList, 12);

    // --- Tab 5: Viewport ---
    private final ViewportNode viewport;

    // --- Tab 1: Button ---
    private final ButtonNode clickBtn;

    // --- Notifications ---
    private final NotificationManager notifications;

    // --- Overlay management ---
    // dialogNode != null means dialog is showing; notification overlay is separate
    private Node dialogNode   = null;
    private boolean notifShowing = false;

    // --- Layout collapsible ---
    private boolean colExpanded = true;

    public DemoApp() {
        notifications = new NotificationManager(() -> setState(() -> {}));
        clickBtn = new ButtonNode("[ Click Me! ]", () -> setState(() -> {
            clickCount++;
            notifications.show("Clicked! Count: " + clickCount, 1500);
        }));
        viewport = new ViewportNode(
            VBox.of(IntStream.range(1, 21)
                .mapToObj(i -> Text.of("  Line " + i + ":  " + "=".repeat(i) + " " + i + "%")
                    .color(i % 2 == 0 ? Color.BRIGHT_BLACK : Color.WHITE))
                .toArray(Node[]::new)),
            5
        );
    }

    @Override
    public void mount(Runnable onStateChange, EventBus eventBus) {
        super.mount(onStateChange, eventBus);

        // Focus
        registerFocusable(clickBtn);

        // Tab switching
        eventBus.registerCharacter(c -> {
            if (c >= '1' && c <= '5') setState(() -> activeTab = c - '1');
        });

        // Theme toggle
        eventBus.registerCharacter(c -> {
            if (c == 't' || c == 'T') setState(() -> {
                boolean isDark = AliveJTUI.getTheme() == Theme.DARK;
                AliveJTUI.setTheme(isDark ? Theme.LIGHT : Theme.DARK);
            });
        });

        // Dialog open
        eventBus.registerCharacter(c -> {
            if ((c == 'd' || c == 'D') && dialogNode == null) {
                setState(() -> dialogNode = buildConfirmDialog());
            }
        });

        // Notification
        eventBus.registerCharacter(c -> {
            if (c == 'n' || c == 'N') {
                notifications.show("Hello from AliveJTUI! " +
                        java.time.LocalTime.now().toString().substring(0, 8),
                        3000, NotificationType.INFO);
            }
        });

        // Progress
        eventBus.registerCharacter(c -> {
            if (c == '+') setState(() -> progress = Math.min(1.0, progress + 0.05));
            if (c == '-') setState(() -> progress = Math.max(0.0, progress - 0.05));
        });

        // Input (tab 1 only, ignore reserved keys)
        eventBus.registerCharacter(c -> {
            if (activeTab == 0 && c >= 32
                    && c != '+' && c != '-'
                    && c != 't' && c != 'T'
                    && c != 'd' && c != 'D'
                    && c != 'n' && c != 'N'
                    && c != 'c' && c != 'C'
                    && c != 'x' && c != 'X'
                    && c != 's' && c != 'S') {
                setState(() -> inputText += c);
            }
        });
        onKey(KeyType.BACKSPACE, () -> {
            if (activeTab == 0 && !inputText.isEmpty())
                setState(() -> inputText = inputText.substring(0, inputText.length() - 1));
        });

        // Navigation
        onKey(KeyType.ARROW_DOWN, () -> {
            if (activeTab == 1) setState(() -> tableRow = Math.min(TABLE_DATA.size() - 1, tableRow + 1));
            if (activeTab == 2) setState(() -> vList.selectDown());
            if (activeTab == 4) setState(() -> viewport.scrollDown());
            if (activeTab == 0) setState(() -> {
                radioIdx   = (radioIdx + 1) % 2;
                spinFrame  = (spinFrame + 1) % SPIN.length;
            });
        });
        onKey(KeyType.ARROW_UP, () -> {
            if (activeTab == 1) setState(() -> tableRow = Math.max(0, tableRow - 1));
            if (activeTab == 2) setState(() -> vList.selectUp());
            if (activeTab == 4) setState(() -> viewport.scrollUp());
            if (activeTab == 0) setState(() -> {
                radioIdx  = (radioIdx + 1) % 2;
                spinFrame = (spinFrame + 1) % SPIN.length;
            });
        });
        onKey(KeyType.PAGE_DOWN, () -> {
            if (activeTab == 2) setState(() -> vList.pageDown());
            if (activeTab == 4) setState(() -> viewport.pageDown());
        });
        onKey(KeyType.PAGE_UP, () -> {
            if (activeTab == 2) setState(() -> vList.pageUp());
            if (activeTab == 4) setState(() -> viewport.pageUp());
        });
        onKey(KeyType.HOME,      () -> { if (activeTab == 2) setState(() -> vList.selectFirst()); });
        onKey(KeyType.END,       () -> { if (activeTab == 2) setState(() -> vList.selectLast()); });

        // Collapsible
        eventBus.registerCharacter(c -> {
            if (c == 'c' || c == 'C') setState(() -> colExpanded = !colExpanded);
        });

        // Checkbox
        eventBus.registerCharacter(c -> {
            if (c == 'x' || c == 'X') setState(() -> cbChecked = !cbChecked);
        });

        // Select cycle
        eventBus.registerCharacter(c -> {
            if (c == 's' || c == 'S') setState(() -> selectIdx = (selectIdx + 1) % COLORS_OPT.length);
        });

        // Spinner auto-tick
        AliveJTUI.scheduleRepeating(150, () -> setState(() -> spinFrame = (spinFrame + 1) % SPIN.length));
    }

    // ==========================================================================
    //  RENDER
    // ==========================================================================

    @Override
    public Node render() {
        // ---- Overlay management ----
        Node notifOverlay = notifications.buildOverlay();

        if (dialogNode != null) {
            // Dialog takes priority
            AliveJTUI.pushOverlay(dialogNode);
            notifShowing = false;
        } else if (notifOverlay != null) {
            AliveJTUI.pushOverlay(notifOverlay);
            notifShowing = true;
        } else if (notifShowing) {
            // Notification just expired — clear overlay
            AliveJTUI.popOverlay();
            notifShowing = false;
        }

        return VBox.of(
            renderHeader(),
            renderTabBar(),
            Divider.horizontal(),
            renderTabContent(),
            Divider.horizontal(),
            renderFooter()
        );
    }

    // --- Header ---
    private Node renderHeader() {
        boolean dark = AliveJTUI.getTheme() == Theme.DARK;
        String themeLabel = dark ? "[Dark]" : "[Light]";
        Color titleColor = dark ? Color.CYAN : Color.BLUE;
        return HBox.of(
            Text.of("  AliveJTUI Demo v0.1.0").bold().color(titleColor),
            Text.of("  theme: " + themeLabel).dim()
        );
    }

    // --- Tab bar ---
    private Node renderTabBar() {
        boolean dark = AliveJTUI.getTheme() == Theme.DARK;
        Color activeColor = dark ? Color.BRIGHT_CYAN : Color.BLUE;
        Node[] tabs = new Node[TAB_NAMES.length];
        for (int i = 0; i < TAB_NAMES.length; i++) {
            boolean active = i == activeTab;
            TextNode t = Text.of(" " + TAB_NAMES[i] + " ")
                    .color(active ? activeColor : Color.BRIGHT_BLACK);
            if (active) t = t.bold();
            tabs[i] = t;
        }
        return HBox.of(tabs);
    }

    // --- Tab content ---
    private Node renderTabContent() {
        return switch (activeTab) {
            case 0 -> renderWidgetsTab();
            case 1 -> renderTableTab();
            case 2 -> renderVirtualListTab();
            case 3 -> renderTextTab();
            case 4 -> renderLayoutTab();
            default -> Text.of("Unknown tab");
        };
    }

    // ==========================================================================
    //  TAB 1 — Widgets
    // ==========================================================================
    private Node renderWidgetsTab() {
        int pct = (int) Math.round(progress * 100);
        Color pctColor = pct >= 80 ? Color.GREEN : pct <= 20 ? Color.RED : Color.YELLOW;

        return VBox.of(
            Text.of(""),
            HBox.of(
                Text.of("  "),
                clickBtn,
                Text.of("  Clicked: ").dim(),
                Text.of(String.valueOf(clickCount)).bold().color(Color.GREEN),
                Text.of("  Spin: " + SPIN[spinFrame]).color(Color.CYAN)
            ),
            Text.of(""),
            HBox.of(
                Text.of("  Progress "),
                Text.of("[+][-]").dim()
            ),
            HBox.of(
                Text.of("  "),
                new ProgressBarNode(progress),
                Text.of("  " + pct + "%").bold().color(pctColor)
            ),
            Text.of(""),
            HBox.of(
                Text.of("  "),
                Checkbox.of("Notifications enabled [X]", cbChecked, () -> {}),
                Text.of("    Input: ").dim(),
                Text.of("[" + inputText + "_]").color(Color.YELLOW)
            ),
            Text.of(""),
            HBox.of(
                Text.of("  Theme radio [Up/Down]:  "),
                Text.of(radioIdx == 0 ? "(x) Dark  ( ) Light" : "( ) Dark  (x) Light")
                    .color(Color.BRIGHT_CYAN)
            ),
            HBox.of(
                Text.of("  Color select [S]:  "),
                Text.of("<< " + COLORS_OPT[selectIdx] + " >>").bold()
                    .color(nameToColor(COLORS_OPT[selectIdx]))
            ),
            Text.of("")
        );
    }

    private Color nameToColor(String name) {
        return switch (name) {
            case "Red"     -> Color.RED;
            case "Green"   -> Color.GREEN;
            case "Blue"    -> Color.BLUE;
            case "Cyan"    -> Color.CYAN;
            case "Magenta" -> Color.MAGENTA;
            default        -> Color.WHITE;
        };
    }

    // ==========================================================================
    //  TAB 2 — Table
    // ==========================================================================
    private Node renderTableTab() {
        TableNode table = Table.of(
                List.of("Name", "Dept", "Level", "City"),
                TABLE_DATA,
                8
        );
        for (int i = 0; i < tableRow; i++) table.selectDown();

        return VBox.of(
            Text.of(""),
            Text.of("  Up/Down: Navigate  |  8 employees").dim(),
            Text.of(""),
            table,
            Text.of(""),
            HBox.of(
                Text.of("  Selected: ").dim(),
                Text.of(TABLE_DATA.get(tableRow).get(0) + "  -  "
                        + TABLE_DATA.get(tableRow).get(1)).bold().color(Color.BRIGHT_CYAN)
            )
        );
    }

    // ==========================================================================
    //  TAB 3 — VirtualList
    // ==========================================================================
    private Node renderVirtualListTab() {
        return VBox.of(
            Text.of(""),
            HBox.of(
                Text.of("  10,000 items — only visible rows rendered  ").dim(),
                Text.of("Up/Down  PgUp/PgDn  Home/End").color(Color.BRIGHT_BLACK)
            ),
            Text.of(""),
            vList,
            Text.of(""),
            HBox.of(
                Text.of("  Item ").dim(),
                Text.of(String.valueOf(vList.getSelectedIndex() + 1)).bold().color(Color.CYAN),
                Text.of(" / " + vList.itemCount()).dim()
            )
        );
    }

    // ==========================================================================
    //  TAB 4 — Text
    // ==========================================================================
    private Node renderTextTab() {
        return VBox.of(
            Text.of(""),
            Text.of("  STYLED TEXT").bold().color(Color.YELLOW),
            HBox.of(
                Text.of("  "),
                Text.of("Bold ").bold(),
                Text.of("Italic ").italic(),
                Text.of("Underline ").underline(),
                Text.of("Strike ").strikethrough(),
                Text.of("Dim").dim()
            ),
            HBox.of(
                Text.of("  "),
                Text.of("Red ").color(Color.RED),
                Text.of("Green ").color(Color.GREEN),
                Text.of("Yellow ").color(Color.YELLOW),
                Text.of("Blue ").color(Color.BLUE),
                Text.of("Cyan ").color(Color.CYAN),
                Text.of("Magenta ").color(Color.MAGENTA),
                Text.of("BrightGreen ").color(Color.BRIGHT_GREEN)
            ),
            Text.of(""),
            Divider.horizontal(),
            Text.of(""),
            Text.of("  MARKDOWN").bold().color(Color.YELLOW),
            Text.of("  "),
            Paragraph.ofMarkdown(
                "**AliveJTUI** is a *declarative* TUI library for Java. " +
                "Build terminal UIs as component trees -- just like React, " +
                "but for the terminal. Supports **bold**, *italic*, and `code`."
            ),
            Text.of(""),
            Divider.horizontal(),
            Text.of(""),
            Text.of("  WORD-WRAPPED").bold().color(Color.YELLOW),
            Text.of("  "),
            new TextNode(
                "Word wrapping automatically breaks long lines to fit the terminal width. " +
                "This is useful for descriptions, help text, and any prose content " +
                "that should reflow when the window is resized.",
                Style.DEFAULT.withForeground(Color.BRIGHT_BLACK)
            ).wrap()
        );
    }

    // ==========================================================================
    //  TAB 5 — Layout
    // ==========================================================================
    private Node renderLayoutTab() {
        return VBox.of(
            Text.of(""),
            Text.of("  BOX LAYOUT").bold().color(Color.YELLOW),
            HBox.of(
                new BoxNode(VBox.of(
                    Text.of(" Panel A ").bold().color(Color.BLUE),
                    Text.of(" alpha   ").dim(),
                    Text.of(" beta    ").dim()
                ), true, Style.DEFAULT.withForeground(Color.BLUE)),
                Text.of(" "),
                new BoxNode(VBox.of(
                    Text.of(" Panel B ").bold().color(Color.CYAN),
                    Text.of(" gamma   ").dim(),
                    Text.of(" delta   ").dim()
                ), true, Style.DEFAULT.withForeground(Color.CYAN)),
                Text.of(" "),
                new BoxNode(VBox.of(
                    Text.of(" Panel C ").bold().color(Color.MAGENTA),
                    Text.of(" epsilon ").dim(),
                    Text.of(" zeta    ").dim()
                ), true, Style.DEFAULT.withForeground(Color.MAGENTA))
            ),
            Text.of(""),
            Divider.horizontal(),
            Text.of(""),
            HBox.of(
                Text.of("  COLLAPSIBLE [C] ").bold().color(Color.YELLOW),
                Text.of(colExpanded ? "v expanded" : "> collapsed").color(Color.BRIGHT_BLACK)
            ),
            colExpanded
                ? VBox.of(
                    Text.of("  * Feature flags configuration"),
                    Text.of("  * Database connection pool settings"),
                    Text.of("  * Logging level overrides"),
                    Text.of("  * Performance tuning knobs")
                  )
                : Text.of("  (hidden -- press C to expand)").dim(),
            Text.of(""),
            Divider.horizontal(),
            Text.of(""),
            Text.of("  VIEWPORT (scrollable) — Up/Down PgUp/PgDn").bold().color(Color.YELLOW),
            viewport
        );
    }

    // --- Confirm dialog (built inline so callbacks can capture `this`) ---
    private Node buildConfirmDialog() {
        return Dialog.of("Confirm Action",
            VBox.of(
                Paragraph.ofMarkdown("Are you sure you want to proceed?"),
                Text.of(""),
                HBox.of(
                    Button.of("[Yes]", () -> setState(() -> {
                        dialogNode = null;
                        notifications.show("Action confirmed!", 2500, NotificationType.SUCCESS);
                    })),
                    Text.of("   "),
                    Button.of("[No]", () -> setState(() -> dialogNode = null))
                )
            )
        );
    }

    // --- Footer ---
    private Node renderFooter() {
        return HBox.of(
            Text.of("  1-5:Tab ").dim(),
            Text.of("T:Theme ").dim(),
            Text.of("D:Dialog ").dim(),
            Text.of("N:Notify ").dim(),
            Text.of("C:Collapse ").dim(),
            Text.of("X:Checkbox ").dim(),
            Text.of("S:Select ").dim(),
            Text.of("+/-:Progress ").dim(),
            Text.of("ESC:Quit").dim()
        );
    }

    // ==========================================================================
    //  Entry point
    // ==========================================================================
    public static void main(String[] args) {
        AliveJTUI.run(new DemoApp());
    }
}
