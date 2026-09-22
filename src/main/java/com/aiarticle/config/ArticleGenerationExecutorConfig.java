package com.aiarticle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 文章生成线程池配置。
 * <p>
 * 固定最多并行生成 5 篇文章，不设置等待队列；满载时立即拒绝新任务。
 */
@Configuration
public class ArticleGenerationExecutorConfig {

    public static final String EXECUTOR_NAME = "articleGenerationExecutor";

    @Bean(name = EXECUTOR_NAME)
    public ThreadPoolTaskExecutor articleGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("article-generation-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
