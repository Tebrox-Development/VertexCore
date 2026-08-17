package de.tebrox.vertexCore.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AsyncQueueTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void callerTimeoutDoesNotAdvanceQueueTailBeforeTaskActuallySettles() throws Exception {
        AsyncQueue queue = new AsyncQueue(executor, 50);
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicBoolean secondStarted = new AtomicBoolean(false);

        CompletableFuture<Void> firstCallerView = queue.submitVoid(() -> {
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

        CompletableFuture<Void> secondCompletion = queue.submitVoidRaw(() -> secondStarted.set(true));
        Thread.sleep(100);
        assertFalse(secondStarted.get(), "second task must remain behind the actual first task");

        releaseFirst.countDown();
        secondCompletion.get(1, TimeUnit.SECONDS);
        assertTrue(secondStarted.get());
    }
}
