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
 *   1-6      Switch tab
 *   Tab      Move focus (tab 6: Login form)
 *   T        Toggle Dark/Light theme
 *   D        Show dialog
 *   N        Show notification
 *   Up/Down  Navigate lists / tables
 *   +/-      Progress bar
 *   ESC      Quit
 * </pre>
 *
 * @author Jarvis (AI)
 */
public class DemoApp extends Component {

    // --- Tabs ---
    private static final String[] TAB_NAMES = {
        "1:Widgets", "2:Table", "3:VirtualList", "4:Text", "5:Layout", "6:Login"
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

    // --- Tab 6: Login form ---
    private String loginStatus = "";
    private final InputNode    loginUserInput  = Input.of("", null);
    private final InputNode    loginPassInput  = Input.of("", null);
    private final CheckboxNode loginRememberCb = Checkbox.of("Remember me", false, null);
    private final ButtonNode   loginLoginBtn   = Button.of("[ Login ]",  this::doLogin);
    private final ButtonNode   loginCancelBtn  = Button.of("[ Cancel ]", this::doCancel);

    // --- Notifications ---
    private final NotificationManager notifications;

    // --- Overlay management ---
    private Node    dialogNode   = null;
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

    /** Shorthand for the active theme. */
    private Theme t() { return AliveJTUI.getTheme(); }

    /** Creates a TextNode with the given full Style from the theme. */
    private TextNode styled(String text, Style style) {
        return new TextNode(text, style);
    }

    @Override
    public void mount(Runnable onStateChange, EventBus eventBus) {
        super.mount(onStateChange, eventBus);

        // Focus — tab 1
        registerFocusable(clickBtn);

        // Focus — tab 6: Login
        loginUserInput.setKey("loginUser");
        loginPassInput.setKey("loginPass");
        registerFocusable(loginUserInput);
        registerFocusable(loginPassInput);
        registerFocusable(loginRememberCb);
        registerFocusable(loginLoginBtn);
        registerFocusable(loginCancelBtn);

        // Tab switching 1-6
        eventBus.registerCharacter(c -> {
            if (c >= '1' && c <= '6') setState(() -> activeTab = c - '1');
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

        // Input — tab 1 only
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

        // Input — tab 6: Login form
        eventBus.registerCharacter(c -> {
            if (activeTab != 5) return;
            if (c == ' ' && loginRememberCb.isFocused()) {
                setState(() -> loginRememberCb.toggle());
                return;
            }
            if (c < 32) return;
            // Let global shortcuts (T/D/N) still fire; don't also type them into inputs
            if (c == 't' || c == 'T' || c == 'd' || c == 'D' || c == 'n' || c == 'N') return;
            if (loginUserInput.isFocused())
                setState(() -> loginUserInput.setValue(loginUserInput.getValue() + c));
            if (loginPassInput.isFocused())
                setState(() -> loginPassInput.setValue(loginPassInput.getValue() + c));
        });
        onKey(KeyType.BACKSPACE, () -> {
            if (activeTab != 5) return;
            if (loginUserInput.isFocused()) {
                String v = loginUserInput.getValue();
                if (!v.isEmpty()) setState(() -> loginUserInput.setValue(v.substring(0, v.length() - 1)));
            }
            if (loginPassInput.isFocused()) {
                String v = loginPassInput.getValue();
                if (!v.isEmpty()) setState(() -> loginPassInput.setValue(v.substring(0, v.length() - 1)));
            }
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
        Node notifOverlay = notifications.buildOverlay();

        if (dialogNode != null) {
            AliveJTUI.pushOverlay(dialogNode);
            notifShowing = false;
        } else if (notifOverlay != null) {
            AliveJTUI.pushOverlay(notifOverlay);
            notifShowing = true;
        } else if (notifShowing) {
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
        String themeLabel = t() == Theme.DARK ? "[Dark]" : "[Light]";
        return HBox.of(
            styled("  AliveJTUI Demo v0.1.0", t().primary()),
            styled("  theme: " + themeLabel, t().muted())
        );
    }

    // --- Tab bar ---
    private Node renderTabBar() {
        Node[] tabs = new Node[TAB_NAMES.length];
        for (int i = 0; i < TAB_NAMES.length; i++) {
            tabs[i] = styled(" " + TAB_NAMES[i] + " ", i == activeTab ? t().primary() : t().muted());
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
            case 5 -> renderLoginTab();
            default -> Text.of("Unknown tab");
        };
    }

    // ==========================================================================
    //  TAB 1 — Widgets
    // ==========================================================================
    private Node renderWidgetsTab() {
        int pct = (int) Math.round(progress * 100);
        Style pctStyle = pct >= 80 ? t().success() : pct <= 20 ? t().error() : t().warning();

        return VBox.of(
            Text.of(""),
            HBox.of(
                Text.of("  "),
                clickBtn,
                styled("  Clicked: ", t().muted()),
                styled(String.valueOf(clickCount), t().success()),
                styled("  Spin: " + SPIN[spinFrame], t().secondary())
            ),
            Text.of(""),
            HBox.of(
                Text.of("  Progress "),
                styled("[+][-]", t().muted())
            ),
            HBox.of(
                Text.of("  "),
                new ProgressBarNode(progress),
                styled("  " + pct + "%", pctStyle)
            ),
            Text.of(""),
            HBox.of(
                Text.of("  "),
                Checkbox.of("Notifications enabled [X]", cbChecked, () -> {}),
                styled("    Input: ", t().muted()),
                styled("[" + inputText + "_]", t().primary())
            ),
            Text.of(""),
            HBox.of(
                Text.of("  Theme radio [Up/Down]:  "),
                styled(radioIdx == 0 ? "(x) Dark  ( ) Light" : "( ) Dark  (x) Light", t().primary())
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
            styled("  Up/Down: Navigate  |  8 employees", t().muted()),
            Text.of(""),
            table,
            Text.of(""),
            HBox.of(
                styled("  Selected: ", t().muted()),
                styled(TABLE_DATA.get(tableRow).get(0) + "  -  "
                        + TABLE_DATA.get(tableRow).get(1), t().primary())
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
                styled("  10,000 items — only visible rows rendered  ", t().muted()),
                styled("Up/Down  PgUp/PgDn  Home/End", t().muted())
            ),
            Text.of(""),
            vList,
            Text.of(""),
            HBox.of(
                styled("  Item ", t().muted()),
                styled(String.valueOf(vList.getSelectedIndex() + 1), t().primary()),
                styled(" / " + vList.itemCount(), t().muted())
            )
        );
    }

    // ==========================================================================
    //  TAB 4 — Text
    // ==========================================================================
    private Node renderTextTab() {
        return VBox.of(
            Text.of(""),
            styled("  STYLED TEXT", t().primary()),
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
            styled("  MARKDOWN", t().primary()),
            Text.of("  "),
            Paragraph.ofMarkdown(
                "**AliveJTUI** is a *declarative* TUI library for Java. " +
                "Build terminal UIs as component trees -- just like React, " +
                "but for the terminal. Supports **bold**, *italic*, and `code`."
            ),
            Text.of(""),
            Divider.horizontal(),
            Text.of(""),
            styled("  WORD-WRAPPED", t().primary()),
            Text.of("  "),
            new TextNode(
                "Word wrapping automatically breaks long lines to fit the terminal width. " +
                "This is useful for descriptions, help text, and any prose content " +
                "that should reflow when the window is resized.",
                t().muted()
            ).wrap()
        );
    }

    // ==========================================================================
    //  TAB 5 — Layout
    // ==========================================================================
    private Node renderLayoutTab() {
        return VBox.of(
            Text.of(""),
            styled("  BOX LAYOUT", t().primary()),
            HBox.of(
                new BoxNode(VBox.of(
                    styled(" Panel A ", t().secondary().withBold(true)),
                    styled(" alpha   ", t().muted()),
                    styled(" beta    ", t().muted())
                ), true, t().secondary()),
                Text.of(" "),
                new BoxNode(VBox.of(
                    styled(" Panel B ", t().primary()),
                    styled(" gamma   ", t().muted()),
                    styled(" delta   ", t().muted())
                ), true, t().primary()),
                Text.of(" "),
                new BoxNode(VBox.of(
                    styled(" Panel C ", t().warning().withBold(true)),
                    styled(" epsilon ", t().muted()),
                    styled(" zeta    ", t().muted())
                ), true, t().warning())
            ),
            Text.of(""),
            Divider.horizontal(),
            Text.of(""),
            HBox.of(
                styled("  COLLAPSIBLE [C] ", t().primary()),
                styled(colExpanded ? "v expanded" : "> collapsed", t().muted())
            ),
            colExpanded
                ? VBox.of(
                    Text.of("  * Feature flags configuration"),
                    Text.of("  * Database connection pool settings"),
                    Text.of("  * Logging level overrides"),
                    Text.of("  * Performance tuning knobs")
                  )
                : styled("  (hidden -- press C to expand)", t().muted()),
            Text.of(""),
            Divider.horizontal(),
            Text.of(""),
            styled("  VIEWPORT (scrollable) — Up/Down PgUp/PgDn", t().primary()),
            viewport
        );
    }

    // ==========================================================================
    //  TAB 6 — Login
    // ==========================================================================
    private Node renderLoginTab() {
        Style statusStyle = loginStatus.startsWith("Logging") ? t().success()
                          : loginStatus.isEmpty()             ? t().muted()
                                                              : t().error();
        return VBox.of(
            Text.of(""),
            styled("  Login Form", t().primary()),
            styled("  Use Tab / Shift+Tab to move focus, Enter to activate buttons.", t().muted()),
            Divider.horizontal(),
            Text.of(""),
            HBox.of(Text.of("  Username : "), loginUserInput),
            HBox.of(Text.of("  Password : "), loginPassInput),
            HBox.of(Text.of("  "), loginRememberCb),
            Text.of(""),
            HBox.of(Text.of("  "), loginLoginBtn, Text.of("  "), loginCancelBtn),
            Text.of(""),
            styled("  " + loginStatus, statusStyle)
        );
    }

    private void doLogin() {
        if (loginUserInput.getValue().isEmpty()) {
            setState(() -> loginStatus = "Username is required.");
            getFocusManager().focusById("loginUser");
            return;
        }
        if (loginPassInput.getValue().isEmpty()) {
            setState(() -> loginStatus = "Password is required.");
            getFocusManager().focusById("loginPass");
            return;
        }
        setState(() -> loginStatus = "Logging in as " + loginUserInput.getValue() + " \u2026");
    }

    private void doCancel() {
        setState(() -> {
            loginUserInput.setValue("");
            loginPassInput.setValue("");
            if (loginRememberCb.isChecked()) loginRememberCb.toggle();
            loginStatus = "";
        });
    }

    // --- Confirm dialog ---
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
        Style s = t().muted();
        return HBox.of(
            styled("  1-6:Tab ", s),
            styled("Tab:Focus ", s),
            styled("T:Theme ", s),
            styled("D:Dialog ", s),
            styled("N:Notify ", s),
            styled("C:Collapse ", s),
            styled("X:Checkbox ", s),
            styled("S:Select ", s),
            styled("+/-:Progress ", s),
            styled("ESC:Quit", s)
        );
    }

    // ==========================================================================
    //  Entry point
    // ==========================================================================
    public static void main(String[] args) {
        AliveJTUI.run(new DemoApp());
    }
}
