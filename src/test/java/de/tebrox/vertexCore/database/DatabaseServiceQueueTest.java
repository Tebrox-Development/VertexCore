package de.tebrox.vertexCore.database;

import de.tebrox.vertexCore.util.AsyncQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseServiceQueueTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void sameOwnerCanUseDifferentTimeoutsWithoutSplittingOrdering() throws Exception {
        Map<String, AsyncQueue> queues = new ConcurrentHashMap<>();
        String ownerKey = "same-owner";

        AsyncQueue shortTimeoutQueue = DatabaseService.queueForOwner(queues, ownerKey, executor, 50);
        AsyncQueue longTimeoutQueue = DatabaseService.queueForOwner(queues, ownerKey, executor, 500);

        assertEquals(1, queues.size(), "same owner must keep one shared ordering queue");

        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicBoolean secondStarted = new AtomicBoolean(false);

        CompletableFuture<Void> firstCallerView = shortTimeoutQueue.submitVoid(() -> {
            firstStarted.countDown();
            try {
                releaseFirst.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });

        assertTrue(firstStarted.await(1, TimeUnit.SECONDS));
        ExecutionException timeout = assertThrows(
                ExecutionException.class,
                () -> firstCallerView.get(1, TimeUnit.SECONDS)
        );
        assertInstanceOf(TimeoutException.class, timeout.getCause());

        CompletableFuture<Void> secondCallerView = longTimeoutQueue.submitVoid(() -> secondStarted.set(true));
        Thread.sleep(100);

        assertFalse(secondStarted.get(), "same-owner operations must remain ordered across timeout views");
        assertFalse(secondCallerView.isDone(), "the first database timeout must not stick to later databases");

        releaseFirst.countDown();
        secondCallerView.get(1, TimeUnit.SECONDS);
        assertTrue(secondStarted.get());
    }
}
