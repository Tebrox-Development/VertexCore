package de.tebrox.vertexCore.database.backend;

import de.tebrox.vertexCore.database.DatabaseWriteResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlatfileDatabaseBackendTrackedWriteTest {

    @TempDir
    File tempDir;

    @Test
    void trackedEnvelopeRemainsTransparentToReadersAndCanBeReconciled() {
        FlatfileDatabaseBackend backend = new FlatfileDatabaseBackend(tempDir);
        UUID operationId = UUID.randomUUID();

        DatabaseWriteResult write = backend.writeTracked(
                "vault_data", "vault-1", "{\"revision\":1}", operationId
        );

        assertEquals(DatabaseWriteResult.Status.COMMITTED, write.status());
        assertEquals("{\"revision\":1}", backend.get("vault_data", "vault-1"));
        assertEquals(
                DatabaseWriteResult.Status.COMMITTED,
                backend.reconcileTrackedWrite("vault_data", "vault-1", operationId).status()
        );
    }

    @Test
    void legacyRawJsonFilesRemainReadable() throws Exception {
        FlatfileDatabaseBackend backend = new FlatfileDatabaseBackend(tempDir);
        File tableDir = new File(tempDir, "vault_data");
        tableDir.mkdirs();
        Files.writeString(
                new File(tableDir, "vault-1.json").toPath(),
                "{\"legacy\":true}",
                StandardCharsets.UTF_8
        );

        assertEquals("{\"legacy\":true}", backend.get("vault_data", "vault-1"));
    }
}
