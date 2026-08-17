package de.tebrox.vertexCore.database;

import java.util.List;
import java.util.UUID;

public interface DatabaseBackend extends AutoCloseable {
    String get(String table, String uniqueId);
    void set(String table, String uniqueId, String json);
    void delete(String table, String uniqueId);
    boolean exists(String table, String uniqueId);

    List<String[]> loadAllRaw(String table); // each entry: [uniqueId, json]

    /**
     * Returns whether this backend and another backend address the same storage.
     *
     * <p>The default deliberately recognizes only the same backend instance so
     * existing third-party implementations remain compatible. Built-in backends
     * override this when they can compare their concrete storage identity.</p>
     */
    default boolean sameStorageAs(DatabaseBackend other) {
        return this == other;
    }

    /**
     * Executes a write that can later be reconciled by operation id.
     *
     * <p>Backends that cannot provide a stronger guarantee may rely on this
     * conservative default. A successful synchronous {@link #set} is treated as
     * committed. An exception is UNKNOWN rather than incorrectly claiming that
     * the remote side definitely did not commit.</p>
     */
    default DatabaseWriteResult writeTracked(String table, String uniqueId, String json, UUID operationId) {
        try {
            set(table, uniqueId, json);
            return DatabaseWriteResult.committed(operationId);
        } catch (RuntimeException ex) {
            return DatabaseWriteResult.unknown(operationId, ex);
        }
    }

    /**
     * Establishes a strong fence for a previously UNKNOWN tracked write.
     *
     * <p>RECONCILED/settled results are only valid if this method proves that
     * the older write can no longer become visible later. The default is
     * intentionally UNKNOWN.</p>
     */
    default DatabaseWriteResult reconcileTrackedWrite(String table, String uniqueId, UUID operationId) {
        return DatabaseWriteResult.unknown(
                operationId,
                new UnsupportedOperationException("Backend does not provide tracked-write reconciliation")
        );
    }

    default void warmup() {}

    @Override void close();
}
