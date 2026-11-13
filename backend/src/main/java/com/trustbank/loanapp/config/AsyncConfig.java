package com.trustbank.loanapp.config;

import java.time.Clock;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Delayed concurrency (Comer ch. 3): fast work is finished on the Tomcat request thread; slow work
 * (credit bureau call, notifications, audit logging) is handed to this bounded worker pool so the
 * HTTP reply is not held back.
 *
 * <p>The pool and its queue are both bounded. When they are full, {@code CallerRunsPolicy} makes the
 * submitting thread do the work itself: natural back-pressure instead of an unbounded queue.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    public static final String BACKGROUND_EXECUTOR = "backgroundExecutor";

    @Bean(name = BACKGROUND_EXECUTOR)
    public Executor backgroundExecutor(AppProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.async().corePoolSize());
        executor.setMaxPoolSize(props.async().maxPoolSize());
        executor.setQueueCapacity(props.async().queueCapacity());
        executor.setThreadNamePrefix("loan-bg-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }

    /** One clock for every deadline check, so tests can pin time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
