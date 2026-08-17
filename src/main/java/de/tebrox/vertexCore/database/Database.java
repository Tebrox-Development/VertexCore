package de.tebrox.vertexCore.database;

import de.tebrox.vertexCore.VertexCoreApi;
import de.tebrox.vertexCore.database.internal.TableNamer;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Database<T extends DataObject> implements AutoCloseable {

    private final Plugin owner;
    private final DatabaseSettings settings;
    private final Class<T> type;
    private final String table;

    public Database(Plugin owner, DatabaseSettings settings, Class<T> type) {
        this.owner = owner;
        this.settings = settings;
        this.type = type;
        this.table = TableNamer.tableName(settings.tablePrefix(), type);
    }

    /**
     * Blocking compatibility API. The write still goes through the tracked
     * per-key lane and waits for the actual write/reconciliation task rather
     * than a caller timeout view.
     */
    public void saveObject(T obj) {
        DatabaseWriteResult result = saveObjectTrackedAsync(obj).completion().join();
        if (result.status() != DatabaseWriteResult.Status.COMMITTED) {
            throw new DatabaseWriteException(result);
        }
    }

    public T loadObject(String uniqueId) {
        String json = VertexCoreApi.get().backendFor(owner, settings).get(table, uniqueId);
        if (json == null) return null;

        T obj = VertexCoreApi.get().json().fromJson(type, json);
        obj.setUniqueId(uniqueId);
        return obj;
    }

    public boolean objectExists(String uniqueId) {
        return VertexCoreApi.get().backendFor(owner, settings).exists(table, uniqueId);
    }

    public void deleteObject(String uniqueId) {
        VertexCoreApi.get().backendFor(owner, settings).delete(table, uniqueId);
    }

    public List<T> loadObjects() {
        List<String[]> rows = VertexCoreApi.get().backendFor(owner, settings).loadAllRaw(table);
        List<T> out = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            String id = row[0];
            String json = row[1];
            T obj = VertexCoreApi.get().json().fromJson(type, json);
            obj.setUniqueId(id);
            out.add(obj);
        }
        return out;
    }

    public DatabaseWriteOperation saveObjectTrackedAsync(T obj) {
        UUID operationId = UUID.randomUUID();

        final String json;
        try {
            json = VertexCoreApi.get().json().toJson(type, obj);
        } catch (RuntimeException ex) {
            return DatabaseWriteOperation.completed(
                    DatabaseWriteResult.notCommitted(operationId, ex)
            );
        }

        return VertexCoreApi.get().databaseService().submitTrackedWrite(
                owner,
                settings,
                table,
                obj.getUniqueId(),
                json,
                operationId
        );
    }

    public CompletableFuture<DatabaseReconciliationResult> reconcileObjectAsync(String uniqueId) {
        return VertexCoreApi.get().databaseService().reconcileTrackedWrite(
                owner,
                settings,
                table,
                uniqueId
        );
    }

    public CompletableFuture<T> loadObjectAsync(String uniqueId) {
        if (settings.useQueue()) {
            return VertexCoreApi.get().databaseService()
                    .queueFor(owner, settings.timeoutMillis())
                    .submit(() -> loadObject(uniqueId));
        }
        return timeoutView(
                CompletableFuture.supplyAsync(() -> loadObject(uniqueId), VertexCoreApi.get().asyncExecutor())
        );
    }

    public CompletableFuture<Void> saveObjectAsync(T obj) {
        return saveObjectTrackedAsync(obj).result().thenCompose(result -> {
            if (result.status() == DatabaseWriteResult.Status.COMMITTED) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.failedFuture(new DatabaseWriteException(result));
        });
    }

    public CompletableFuture<List<T>> loadObjectsAsync() {
        if (settings.useQueue()) {
            return VertexCoreApi.get().databaseService()
                    .queueFor(owner, settings.timeoutMillis())
                    .submit(this::loadObjects);
        }
        return timeoutView(
                CompletableFuture.supplyAsync(this::loadObjects, VertexCoreApi.get().asyncExecutor())
        );
    }

    public CompletableFuture<Void> deleteObjectAsync(String uniqueId) {
        if (settings.useQueue()) {
            return VertexCoreApi.get().databaseService()
                    .queueFor(owner, settings.timeoutMillis())
                    .submitVoid(() -> deleteObject(uniqueId));
        }
        return timeoutView(
                CompletableFuture.runAsync(() -> deleteObject(uniqueId), VertexCoreApi.get().asyncExecutor())
        );
    }

    public void loadObjectAsyncMain(String uniqueId, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        loadObjectAsync(uniqueId).whenComplete((result, err) ->
                CompletableFuture.runAsync(() -> {
                    if (err != null) onError.accept(unwrap(err));
                    else onSuccess.accept(result);
                }, VertexCoreApi.get().mainExecutor())
        );
    }

    public void loadObjectsAsyncMain(Consumer<List<T>> onSuccess, Consumer<Throwable> onError) {
        loadObjectsAsync().whenComplete((result, err) ->
                CompletableFuture.runAsync(() -> {
                    if (err != null) onError.accept(unwrap(err));
                    else onSuccess.accept(result);
                }, VertexCoreApi.get().mainExecutor())
        );
    }

    public void saveObjectAsyncMain(T obj, Runnable onSuccess, Consumer<Throwable> onError) {
        saveObjectAsync(obj).whenComplete((v, err) ->
                CompletableFuture.runAsync(() -> {
                    if (err != null) onError.accept(unwrap(err));
                    else onSuccess.run();
                }, VertexCoreApi.get().mainExecutor())
        );
    }

    public <R> void supplyAsyncMain(CompletableFuture<R> future, BiConsumer<R, Throwable> callbackOnMain) {
        future.whenComplete((r, err) ->
                CompletableFuture.runAsync(() -> callbackOnMain.accept(r, err == null ? null : unwrap(err)),
                        VertexCoreApi.get().mainExecutor())
        );
    }

    private <R> CompletableFuture<R> timeoutView(CompletableFuture<R> completion) {
        if (settings.timeoutMillis() <= 0) {
            return completion;
        }

        CompletableFuture<R> callerView = new CompletableFuture<>();
        completion.whenComplete((result, error) -> {
            if (error != null) {
                callerView.completeExceptionally(error);
            } else {
                callerView.complete(result);
            }
        });

        CompletableFuture.delayedExecutor(settings.timeoutMillis(), TimeUnit.MILLISECONDS).execute(() ->
                callerView.completeExceptionally(new TimeoutException(
                        "Database operation timed out after " + settings.timeoutMillis() + " ms"
                ))
        );

        return callerView;
    }

    private static Throwable unwrap(Throwable t) {
        if (t instanceof java.util.concurrent.CompletionException ce && ce.getCause() != null) return ce.getCause();
        if (t instanceof java.util.concurrent.ExecutionException ee && ee.getCause() != null) return ee.getCause();
        return t;
    }

    @Override
    public void close() {
        VertexCoreApi.get().closeFor(owner);
    }
}
