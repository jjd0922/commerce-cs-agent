package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.lock.DistributedLock;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
@Profile({"demo", "local"})
public class LocalDistributedLock implements DistributedLock {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T withLock(String key, Duration timeout, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        boolean acquired = acquire(lock, key, timeout);
        try {
            return action.get();
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }

    private boolean acquire(ReentrantLock lock, String key, Duration timeout) {
        try {
            if (lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring lock: " + key, e);
        }
        throw new IllegalStateException("Could not acquire lock within timeout: " + key);
    }
}
