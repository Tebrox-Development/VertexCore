package de.tebrox.vertexCore.database;

import de.tebrox.vertexCore.database.backend.FlatfileDatabaseBackend;
import de.tebrox.vertexCore.database.backend.JdbcDatabaseBackend;
import de.tebrox.vertexCore.util.AsyncQueue;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class DatabaseService implements Listener {
    private final Plugin core;
    private final JsonCodec json = new JsonCodec();
    private final PluginDataRegistry registry;

    private final Map<String, DatabaseBackend> backends = new ConcurrentHashMap<>();
    private final Map<String, AsyncQueue> queues = new ConcurrentHashMap<>();

    private final Map<WriteKey, WriteLane> writeLanes = new ConcurrentHashMap<>();
    private final Map<WriteKey, UUID> unresolvedWrites = new ConcurrentHashMap<>();

    public DatabaseService(Plugin core, PluginDataRegistry registry) {
        this.core = core;
        this.registry = registry;
        Bukkit.getPluginManager().registerEvents(this, core);
    }

    public JsonCodec json() {
        return json;
    }

    public DatabaseBackend backendFor(Plugin owner, DatabaseSettings settings) {
        String fp = fingerprint(owner, settings);

        return backends.computeIfAbsent(fp, k -> {
            return switch (settings.backend().toLowerCase()) {
                case "json" -> FlatfileDatabaseBackend.start(owner);
                case "h2" -> new JdbcDatabaseBackend(JdbcDatabaseBackend.createDataSource(owner, settings), "h2");
                case "mysql" -> new JdbcDatabaseBackend(JdbcDatabaseBackend.createDataSource(owner, settings), "mysql");
                default -> throw new IllegalArgumentException("Unknown backend: " + settings.backend());
            };
        });
    }

    public AsyncQueue queueFor(Plugin owner, long timeoutMillis) {
        String key = owner.getName().toLowerCase();
        AsyncQueue orderingQueue = queues.computeIfAbsent(
                key,
                k -> new AsyncQueue(r -> Bukkit.getScheduler().runTaskAsynchronously(core, r), 0)
        );
        return orderingQueue.withTimeout(timeoutMillis);
    }

    public DatabaseWriteOperation submitTrackedWrite(
            Plugin owner,
            DatabaseSettings settings,
            String table,
            String uniqueId,
            String json,
            UUID operationId
    ) {
        WriteKey key = new WriteKey(fingerprint(owner, settings), table, uniqueId);

        CompletableFuture<DatabaseWriteResult> completion = enqueue(
                key,
                owner,
                settings,
                () -> performTrackedWrite(key, owner, settings, table, uniqueId, json, operationId)
        );

        CompletableFuture<DatabaseWriteResult> callerView = writeTimeoutView(
                completion,
                operationId,
                settings.timeoutMillis()
        );

        return new DatabaseWriteOperation(operationId, callerView, completion);
    }

    public CompletableFuture<DatabaseReconciliationResult> reconcileTrackedWrite(
            Plugin owner,
            DatabaseSettings settings,
            String table,
            String uniqueId
    ) {
        WriteKey key = new WriteKey(fingerprint(owner, settings), table, uniqueId);

        CompletableFuture<DatabaseReconciliationResult> completion = enqueue(
                key,
                owner,
                settings,
                () -> performReconciliation(key, owner, settings, table, uniqueId)
        );

        if (settings.timeoutMillis() <= 0) {
            return completion;
        }

        CompletableFuture<DatabaseReconciliationResult> callerView = new CompletableFuture<>();
        completion.whenComplete((result, error) -> {
            if (error != null) {
                callerView.completeExceptionally(error);
            } else {
                callerView.complete(result);
            }
        });

        CompletableFuture.delayedExecutor(settings.timeoutMillis(), TimeUnit.MILLISECONDS).execute(() -> {
            UUID pending = unresolvedWrites.get(key);
            callerView.complete(DatabaseReconciliationResult.stillUnknown(
                    pending,
                    new TimeoutException(
                            "Database reconciliation timed out after " + settings.timeoutMillis() + " ms"
                    )
            ));
        });

        return callerView;
    }

    public void closeFor(Plugin owner) {
        String prefix = owner.getName().toLowerCase() + "|";
        backends.entrySet().removeIf(e -> {
            if(e.getKey().startsWith(prefix)) {
                e.getValue().close();
                return true;
            }
            return false;
        });
        queues.remove(owner.getName().toLowerCase());
    }

    public CompletableFuture<Void> warmupFor(Plugin plugin) {
        PluginDataRegistry.Entry entry = registry.get(plugin.getName());
        if (entry == null) return CompletableFuture.completedFuture(null);

        DatabaseSettings settings = entry.settingsSupplier().get();

        return CompletableFuture.runAsync(() -> {
            DatabaseBackend backend = backendFor(plugin, settings);
            backend.warmup();
            plugin.getLogger().info("[VertexCore] Database warmup done (" + settings.backend() + ")");
        }, r -> Bukkit.getScheduler().runTaskAsynchronously(core, r));
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        Plugin p = event.getPlugin();
        if(!registry.isRegistered(p.getName())) return;

        warmupFor(p).exceptionally(err -> {
            Throwable u = unwrap(err);
            p.getLogger().severe("[VertexCore] Database warmup failed: " + u.getClass().getName()
                    + (u.getMessage() != null ? " - " + u.getMessage() : ""));
            u.printStackTrace();
            return null;
        });
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        closeFor(event.getPlugin());
    }

    public void closeAll() {
        backends.values().forEach(DatabaseBackend::close);
        backends.clear();
        queues.clear();
        unresolvedWrites.clear();
        writeLanes.clear();
    }

    private DatabaseWriteResult performTrackedWrite(
            WriteKey key,
            Plugin owner,
            DatabaseSettings settings,
            String table,
            String uniqueId,
            String json,
            UUID operationId
    ) {
        final DatabaseBackend backend;
        try {
            backend = backendFor(owner, settings);
        } catch (RuntimeException ex) {
            return DatabaseWriteResult.notCommitted(operationId, ex);
        }

        UUID unresolvedOperation = unresolvedWrites.get(key);
        if (unresolvedOperation != null) {
            DatabaseWriteResult previous = safeReconcile(
                    backend,
                    table,
                    uniqueId,
                    unresolvedOperation
            );

            if (previous.status() == DatabaseWriteResult.Status.UNKNOWN) {
                return DatabaseWriteResult.notCommitted(
                        operationId,
                        new IllegalStateException(
                                "Previous write " + unresolvedOperation
                                        + " is still unresolved; new write was not started",
                                previous.cause()
                        )
                );
            }

            unresolvedWrites.remove(key, unresolvedOperation);
        }

        DatabaseWriteResult result;
        try {
            result = backend.writeTracked(table, uniqueId, json, operationId);
        } catch (RuntimeException ex) {
            result = DatabaseWriteResult.unknown(operationId, ex);
        }

        if (result.status() == DatabaseWriteResult.Status.UNKNOWN) {
            unresolvedWrites.put(key, operationId);
        }

        return result;
    }

    private DatabaseReconciliationResult performReconciliation(
            WriteKey key,
            Plugin owner,
            DatabaseSettings settings,
            String table,
            String uniqueId
    ) {
        UUID unresolvedOperation = unresolvedWrites.get(key);
        if (unresolvedOperation == null) {
            return DatabaseReconciliationResult.noPendingWrite();
        }

        final DatabaseBackend backend;
        try {
            backend = backendFor(owner, settings);
        } catch (RuntimeException ex) {
            return DatabaseReconciliationResult.stillUnknown(unresolvedOperation, ex);
        }

        DatabaseWriteResult result = safeReconcile(
                backend,
                table,
                uniqueId,
                unresolvedOperation
        );

        if (result.status() == DatabaseWriteResult.Status.UNKNOWN) {
            return DatabaseReconciliationResult.stillUnknown(unresolvedOperation, result.cause());
        }

        unresolvedWrites.remove(key, unresolvedOperation);
        return DatabaseReconciliationResult.reconciled(result);
    }

    private DatabaseWriteResult safeReconcile(
            DatabaseBackend backend,
            String table,
            String uniqueId,
            UUID operationId
    ) {
        try {
            return backend.reconcileTrackedWrite(table, uniqueId, operationId);
        } catch (RuntimeException ex) {
            return DatabaseWriteResult.unknown(operationId, ex);
        }
    }

    private <T> CompletableFuture<T> enqueue(
            WriteKey key,
            Plugin owner,
            DatabaseSettings settings,
            Supplier<T> task
    ) {
        WriteLane lane = writeLanes.computeIfAbsent(key, ignored -> new WriteLane());

        CompletableFuture<T> next;
        CompletableFuture<Void> newTail;

        synchronized (lane) {
            next = lane.tail.thenCompose(ignored -> submitRaw(owner, settings, task));
            newTail = next.handle((result, error) -> null);
            lane.tail = newTail;
        }

        CompletableFuture<Void> expectedTail = newTail;
        newTail.whenComplete((ignored, error) -> {
            synchronized (lane) {
                if (lane.tail == expectedTail && !unresolvedWrites.containsKey(key)) {
                    writeLanes.remove(key, lane);
                }
            }
        });

        return next;
    }

    private <T> CompletableFuture<T> submitRaw(
            Plugin owner,
            DatabaseSettings settings,
            Supplier<T> task
    ) {
        if (settings.useQueue()) {
            return queueFor(owner, settings.timeoutMillis()).submitRaw(task);
        }

        return CompletableFuture.supplyAsync(
                task,
                runnable -> Bukkit.getScheduler().runTaskAsynchronously(core, runnable)
        );
    }

    private static CompletableFuture<DatabaseWriteResult> writeTimeoutView(
            CompletableFuture<DatabaseWriteResult> completion,
            UUID operationId,
            long timeoutMillis
    ) {
        if (timeoutMillis <= 0) {
            return completion;
        }

        CompletableFuture<DatabaseWriteResult> callerView = new CompletableFuture<>();
        completion.whenComplete((result, error) -> {
            if (error != null) {
                callerView.completeExceptionally(error);
            } else {
                callerView.complete(result);
            }
        });

        CompletableFuture.delayedExecutor(timeoutMillis, TimeUnit.MILLISECONDS).execute(() ->
                callerView.complete(DatabaseWriteResult.unknown(
                        operationId,
                        new TimeoutException("Database write timed out after " + timeoutMillis + " ms")
                ))
        );

        return callerView;
    }

    private static Throwable unwrap(Throwable t) {
        if (t instanceof java.util.concurrent.CompletionException ce && ce.getCause() != null) return ce.getCause();
        if (t instanceof java.util.concurrent.ExecutionException ee && ee.getCause() != null) return ee.getCause();
        return t;
    }

    private static String fingerprint(Plugin owner, DatabaseSettings s) {
        String b = s.backend().toLowerCase();
        String base = owner.getName().toLowerCase() + "|" + b + "|" + s.poolSize() + "|" + s.tablePrefix();

        if (b.equals("mysql")) {
            return base + "|" + s.mysqlUrl() + "|" + s.mysqlUser();
        }
        return base;
    }

    private record WriteKey(String backendFingerprint, String table, String uniqueId) {}

    private static final class WriteLane {
        private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
    }
}
