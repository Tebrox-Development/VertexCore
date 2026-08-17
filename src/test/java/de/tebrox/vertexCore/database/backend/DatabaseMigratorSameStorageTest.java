package de.tebrox.vertexCore.database.backend;

import de.tebrox.vertexCore.database.migration.DatabaseMigrator;
import de.tebrox.vertexCore.database.migration.MigrationOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigratorSameStorageTest {

    @TempDir
    File tempDir;

    @Test
    void rejectsDistinctBackendInstancesForSameStorageBeforeSourceDeletion() {
        FlatfileDatabaseBackend source = new FlatfileDatabaseBackend(tempDir);
        FlatfileDatabaseBackend target = new FlatfileDatabaseBackend(tempDir);
        source.set("vault_data", "vault-1", "{\"value\":1}");

        MigrationOptions options = new MigrationOptions();
        options.overwrite = true;
        options.deleteSourceAfter = true;

        assertTrue(source.sameStorageAs(target));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new DatabaseMigrator().migrateTableRaw(
                        source,
                        target,
                        "vault_data",
                        options,
                        null
                )
        );

        assertEquals("Migration source and target refer to the same storage.", error.getMessage());
        assertEquals("{\"value\":1}", source.get("vault_data", "vault-1"));
    }
}
