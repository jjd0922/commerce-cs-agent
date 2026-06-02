package com.commerce.cs.application.lock;

import java.time.Duration;
import java.util.function.Supplier;

public interface DistributedLock {

    <T> T withLock(String key, Duration timeout, Supplier<T> action);
}
