package de.tebrox.vertexCore.database;

import java.util.UUID;

public record DatabaseReconciliationResult(
        Status status,
        UUID operationId,
        DatabaseWriteResult.Status resolvedWriteStatus,
        Throwable cause
) {

    public enum Status {
        RECONCILED,
        STILL_UNKNOWN
    }

    public static DatabaseReconciliationResult noPendingWrite() {
        return new DatabaseReconciliationResult(Status.RECONCILED, null, null, null);
    }

    public static DatabaseReconciliationResult reconciled(DatabaseWriteResult result) {
        if (!result.settled()) {
            throw new IllegalArgumentException("Cannot mark an UNKNOWN write as reconciled");
        }
        return new DatabaseReconciliationResult(Status.RECONCILED, result.operationId(), result.status(), result.cause());
    }

    public static DatabaseReconciliationResult stillUnknown(UUID operationId, Throwable cause) {
        return new DatabaseReconciliationResult(Status.STILL_UNKNOWN, operationId, DatabaseWriteResult.Status.UNKNOWN, cause);
    }

    public boolean reconciled() {
        return status == Status.RECONCILED;
    }
}
