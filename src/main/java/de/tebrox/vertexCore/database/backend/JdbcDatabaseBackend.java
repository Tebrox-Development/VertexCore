package de.tebrox.vertexCore.database.backend;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.tebrox.vertexCore.database.DatabaseBackend;
import de.tebrox.vertexCore.database.DatabaseSettings;
import de.tebrox.vertexCore.database.DatabaseWriteResult;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class JdbcDatabaseBackend implements DatabaseBackend {

    static final String WRITE_FENCE_TABLE = "vertexcore_write_fence";

    public static HikariDataSource createDataSource(Plugin owner, DatabaseSettings settings) {
        String backend = settings.backend().toLowerCase();
        String url;
        String user;
        String pass;

        if(backend.equals("h2")) {
            File folder = owner.getDataFolder();
            if(!folder.exists()) folder.mkdirs();
            String abs = new File(folder, "database").getAbsolutePath().replace("\\", "/");
            url = "jdbc:h2:file:" + abs + ";AUTO_SERVER=TRUE";
            user = "sa";
            pass = "";
        }else if(backend.equals("mysql")) {
            url = settings.mysqlUrl();
            user = settings.mysqlUser();
            pass = settings.mysqlPassword();
            if(url == null || url.isBlank()) {
                throw new IllegalArgumentException("mysqlUrl is required when backend=mysql");
            }
            if(user == null) user = "";
            if(pass == null) pass = "";
        } else {
            throw new IllegalArgumentException("Jdbc backend used but backend=" + settings.backend());
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(Math.max(1, settings.poolSize()));
        cfg.setPoolName("VertexCore-" + owner.getName());
        cfg.setInitializationFailTimeout(10_000);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(10_000);
        cfg.setValidationTimeout(5_000);

        String lower = url.toLowerCase();
        if (lower.startsWith("jdbc:h2:")) cfg.setDriverClassName("org.h2.Driver");
        if (lower.startsWith("jdbc:mysql:")) cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        try {
            if (lower.startsWith("jdbc:h2:")) Class.forName("org.h2.Driver", true, JdbcDatabaseBackend.class.getClassLoader());
            if (lower.startsWith("jdbc:mysql:")) Class.forName("com.mysql.cj.jdbc.Driver", true, JdbcDatabaseBackend.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC driver missing for url=" + url, e);
        }

        return new HikariDataSource(cfg);
    }

    private final HikariDataSource ds;
    private final Dialect dialect;
    private final Set<String> ensuredTables = ConcurrentHashMap.newKeySet();
    private volatile boolean writeFenceTableEnsured;

    public JdbcDatabaseBackend(HikariDataSource ds, String jdbcUrl) {
        this.ds = ds;
        this.dialect = jdbcUrl.toLowerCase().contains("mysql") ? Dialect.MYSQL : Dialect.H2;
    }

    public void ensureTable(String table) {
        table = sanitizeTableName(table);
        if (!ensuredTables.add(table)) return;

        String ddl = switch (dialect) {
            case MYSQL -> """
                CREATE TABLE IF NOT EXISTS %s (
                  unique_id VARCHAR(128) NOT NULL,
                  json LONGTEXT NOT NULL,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (unique_id)
                )
                """.formatted(table);
            case H2 -> """
                CREATE TABLE IF NOT EXISTS %s (
                  unique_id VARCHAR(128) NOT NULL,
                  json CLOB NOT NULL,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (unique_id)
                )
                """.formatted(table);
        };

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute(ddl);
        } catch (SQLException e) {
            ensuredTables.remove(table);
            throw new RuntimeException("Failed to init table: " + table, e);
        }
    }

    @Override
    public String get(String table, String uniqueId) {
        table = sanitizeTableName(table);
        ensureTable(table);
        String sql = "SELECT json FROM " + table + " WHERE unique_id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uniqueId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB get failed (table=" + table + ")", e);
        }
    }

    @Override
    public void set(String table, String uniqueId, String json) {
        table = sanitizeTableName(table);
        ensureTable(table);
        try (Connection c = ds.getConnection()) {
            switch (dialect) {
                case MYSQL -> upsertMySql(c, table, uniqueId, json);
                case H2 -> upsertH2(c, table, uniqueId, json);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB set failed (table=" + table + ")", e);
        }
    }

    @Override
    public DatabaseWriteResult writeTracked(String table, String uniqueId, String json, UUID operationId) {
        table = sanitizeTableName(table);
        try {
            ensureTable(table);
            ensureWriteFenceTable();
            ensureFenceRow(table, uniqueId);
        } catch (RuntimeException ex) {
            return DatabaseWriteResult.notCommitted(operationId, ex);
        }

        boolean commitStarted = false;
        SQLException failure = null;

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                lockFenceRow(c, table, uniqueId);
                switch (dialect) {
                    case MYSQL -> upsertMySql(c, table, uniqueId, json);
                    case H2 -> upsertH2(c, table, uniqueId, json);
                }
                setFenceOperation(c, table, uniqueId, operationId);
                commitStarted = true;
                c.commit();
                return DatabaseWriteResult.committed(operationId);
            } catch (SQLException ex) {
                if (!commitStarted) rollbackQuietly(c);
                throw ex;
            }
        } catch (SQLException ex) {
            failure = ex;
        }

        if (!commitStarted) {
            return DatabaseWriteResult.notCommitted(operationId, failure);
        }

        DatabaseWriteResult reconciled = reconcileTrackedWrite(table, uniqueId, operationId);
        if (reconciled.status() == DatabaseWriteResult.Status.UNKNOWN
                && failure != null
                && reconciled.cause() != null
                && reconciled.cause() != failure) {
            reconciled.cause().addSuppressed(failure);
        }
        return reconciled;
    }

    @Override
    public DatabaseWriteResult reconcileTrackedWrite(String table, String uniqueId, UUID operationId) {
        table = sanitizeTableName(table);
        try {
            ensureTable(table);
            ensureWriteFenceTable();
            ensureFenceRow(table, uniqueId);
            try (Connection c = ds.getConnection()) {
                c.setAutoCommit(false);
                try {
                    String lastOperationId = lockFenceRow(c, table, uniqueId);
                    c.commit();
                    if (operationId.toString().equals(lastOperationId)) {
                        return DatabaseWriteResult.committed(operationId);
                    }
                    return DatabaseWriteResult.notCommitted(operationId, null);
                } catch (SQLException ex) {
                    rollbackQuietly(c);
                    throw ex;
                }
            }
        } catch (RuntimeException | SQLException ex) {
            return DatabaseWriteResult.unknown(operationId, ex);
        }
    }

    private void upsertMySql(Connection c, String table, String id, String json) throws SQLException {
        String sql = """
            INSERT INTO %s (unique_id, json) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE json=VALUES(json)
            """.formatted(table);
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, json);
            ps.executeUpdate();
        }
    }

    private void upsertH2(Connection c, String table, String id, String json) throws SQLException {
        String sql = """
            MERGE INTO %s (unique_id, json) KEY (unique_id) VALUES (?, ?)
            """.formatted(table);
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, json);
            ps.executeUpdate();
        }
    }

    private void ensureWriteFenceTable() {
        if (writeFenceTableEnsured) return;
        synchronized (this) {
            if (writeFenceTableEnsured) return;
            String ddl = """
                CREATE TABLE IF NOT EXISTS %s (
                  table_name VARCHAR(64) NOT NULL,
                  unique_id VARCHAR(128) NOT NULL,
                  last_operation_id VARCHAR(36),
                  PRIMARY KEY (table_name, unique_id)
                )
                """.formatted(WRITE_FENCE_TABLE);
            try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
                st.execute(ddl);
                writeFenceTableEnsured = true;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to init tracked-write fence table", e);
            }
        }
    }

    private void ensureFenceRow(String table, String uniqueId) {
        String sql = """
            INSERT INTO %s (table_name, unique_id, last_operation_id)
            SELECT ?, ?, NULL
            WHERE NOT EXISTS (
              SELECT 1 FROM %s WHERE table_name=? AND unique_id=?
            )
            """.formatted(WRITE_FENCE_TABLE, WRITE_FENCE_TABLE);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, uniqueId);
            ps.setString(3, table);
            ps.setString(4, uniqueId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (!isDuplicateKey(e)) {
                throw new RuntimeException("Failed to initialize tracked-write fence row", e);
            }
        }
    }

    private String lockFenceRow(Connection c, String table, String uniqueId) throws SQLException {
        String sql = "SELECT last_operation_id FROM " + WRITE_FENCE_TABLE
                + " WHERE table_name=? AND unique_id=? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, uniqueId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Tracked-write fence row disappeared");
                return rs.getString(1);
            }
        }
    }

    private void setFenceOperation(Connection c, String table, String uniqueId, UUID operationId) throws SQLException {
        String sql = "UPDATE " + WRITE_FENCE_TABLE
                + " SET last_operation_id=? WHERE table_name=? AND unique_id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, operationId.toString());
            ps.setString(2, table);
            ps.setString(3, uniqueId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Tracked-write fence row update affected an unexpected number of rows");
            }
        }
    }

    private static void rollbackQuietly(Connection c) {
        try {
            c.rollback();
        } catch (SQLException ignored) {
        }
    }

    private static boolean isDuplicateKey(SQLException e) {
        String state = e.getSQLState();
        return e instanceof SQLIntegrityConstraintViolationException
                || "23505".equals(state)
                || "23000".equals(state);
    }

    @Override
    public void delete(String table, String uniqueId) {
        table = sanitizeTableName(table);
        ensureTable(table);
        String sql = "DELETE FROM " + table + " WHERE unique_id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uniqueId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB delete failed (table=" + table + ")", e);
        }
    }

    @Override
    public boolean exists(String table, String uniqueId) {
        table = sanitizeTableName(table);
        ensureTable(table);
        String sql = "SELECT 1 FROM " + table + " WHERE unique_id=? LIMIT 1";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uniqueId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB exists failed (table=" + table + ")", e);
        }
    }

    @Override
    public void warmup() {
        try (Connection c = ds.getConnection()) {
        } catch(Exception e) {
            throw new RuntimeException("DB warmup failed", e);
        }
    }

    @Override
    public void close() {
        ds.close();
    }

    private static String sanitizeTableName(String table) {
        if (table == null || table.isBlank()) throw new IllegalArgumentException("table is blank");
        String safe = table.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (Character.isDigit(safe.charAt(0))) safe = "_" + safe;
        return safe;
    }

    @Override
    public List<String[]> loadAllRaw(String table) {
        table = sanitizeTableName(table);
        ensureTable(table);
        String sql = "SELECT unique_id, json FROM " + table;
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String[]> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new String[]{ rs.getString(1), rs.getString(2) });
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("DB loadAll failed (table=" + table + ")", e);
        }
    }

    private enum Dialect { MYSQL, H2 }
}
