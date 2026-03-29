package io.github.yehorsyrin.tui.core;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * A background task whose result is delivered to a callback on the event loop thread.
 *
 * <pre>{@code
 * AliveJTUI.runAsync(AsyncTask.of(
 *     ()     -> fetchData(),          // runs in background thread
 *     result -> setState(result),     // called on event loop thread
 *     ex     -> showError(ex)         // called on error (event loop thread)
 * ));
 * }</pre>
 *
 * <p>Use {@link AliveJTUI#runAsync(AsyncTask)} to submit.
 *
 * @param <T> type of the result produced by the background task
 *
 * @author Jarvis (AI)
 */
public final class AsyncTask<T> {

    private final Callable<T>         work;
    private final Consumer<T>         onSuccess;
    private final Consumer<Exception> onError;

    private AsyncTask(Callable<T> work, Consumer<T> onSuccess, Consumer<Exception> onError) {
        if (work      == null) throw new IllegalArgumentException("work must not be null");
        if (onSuccess == null) throw new IllegalArgumentException("onSuccess must not be null");
        this.work      = work;
        this.onSuccess = onSuccess;
        this.onError   = onError; // nullable — errors silently ignored when null
    }

    /**
     * Creates a task with a success callback; errors are silently ignored.
     *
     * @param work      background computation
     * @param onSuccess callback invoked on the event loop thread with the result
     */
    public static <T> AsyncTask<T> of(Callable<T> work, Consumer<T> onSuccess) {
        return new AsyncTask<>(work, onSuccess, null);
    }

    /**
     * Creates a task with both a success and an error callback.
     *
     * @param work      background computation
     * @param onSuccess callback invoked on the event loop thread with the result
     * @param onError   callback invoked on the event loop thread if the task throws
     */
    public static <T> AsyncTask<T> of(Callable<T> work, Consumer<T> onSuccess,
                                      Consumer<Exception> onError) {
        return new AsyncTask<>(work, onSuccess, onError);
    }

    /**
     * Convenience method equivalent to {@link AliveJTUI#runAsync(AsyncTask)}.
     * Submits this task to the background executor.
     */
    public void run() {
        AliveJTUI.runAsync(this);
    }

    // --- Package-private accessors for AliveJTUI ---

    Callable<T>         getWork()      { return work;      }
    Consumer<T>         getOnSuccess() { return onSuccess; }
    Consumer<Exception> getOnError()   { return onError;   }
}
