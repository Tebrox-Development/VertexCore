package de.tebrox.vertexCore.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public final class AsyncQueue {
    private final Executor executor;
    private final long timeoutMillis;

    /**
     * Always tracks the actual end of the queued task. Caller timeouts must
     * never advance this tail while the underlying task is still running.
     */
    private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);

    public AsyncQueue(Executor executor, long timeoutMillis) {
        this.executor = executor;
        this.timeoutMillis = timeoutMillis;
    }

    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        return timeoutView(submitRaw(task));
    }

    public CompletableFuture<Void> submitVoid(Runnable task) {
        return timeoutView(submitVoidRaw(task));
    }

    /**
     * Submit without a caller timeout. The returned future represents the
     * actual task settlement and is also what the queue uses for ordering.
     */
    public synchronized <T> CompletableFuture<T> submitRaw(Supplier<T> task) {
        CompletableFuture<T> raw = tail.thenApplyAsync(v -> task.get(), executor);
        tail = raw.handle((result, error) -> null);
        return raw;
    }

    public synchronized CompletableFuture<Void> submitVoidRaw(Runnable task) {
        CompletableFuture<Void> raw = tail.thenRunAsync(task, executor);
        tail = raw.handle((result, error) -> null);
        return raw;
    }

    private <T> CompletableFuture<T> timeoutView(CompletableFuture<T> raw) {
        if (timeoutMillis <= 0) {
            return raw;
        }

        CompletableFuture<T> view = new CompletableFuture<>();
        raw.whenComplete((result, error) -> {
            if (error != null) {
                view.completeExceptionally(error);
            } else {
                view.complete(result);
            }
        });

        CompletableFuture.delayedExecutor(timeoutMillis, TimeUnit.MILLISECONDS).execute(() ->
                view.completeExceptionally(new TimeoutException(
                        "AsyncQueue operation timed out after " + timeoutMillis + " ms"
                ))
        );

        return view;
    }
}
