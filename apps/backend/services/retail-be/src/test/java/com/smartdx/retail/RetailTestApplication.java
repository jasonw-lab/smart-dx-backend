package com.smartdx.retail;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.data.redis.RedisReactiveHealthContributorAutoConfiguration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;

/**
 * Retail domain E2E test bootstrap.
 * <p>
 * Scans the retail domain and imports library auto-configurations
 * (core, tenant, security, redis, observability) via their
 * {@code META-INF/spring/...AutoConfiguration.imports} entries.
 * </p>
 */
@SpringBootApplication(
        scanBasePackages = "com.smartdx.retail",
        exclude = {
                FlywayAutoConfiguration.class,
                RedisReactiveAutoConfiguration.class,
                RedisReactiveHealthContributorAutoConfiguration.class,
                RedissonAutoConfigurationV2.class
        }
)
@MapperScan(
        basePackages = "com.smartdx.retail.mapper",
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class RetailTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetailTestApplication.class, args);
    }
}
