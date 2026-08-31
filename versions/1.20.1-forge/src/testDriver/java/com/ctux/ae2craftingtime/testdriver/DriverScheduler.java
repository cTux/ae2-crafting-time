package com.ctux.ae2craftingtime.testdriver;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class DriverScheduler {
    private final Semaphore capacity;
    private final Duration timeout;

    public DriverScheduler(int capacity, Duration timeout) {
        this.capacity = new Semaphore(capacity);
        this.timeout = timeout;
    }

    public <T> T call(Executor executor, Callable<T> action) throws Exception {
        if (!capacity.tryAcquire()) {
            throw new IllegalStateException("driver queue is full");
        }
        try {
            var future = new CompletableFuture<T>();
            executor.execute(() -> {
                try {
                    future.complete(action.call());
                } catch (Throwable error) {
                    future.completeExceptionally(error);
                } finally {
                    capacity.release();
                }
            });
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RuntimeException error) {
            capacity.release();
            throw error;
        }
    }
}
