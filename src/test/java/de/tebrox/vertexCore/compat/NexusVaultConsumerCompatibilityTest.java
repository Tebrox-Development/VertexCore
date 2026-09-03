package de.tebrox.vertexCore.compat;

import de.tebrox.vertexCore.VertexCoreApi;
import de.tebrox.vertexCore.database.DataObject;
import de.tebrox.vertexCore.database.Database;
import de.tebrox.vertexCore.database.DatabaseBackend;
import de.tebrox.vertexCore.database.DatabaseReconciliationResult;
import de.tebrox.vertexCore.database.DatabaseService;
import de.tebrox.vertexCore.database.DatabaseSettings;
import de.tebrox.vertexCore.database.DatabaseWriteOperation;
import de.tebrox.vertexCore.database.DatabaseWriteResult;
import de.tebrox.vertexCore.database.JsonCodec;
import de.tebrox.vertexCore.database.annotation.DbExpose;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Compile-time contract for the VertexCore APIs used by NexusVault dev.
 *
 * <p>The snapshot was refreshed read-only from NexusVault dev at
 * {@value #NEXUSVAULT_DEV_SHA}. No NexusVault checkout or repository write is
 * required by CI. Keeping the representative calls in Java source makes Maven
 * test compilation fail as soon as a used VertexCore 1.x signature becomes
 * source-incompatible.</p>
 */
final class NexusVaultConsumerCompatibilityTest {

    static final String NEXUSVAULT_DEV_SHA = "412becf44e6de104cfb0804f7735ff012516c0cb";

    @Test
    void nexusVaultSnapshotIsPinnedToAFullCommitSha() {
        assertEquals(40, NEXUSVAULT_DEV_SHA.length());
    }

    /**
     * Intentionally never executed: javac type-checks this method during the
     * normal Maven test-compile phase and therefore validates the consumer
     * surface against the current VertexCore sources.
     */
    @SuppressWarnings({"unused", "ConstantConditions"})
    private static void compileCurrentNexusVaultUsage(
            Plugin owner,
            NexusVaultSettings settings,
            Executor ioExecutor,
            NexusVaultRecord record) {

        Database<NexusVaultRecord> database = new Database<>(owner, settings, NexusVaultRecord.class);
        NexusVaultRecord loaded = database.loadObject(record.getUniqueId());
        database.saveObject(record);

        VertexCoreApi api = VertexCoreApi.get();
        DatabaseService databaseService = api.databaseService();
        DatabaseBackend backend = api.backendFor(owner, settings);
        JsonCodec json = api.json();
        Executor asyncExecutor = api.asyncExecutor();
        Executor mainExecutor = api.mainExecutor();
        backend.warmup();
        api.closeFor(owner);

        String encoded = json.toJson(NexusVaultRecord.class, record);
        NexusVaultRecord decoded = json.fromJson(NexusVaultRecord.class, encoded);

        UUID operationId = UUID.randomUUID();
        DatabaseWriteOperation operation = databaseService.submitTrackedWrite(
                owner,
                settings,
                "nexusvault_vaults_v1",
                record.getUniqueId(),
                encoded,
                operationId);
        UUID returnedOperationId = operation.operationId();
        CompletableFuture<DatabaseWriteResult> result = operation.result();
        CompletableFuture<DatabaseWriteResult> completion = operation.completion();
        CompletableFuture<DatabaseReconciliationResult> reconciliation =
                databaseService.reconcileTrackedWrite(
                        owner,
                        settings,
                        "nexusvault_vaults_v1",
                        record.getUniqueId());

        String raw = backend.get("nexusvault_vaults_v1", record.getUniqueId());
        DatabaseWriteResult directWrite = backend.writeTracked(
                "nexusvault_vaults_v1",
                record.getUniqueId(),
                encoded,
                operationId);
        DatabaseWriteResult directReconciliation = backend.reconcileTrackedWrite(
                "nexusvault_vaults_v1",
                record.getUniqueId(),
                operationId);

        DatabaseWriteResult notCommitted =
                DatabaseWriteResult.notCommitted(operationId, new IllegalStateException("compatibility fixture"));
        DatabaseWriteResult.Status status = directWrite.status();
        Throwable cause = directWrite.cause();

        DatabaseReconciliationResult reconciled = DatabaseReconciliationResult.reconciled(directReconciliation);
        DatabaseReconciliationResult stillUnknown = DatabaseReconciliationResult.stillUnknown(operationId, cause);
        boolean isReconciled = reconciled.reconciled();
        Throwable reconciliationCause = stillUnknown.cause();

        // Keep local variables observably connected so static analyzers do not
        // mistake the contract calls for accidental dead code.
        if (loaded == decoded && ioExecutor == asyncExecutor && mainExecutor == asyncExecutor && raw == null
                && returnedOperationId == operationId && result == completion
                && reconciliation.isDone() && notCommitted.status() == status
                && isReconciled && reconciliationCause == cause) {
            throw new AssertionError("compile-only compatibility contract");
        }
    }

    /** Mirrors NexusVaultDatabaseSettings' direct DatabaseSettings implementation. */
    record NexusVaultSettings(
            String backend,
            long timeoutMillis,
            int poolSize,
            String mysqlUrl,
            String mysqlUser,
            String mysqlPassword,
            String tablePrefix) implements DatabaseSettings {
    }

    static final class NexusVaultRecord implements DataObject {
        private String uniqueId;

        @DbExpose
        private String payload;

        @Override
        public String getUniqueId() {
            return uniqueId;
        }

        @Override
        public void setUniqueId(String uniqueId) {
            this.uniqueId = uniqueId;
        }
    }
}
