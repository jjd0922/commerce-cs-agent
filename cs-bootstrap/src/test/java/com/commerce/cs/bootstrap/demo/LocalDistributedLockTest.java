package com.commerce.cs.bootstrap.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalDistributedLock timeout 처리")
class LocalDistributedLockTest {

    @Test
    @DisplayName("같은 key의 lock을 timeout 안에 얻지 못하면 실패한다")
    void fails_when_lock_is_not_acquired_within_timeout() throws Exception {
        LocalDistributedLock lock = new LocalDistributedLock();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<String> held = executor.submit(() -> lock.withLock("tool:request_return:order-1", Duration.ofSeconds(1), () -> {
                locked.countDown();
                await(release);
                return "held";
            }));
            assertThat(locked.await(1, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> lock.withLock("tool:request_return:order-1", Duration.ofMillis(50), () -> "missed"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not acquire lock within timeout");

            release.countDown();
            assertThat(held.get(1, TimeUnit.SECONDS)).isEqualTo("held");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
