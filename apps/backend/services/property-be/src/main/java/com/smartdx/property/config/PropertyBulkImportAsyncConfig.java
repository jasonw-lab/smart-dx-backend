package com.smartdx.property.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class PropertyBulkImportAsyncConfig {

    /**
     * 一括インポート用 Executor。
     * <p>
     * TenantContextHolder (TransmittableThreadLocal) をプールスレッドへ正しく伝搬させるため、
     * 必ず {@link TtlExecutors#getTtlExecutor(Executor)} でラップする。
     * ラップしない場合、プールスレッドには生成時のコンテキストが残留し、
     * 別テナントとして DB アクセスする恐れがある。
     * </p>
     */
    @Bean("propertyBulkImportExecutor")
    public Executor propertyBulkImportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("property-bulk-import-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return TtlExecutors.getTtlExecutor(executor);
    }
}
