package de.tebrox.vertexCore.database.backend;

import de.tebrox.vertexCore.database.DatabaseWriteResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
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
        assertEquals("{\"legacy\":true}", loadAll(backend, "vault_data").get("vault-1"));
    }

    @Test
    void idsRoundTripWithoutCaseOrSanitizationCollisions() {
        FlatfileDatabaseBackend backend = new FlatfileDatabaseBackend(tempDir);

        backend.set("vault_data", "ABC", "upper");
        backend.set("vault_data", "abc", "lower");
        backend.set("vault_data", "foo/bar", "slash");
        backend.set("vault_data", "foo_bar", "underscore");
        backend.set("vault_data", "Ümlaut/ID", "unicode");

        assertEquals("upper", backend.get("vault_data", "ABC"));
        assertEquals("lower", backend.get("vault_data", "abc"));
        assertEquals("slash", backend.get("vault_data", "foo/bar"));
        assertEquals("underscore", backend.get("vault_data", "foo_bar"));
        assertEquals("unicode", backend.get("vault_data", "Ümlaut/ID"));

        Map<String, String> all = loadAll(backend, "vault_data");
        assertEquals(5, all.size());
        assertEquals("upper", all.get("ABC"));
        assertEquals("lower", all.get("abc"));
        assertEquals("slash", all.get("foo/bar"));
        assertEquals("underscore", all.get("foo_bar"));
        assertEquals("unicode", all.get("Ümlaut/ID"));
    }

    @Test
    void newWritesDoNotOverwriteCollidingLegacyFiles() throws Exception {
        FlatfileDatabaseBackend backend = new FlatfileDatabaseBackend(tempDir);
        File tableDir = new File(tempDir, "vault_data");
        tableDir.mkdirs();
        File legacy = new File(tableDir, "foo_bar.json");
        Files.writeString(legacy.toPath(), "legacy", StandardCharsets.UTF_8);

        backend.set("vault_data", "foo/bar", "new");

        assertEquals("legacy", Files.readString(legacy.toPath(), StandardCharsets.UTF_8));
        assertEquals("new", backend.get("vault_data", "foo/bar"));
        assertEquals("legacy", backend.get("vault_data", "foo_bar"));

        Map<String, String> all = loadAll(backend, "vault_data");
        assertEquals(2, all.size());
        assertEquals("new", all.get("foo/bar"));
        assertEquals("legacy", all.get("foo_bar"));
    }

    private static Map<String, String> loadAll(FlatfileDatabaseBackend backend, String table) {
        Map<String, String> result = new HashMap<>();
        for (String[] entry : backend.loadAllRaw(table)) {
            result.put(entry[0], entry[1]);
        }
        return result;
    }
}
