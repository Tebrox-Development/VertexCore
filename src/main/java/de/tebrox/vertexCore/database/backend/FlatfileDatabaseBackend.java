package de.tebrox.vertexCore.database.backend;

import de.tebrox.vertexCore.database.DatabaseBackend;
import de.tebrox.vertexCore.database.DatabaseWriteResult;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FlatfileDatabaseBackend implements DatabaseBackend {

    private static final String TRACKED_PREFIX = "__VERTEXCORE_TRACKED_WRITE_V1__\n";
    private static final int LOCK_STRIPES = 64;

    public static FlatfileDatabaseBackend start(Plugin owner) {
        File root = new File(owner.getDataFolder(), "data");
        if (!root.exists() && !root.mkdirs()) {
            throw new RuntimeException("Failed to create json root: " + root.getAbsolutePath());
        }
        return new FlatfileDatabaseBackend(root);
    }

    private final File root;
    private final Object[] writeLocks = new Object[LOCK_STRIPES];

    FlatfileDatabaseBackend(File root) {
        this.root = root;
        for (int i = 0; i < writeLocks.length; i++) {
            writeLocks[i] = new Object();
        }
    }

    @Override
    public String get(String table, String uniqueId) {
        File f = file(table, uniqueId);
        if (!f.exists()) return null;
        try {
            return decodePayload(Files.readString(f.toPath(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read: " + f.getAbsolutePath(), e);
        }
    }

    @Override
    public void set(String table, String uniqueId, String json) {
        File f = file(table, uniqueId);
        File parent = f.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("Failed to create folder: " + parent.getAbsolutePath());
        }
        try {
            Files.writeString(f.toPath(), json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write: " + f.getAbsolutePath(), e);
        }
    }

    @Override
    public DatabaseWriteResult writeTracked(String table, String uniqueId, String json, UUID operationId) {
        synchronized (lockFor(table, uniqueId)) {
            File f = file(table, uniqueId);
            File parent = f.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                return DatabaseWriteResult.notCommitted(
                        operationId,
                        new RuntimeException("Failed to create folder: " + parent.getAbsolutePath())
                );
            }

            Path temp = null;
            try {
                temp = Files.createTempFile(parent.toPath(), sanitize(uniqueId) + ".", ".vc-write");
                String envelope = TRACKED_PREFIX + operationId + "\n" + json;
                Files.writeString(temp, envelope, StandardCharsets.UTF_8);
                try {
                    Files.move(temp, f.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temp, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return DatabaseWriteResult.committed(operationId);
            } catch (Exception ex) {
                DatabaseWriteResult reconciled = reconcileTrackedWrite(table, uniqueId, operationId);
                if (reconciled.status() == DatabaseWriteResult.Status.COMMITTED) return reconciled;
                if (reconciled.status() == DatabaseWriteResult.Status.NOT_COMMITTED) {
                    return DatabaseWriteResult.notCommitted(operationId, ex);
                }
                if (reconciled.cause() != null && reconciled.cause() != ex) {
                    reconciled.cause().addSuppressed(ex);
                }
                return reconciled;
            } finally {
                if (temp != null) {
                    try {
                        Files.deleteIfExists(temp);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @Override
    public DatabaseWriteResult reconcileTrackedWrite(String table, String uniqueId, UUID operationId) {
        synchronized (lockFor(table, uniqueId)) {
            File f = file(table, uniqueId);
            if (!f.exists()) return DatabaseWriteResult.notCommitted(operationId, null);
            try {
                String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                String storedOperation = trackedOperationId(content);
                if (operationId.toString().equals(storedOperation)) {
                    return DatabaseWriteResult.committed(operationId);
                }
                return DatabaseWriteResult.notCommitted(operationId, null);
            } catch (Exception ex) {
                return DatabaseWriteResult.unknown(operationId, ex);
            }
        }
    }

    @Override
    public void delete(String table, String uniqueId) {
        File f = file(table, uniqueId);
        if (f.exists()) f.delete();
    }

    @Override
    public boolean exists(String table, String uniqueId) {
        return file(table, uniqueId).exists();
    }

    @Override
    public List<String[]> loadAllRaw(String table) {
        File dir = tableDir(table);
        List<String[]> out = new ArrayList<>();
        if (!dir.exists()) return out;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return out;

        for (File f : files) {
            try {
                String id = f.getName().substring(0, f.getName().length() - 5);
                String json = decodePayload(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                out.add(new String[]{id, json});
            } catch (Exception e) {
                throw new RuntimeException("Failed to read: " + f.getAbsolutePath(), e);
            }
        }
        return out;
    }

    private Object lockFor(String table, String uniqueId) {
        int hash = 31 * sanitize(table).hashCode() + sanitize(uniqueId).hashCode();
        return writeLocks[Math.floorMod(hash, writeLocks.length)];
    }

    private File tableDir(String table) {
        return new File(root, sanitize(table));
    }

    private File file(String table, String uniqueId) {
        return new File(tableDir(table), sanitize(uniqueId) + ".json");
    }

    private static String decodePayload(String content) {
        if (!content.startsWith(TRACKED_PREFIX)) return content;
        int operationEnd = content.indexOf('\n', TRACKED_PREFIX.length());
        if (operationEnd < 0) throw new IllegalStateException("Corrupted tracked-write envelope");
        return content.substring(operationEnd + 1);
    }

    private static String trackedOperationId(String content) {
        if (!content.startsWith(TRACKED_PREFIX)) return null;
        int operationEnd = content.indexOf('\n', TRACKED_PREFIX.length());
        if (operationEnd < 0) throw new IllegalStateException("Corrupted tracked-write envelope");
        return content.substring(TRACKED_PREFIX.length(), operationEnd);
    }

    private static String sanitize(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
    }

    @Override
    public void close() {}
}
