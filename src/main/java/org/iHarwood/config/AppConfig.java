package org.iHarwood.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shared infrastructure beans optimised for Spring Boot 4.1 + Java 21.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties({AppProperties.class, AwtrixProperties.class})
public class AppConfig {

    /**
     * Application-wide virtual-thread executor for fire-and-forget work
     * (manual refresh, history populate, Claude verification, Awtrix batching).
     * Lifecycle is managed by Spring — shut down on context close.
     */
    @Bean(destroyMethod = "close")
    public ExecutorService applicationTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Spring {@link AsyncTaskExecutor} facade over the virtual-thread pool
     * so {@code @Async} and MVC async support use virtual threads when enabled.
     */
    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor taskExecutor(ExecutorService applicationTaskExecutor) {
        return new TaskExecutorAdapter(applicationTaskExecutor);
    }
}

