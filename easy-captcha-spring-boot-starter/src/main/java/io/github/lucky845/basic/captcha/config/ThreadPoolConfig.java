package io.github.lucky845.basic.captcha.config;


import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author created by lucky845 on 2025-01-17
 */
public class ThreadPoolConfig {

    private static final String CAPTCHA_THREAD_POOL_NAME = "captcha-thread-pool-";

    @Bean
    public ThreadFactory virtualThreadFactory() {
        ThreadFactory factory = Thread.ofVirtual()
                .name("virtual-thread-pool-", 0)
                .factory();
        return TtlExecutors.getDisableInheritableThreadFactory(factory);
    }

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix(CAPTCHA_THREAD_POOL_NAME);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadFactory(virtualThreadFactory());
        return executor;
    }

}
