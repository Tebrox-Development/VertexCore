package de.tebrox.vertexCore.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public final class AsyncQueue {
    private final QueueState state;
    private final long timeoutMillis;

    public AsyncQueue(Executor executor, long timeoutMillis) {
        this(new QueueState(executor), timeoutMillis);
    }

    private AsyncQueue(QueueState state, long timeoutMillis) {
        this.state = state;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Returns a timeout-specific caller view that shares this queue's ordering state.
     * Tasks submitted through either instance remain in one ordered lane while each
     * caller view applies its own timeout independently.
     */
    public AsyncQueue withTimeout(long timeoutMillis) {
        if (this.timeoutMillis == timeoutMillis) {
            return this;
        }
        return new AsyncQueue(state, timeoutMillis);
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
    public <T> CompletableFuture<T> submitRaw(Supplier<T> task) {
        CompletableFuture<T> raw;
        synchronized (state) {
            raw = state.tail.thenApplyAsync(v -> task.get(), state.executor);
            state.tail = raw.handle((result, error) -> null);
        }
        return raw;
    }

    public CompletableFuture<Void> submitVoidRaw(Runnable task) {
        CompletableFuture<Void> raw;
        synchronized (state) {
            raw = state.tail.thenRunAsync(task, state.executor);
            state.tail = raw.handle((result, error) -> null);
        }
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

    private static final class QueueState {
        private final Executor executor;
        private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);

        private QueueState(Executor executor) {
            this.executor = executor;
        }
    }
}
