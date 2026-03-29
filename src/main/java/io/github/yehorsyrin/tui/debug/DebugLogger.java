package io.github.yehorsyrin.tui.debug;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Optional debug logger for AliveJTUI.
 *
 * <p>When enabled via {@link #enable(String)}, all logged events are appended to the given file.
 * Disable with {@link #disable()} to close the writer and stop logging.
 *
 * <pre>{@code
 * DebugLogger.enable("tui-debug.log");
 * // ...run application...
 * DebugLogger.disable();
 * }</pre>
 *
 * <p>All methods are thread-safe.
 *
 * @author Jarvis (AI)
 */
public final class DebugLogger implements Closeable {

    private static final AtomicReference<DebugLogger> INSTANCE = new AtomicReference<>();

    private final PrintWriter writer;
    private long renderCount  = 0;
    private long eventCount   = 0;

    private DebugLogger(String filePath) throws IOException {
        this.writer = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)));
        log("DebugLogger started");
    }

    // --- Static API ---

    /**
     * Enables debug logging to the given file. If logging is already enabled,
     * the old logger is closed first.
     *
     * @param filePath path to the log file (created or appended)
     * @throws IllegalArgumentException if {@code filePath} is null or blank
     */
    public static synchronized void enable(String filePath) {
        if (filePath == null || filePath.isBlank())
            throw new IllegalArgumentException("filePath must not be blank");
        disable();  // close any previous logger
        try {
            INSTANCE.set(new DebugLogger(filePath));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot open debug log: " + filePath, e);
        }
    }

    /**
     * Disables debug logging and closes the log file.
     * No-op if logging is already disabled.
     */
    public static synchronized void disable() {
        DebugLogger current = INSTANCE.getAndSet(null);
        if (current != null) {
            current.log("DebugLogger stopped");
            current.writer.flush();
            current.writer.close();
        }
    }

    /** Returns {@code true} if debug logging is currently enabled. */
    public static boolean isEnabled() {
        return INSTANCE.get() != null;
    }

    /**
     * Logs a render event with the number of changed cells.
     *
     * @param changedCells number of cells that were redrawn
     * @param nodeCount    total number of nodes in the tree
     */
    public static void logRender(int changedCells, int nodeCount) {
        DebugLogger l = INSTANCE.get();
        if (l == null) return;
        synchronized (l) {
            l.renderCount++;
            l.log(String.format("[RENDER #%d] changed=%d nodes=%d", l.renderCount, changedCells, nodeCount));
        }
    }

    /**
     * Logs an input event (key or mouse).
     *
     * @param description human-readable event description
     */
    public static void logEvent(String description) {
        DebugLogger l = INSTANCE.get();
        if (l == null) return;
        synchronized (l) {
            l.eventCount++;
            l.log(String.format("[EVENT  #%d] %s", l.eventCount, description));
        }
    }

    /**
     * Logs a free-form message.
     *
     * @param message the message to log
     */
    public static void logMessage(String message) {
        DebugLogger l = INSTANCE.get();
        if (l == null) return;
        synchronized (l) {
            l.log(message);
        }
    }

    /**
     * Logs an exception with its stack trace.
     *
     * @param label short description of where the exception occurred
     * @param ex    the exception
     */
    public static void logError(String label, Throwable ex) {
        DebugLogger l = INSTANCE.get();
        if (l == null) return;
        synchronized (l) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            l.log(String.format("[ERROR ] %s: %s", label, sw));
        }
    }

    // --- Internal ---

    private void log(String msg) {
        writer.printf("[%s] %s%n", Instant.now(), msg);
        writer.flush();
    }

    @Override
    public void close() {
        disable();
    }
}
