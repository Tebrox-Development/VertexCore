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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FlatfileDatabaseBackend implements DatabaseBackend {

    private static final String TRACKED_PREFIX = "__VERTEXCORE_TRACKED_WRITE_V1__\n";
    private static final String ENCODED_ID_DIRECTORY = ".vertexcore-ids-v2";
    private static final String TOMBSTONE_EXTENSION = ".deleted";
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
        File f = readableFile(table, uniqueId);
        if (f == null) return null;
        try {
            return decodePayload(Files.readString(f.toPath(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read: " + f.getAbsolutePath(), e);
        }
    }

    @Override
    public void set(String table, String uniqueId, String json) {
        File f = encodedFile(table, uniqueId);
        File parent = f.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("Failed to create folder: " + parent.getAbsolutePath());
        }
        try {
            Files.writeString(f.toPath(), json, StandardCharsets.UTF_8);
            clearTombstone(table, uniqueId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write: " + f.getAbsolutePath(), e);
        }
    }

    @Override
    public DatabaseWriteResult writeTracked(String table, String uniqueId, String json, UUID operationId) {
        synchronized (lockFor(table, uniqueId)) {
            File f = encodedFile(table, uniqueId);
            File parent = f.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                return DatabaseWriteResult.notCommitted(
                        operationId,
                        new RuntimeException("Failed to create folder: " + parent.getAbsolutePath())
                );
            }

            Path temp = null;
            try {
                temp = Files.createTempFile(parent.toPath(), "vc-write-", ".tmp");
                String envelope = TRACKED_PREFIX + operationId + "\n" + json;
                Files.writeString(temp, envelope, StandardCharsets.UTF_8);
                try {
                    Files.move(temp, f.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temp, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                clearTombstone(table, uniqueId);
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
            File f = readableFile(table, uniqueId);
            if (f == null) return DatabaseWriteResult.notCommitted(operationId, null);
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
        File tombstone = tombstoneFile(table, uniqueId);
        File parent = tombstone.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("Failed to create folder: " + parent.getAbsolutePath());
        }
        try {
            Files.writeString(tombstone.toPath(), "", StandardCharsets.UTF_8);
            Files.deleteIfExists(encodedFile(table, uniqueId).toPath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete flatfile id: " + uniqueId, e);
        }
    }

    @Override
    public boolean exists(String table, String uniqueId) {
        if (encodedFile(table, uniqueId).exists()) return true;
        if (tombstoneFile(table, uniqueId).exists()) return false;
        return legacyFile(table, uniqueId).exists();
    }

    @Override
    public List<String[]> loadAllRaw(String table) {
        Map<String, String> records = new LinkedHashMap<>();
        File dir = tableDir(table);
        if (!dir.exists()) return new ArrayList<>();

        File[] legacyFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (legacyFiles != null) {
            for (File f : legacyFiles) {
                try {
                    String id = f.getName().substring(0, f.getName().length() - 5);
                    if (tombstoneFile(table, id).exists()) continue;
                    String json = decodePayload(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                    records.put(id, json);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to read: " + f.getAbsolutePath(), e);
                }
            }
        }

        File encodedDir = encodedIdDir(table);
        File[] encodedFiles = encodedDir.listFiles((d, name) -> name.endsWith(".json"));
        if (encodedFiles != null) {
            for (File f : encodedFiles) {
                try {
                    String encodedId = f.getName().substring(0, f.getName().length() - 5);
                    String id = decodeId(encodedId);
                    String json = decodePayload(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                    records.put(id, json);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to read: " + f.getAbsolutePath(), e);
                }
            }
        }

        List<String[]> out = new ArrayList<>(records.size());
        for (Map.Entry<String, String> entry : records.entrySet()) {
            out.add(new String[]{entry.getKey(), entry.getValue()});
        }
        return out;
    }

    private Object lockFor(String table, String uniqueId) {
        int hash = 31 * sanitize(table).hashCode() + uniqueId.hashCode();
        return writeLocks[Math.floorMod(hash, writeLocks.length)];
    }

    private File tableDir(String table) {
        return new File(root, sanitize(table));
    }

    private File encodedIdDir(String table) {
        return new File(tableDir(table), ENCODED_ID_DIRECTORY);
    }

    private File encodedFile(String table, String uniqueId) {
        return new File(encodedIdDir(table), encodeId(uniqueId) + ".json");
    }

    private File tombstoneFile(String table, String uniqueId) {
        return new File(encodedIdDir(table), encodeId(uniqueId) + TOMBSTONE_EXTENSION);
    }

    private File legacyFile(String table, String uniqueId) {
        return new File(tableDir(table), sanitize(uniqueId) + ".json");
    }

    private File readableFile(String table, String uniqueId) {
        File encoded = encodedFile(table, uniqueId);
        if (encoded.exists()) return encoded;
        if (tombstoneFile(table, uniqueId).exists()) return null;
        File legacy = legacyFile(table, uniqueId);
        return legacy.exists() ? legacy : null;
    }

    private void clearTombstone(String table, String uniqueId) {
        try {
            Files.deleteIfExists(tombstoneFile(table, uniqueId).toPath());
        } catch (Exception ignored) {
        }
    }

    private static String encodeId(String id) {
        StringBuilder encoded = new StringBuilder(id.length() * 4);
        for (int i = 0; i < id.length(); i++) {
            int value = id.charAt(i);
            encoded.append(Character.forDigit((value >>> 12) & 0x0f, 16));
            encoded.append(Character.forDigit((value >>> 8) & 0x0f, 16));
            encoded.append(Character.forDigit((value >>> 4) & 0x0f, 16));
            encoded.append(Character.forDigit(value & 0x0f, 16));
        }
        return encoded.toString();
    }

    private static String decodeId(String encoded) {
        if ((encoded.length() & 3) != 0) {
            throw new IllegalArgumentException("Invalid encoded flatfile id");
        }
        char[] chars = new char[encoded.length() / 4];
        for (int i = 0; i < encoded.length(); i += 4) {
            int value = 0;
            for (int offset = 0; offset < 4; offset++) {
                int digit = Character.digit(encoded.charAt(i + offset), 16);
                if (digit < 0) {
                    throw new IllegalArgumentException("Invalid encoded flatfile id");
                }
                value = (value << 4) | digit;
            }
            chars[i / 4] = (char) value;
        }
        return new String(chars);
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
