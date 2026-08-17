package de.tebrox.vertexCore.database;

import java.util.Objects;
import java.util.UUID;

public record DatabaseWriteResult(UUID operationId, Status status, Throwable cause) {

    public enum Status {
        COMMITTED,
        NOT_COMMITTED,
        UNKNOWN
    }

    public DatabaseWriteResult {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(status, "status");
    }

    public static DatabaseWriteResult committed(UUID operationId) {
        return new DatabaseWriteResult(operationId, Status.COMMITTED, null);
    }

    public static DatabaseWriteResult notCommitted(UUID operationId, Throwable cause) {
        return new DatabaseWriteResult(operationId, Status.NOT_COMMITTED, cause);
    }

    public static DatabaseWriteResult unknown(UUID operationId, Throwable cause) {
        return new DatabaseWriteResult(operationId, Status.UNKNOWN, cause);
    }

    public boolean settled() {
        return status != Status.UNKNOWN;
    }
}
