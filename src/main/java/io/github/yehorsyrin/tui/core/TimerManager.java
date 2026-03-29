package io.github.yehorsyrin.tui.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages one-shot and repeating timers polled by the event loop.
 *
 * <p>All methods must be called from the event loop thread. Timers are fired inside
 * {@link #tick(Runnable)}, which is called once per iteration of the event loop.
 *
 * <pre>{@code
 * AliveJTUI.schedule(500, () -> setState(s -> s.withTick(s.tick() + 1)));
 * AliveJTUI.scheduleRepeating(100, () -> spinnerState.advance());
 * }</pre>
 *
 * @author Jarvis (AI)
 */
public class TimerManager {

    private record Entry(long fireAtNanos, long intervalNanos, Runnable task) {
        boolean repeating() { return intervalNanos > 0; }
    }

    private final List<Entry> pending = new ArrayList<>();

    /**
     * Schedules a one-shot timer.
     *
     * @param delayMs delay in milliseconds before the task fires
     * @param task    the callback to run
     */
    public void schedule(long delayMs, Runnable task) {
        if (task == null) return;
        long fireAt = System.nanoTime() + delayMs * 1_000_000L;
        pending.add(new Entry(fireAt, 0, task));
    }

    /**
     * Schedules a repeating timer that fires every {@code intervalMs} milliseconds
     * after an initial delay of the same duration.
     *
     * @param intervalMs interval in milliseconds between firings
     * @param task       the callback to run on each interval
     */
    public void scheduleRepeating(long intervalMs, Runnable task) {
        if (task == null || intervalMs <= 0) return;
        long intervalNs = intervalMs * 1_000_000L;
        long fireAt = System.nanoTime() + intervalNs;
        pending.add(new Entry(fireAt, intervalNs, task));
    }

    /**
     * Fires all timers whose fire time has arrived.
     * Repeating timers are rescheduled after firing.
     * Calls {@code onFire} once after all due timers have fired (if any fired).
     *
     * @param onFire called when at least one timer fired (typically triggers a re-render)
     * @return {@code true} if at least one timer fired
     */
    public boolean tick(Runnable onFire) {
        if (pending.isEmpty()) return false;
        long now = System.nanoTime();
        boolean fired = false;
        List<Entry> toReschedule = new ArrayList<>();

        Iterator<Entry> it = pending.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.fireAtNanos() <= now) {
                it.remove();
                e.task().run();
                if (e.repeating()) {
                    toReschedule.add(new Entry(now + e.intervalNanos(), e.intervalNanos(), e.task()));
                }
                fired = true;
            }
        }

        pending.addAll(toReschedule);
        if (fired && onFire != null) onFire.run();
        return fired;
    }

    /** Returns {@code true} if any timers are registered. */
    public boolean hasPendingTimers() {
        return !pending.isEmpty();
    }

    /**
     * Returns the number of milliseconds until the next timer fires,
     * or {@link Long#MAX_VALUE} if no timers are registered.
     */
    public long msUntilNext() {
        if (pending.isEmpty()) return Long.MAX_VALUE;
        long now = System.nanoTime();
        long minNs = Long.MAX_VALUE;
        for (Entry e : pending) {
            minNs = Math.min(minNs, e.fireAtNanos() - now);
        }
        return Math.max(0L, minNs / 1_000_000L);
    }

    /**
     * Cancels all timers whose task is the given runnable (by reference equality).
     *
     * @param task the task to cancel
     */
    public void cancel(Runnable task) {
        pending.removeIf(e -> e.task() == task);
    }

    /** Cancels all registered timers. */
    public void clear() {
        pending.clear();
    }

    /** Returns the number of currently registered timers. */
    public int size() {
        return pending.size();
    }
}
