package de.tebrox.vertexCore.database;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DatabaseWriteOperation {

    private final UUID operationId;
    private final CompletableFuture<DatabaseWriteResult> result;
    private final CompletableFuture<DatabaseWriteResult> completion;

    DatabaseWriteOperation(
            UUID operationId,
            CompletableFuture<DatabaseWriteResult> result,
            CompletableFuture<DatabaseWriteResult> completion
    ) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.result = Objects.requireNonNull(result, "result");
        this.completion = Objects.requireNonNull(completion, "completion");
    }

    public UUID operationId() {
        return operationId;
    }

    /**
     * Caller-facing result. A configured deadline may complete this view with
     * {@link DatabaseWriteResult.Status#UNKNOWN} while the underlying operation
     * continues.
     */
    public CompletableFuture<DatabaseWriteResult> result() {
        return result;
    }

    /**
     * Completes only when the local write/reconciliation task actually ends.
     * Caller deadlines never complete this future early.
     */
    public CompletableFuture<DatabaseWriteResult> completion() {
        return completion;
    }

    static DatabaseWriteOperation completed(DatabaseWriteResult result) {
        CompletableFuture<DatabaseWriteResult> completed = CompletableFuture.completedFuture(result);
        return new DatabaseWriteOperation(result.operationId(), completed, completed);
    }
}
