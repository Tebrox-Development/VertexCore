package de.tebrox.vertexCore.database.backend;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.tebrox.vertexCore.database.DatabaseWriteResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class JdbcDatabaseBackendTrackedWriteTest {

    private HikariDataSource dataSource;
    private JdbcDatabaseBackend backend;

    @BeforeEach
    void setUp() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:vertexcore-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(4);
        dataSource = new HikariDataSource(config);
        backend = new JdbcDatabaseBackend(dataSource, "h2");
    }

    @AfterEach
    void tearDown() {
        if (backend != null) backend.close();
    }

    @Test
    void trackedWriteCanBeStronglyReconciledAsCommitted() {
        UUID operationId = UUID.randomUUID();
        DatabaseWriteResult write = backend.writeTracked(
                "vault_data", "vault-1", "{\"revision\":1}", operationId
        );
        assertEquals(DatabaseWriteResult.Status.COMMITTED, write.status());
        assertEquals("{\"revision\":1}", backend.get("vault_data", "vault-1"));

        DatabaseWriteResult reconciled = backend.reconcileTrackedWrite(
                "vault_data", "vault-1", operationId
        );
        assertEquals(DatabaseWriteResult.Status.COMMITTED, reconciled.status());
    }

    @Test
    void reconciliationProvesUncommittedOperationWhenFenceContainsAnotherOperation() {
        UUID committedOperation = UUID.randomUUID();
        UUID neverCommittedOperation = UUID.randomUUID();
        assertEquals(
                DatabaseWriteResult.Status.COMMITTED,
                backend.writeTracked("vault_data", "vault-1", "{\"revision\":1}", committedOperation).status()
        );

        DatabaseWriteResult reconciled = backend.reconcileTrackedWrite(
                "vault_data", "vault-1", neverCommittedOperation
        );
        assertEquals(DatabaseWriteResult.Status.NOT_COMMITTED, reconciled.status());
    }

    @Test
    void reconciliationWaitsForTheDatabaseFenceLock() throws Exception {
        UUID operationId = UUID.randomUUID();
        assertEquals(
                DatabaseWriteResult.Status.COMMITTED,
                backend.writeTracked("vault_data", "vault-1", "{\"revision\":1}", operationId).status()
        );

        try (Connection lockConnection = dataSource.getConnection()) {
            lockConnection.setAutoCommit(false);
            try (PreparedStatement ps = lockConnection.prepareStatement(
                    "SELECT last_operation_id FROM " + JdbcDatabaseBackend.WRITE_FENCE_TABLE
                            + " WHERE table_name=? AND unique_id=? FOR UPDATE"
            )) {
                ps.setString(1, "vault_data");
                ps.setString(2, "vault-1");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                }
            }

            CompletableFuture<DatabaseWriteResult> reconciliation = CompletableFuture.supplyAsync(() ->
                    backend.reconcileTrackedWrite("vault_data", "vault-1", operationId)
            );

            Thread.sleep(100);
            assertFalse(reconciliation.isDone(), "reconciliation must wait for the strong DB fence");
            lockConnection.commit();

            DatabaseWriteResult result = reconciliation.get(1, TimeUnit.SECONDS);
            assertEquals(DatabaseWriteResult.Status.COMMITTED, result.status());
        }
    }
}
