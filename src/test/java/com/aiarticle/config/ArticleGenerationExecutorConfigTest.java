package com.aiarticle.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleGenerationExecutorConfigTest {

    @Test
    void rejectsSixthConcurrentTaskWithoutQueueing() throws Exception {
        ThreadPoolTaskExecutor executor = new ArticleGenerationExecutorConfig()
                .articleGenerationExecutor();
        CountDownLatch running = new CountDownLatch(5);
        CountDownLatch release = new CountDownLatch(1);
        try {
            for (int i = 0; i < 5; i++) {
                executor.execute(() -> {
                    running.countDown();
                    await(release);
                });
            }

            assertTrue(running.await(2, TimeUnit.SECONDS));
            assertThrows(TaskRejectedException.class, () -> executor.execute(() -> {
            }));
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
