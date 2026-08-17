package de.tebrox.vertexCore.database;

import java.util.UUID;

public final class DatabaseWriteException extends RuntimeException {

    private final UUID operationId;
    private final DatabaseWriteResult.Status status;

    public DatabaseWriteException(DatabaseWriteResult result) {
        super(message(result), result.cause());
        this.operationId = result.operationId();
        this.status = result.status();
    }

    public UUID operationId() {
        return operationId;
    }

    public DatabaseWriteResult.Status status() {
        return status;
    }

    private static String message(DatabaseWriteResult result) {
        return "Database write " + result.operationId() + " ended with status " + result.status();
    }
}
