package com.smartdx.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.data.redis.RedisReactiveHealthContributorAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;

/**
 * Smart DX Unified Application
 * <p>
 * Modular Monolith Bootstrap - combines all domain modules into single deployment.
 * Each domain module is auto-configured via Spring Boot AutoConfiguration:
 * - PropertyAutoConfiguration (property-be)
 * - SystemAutoConfiguration (system-be)
 * - RetailAutoConfiguration (retail-be)
 * </p>
 * <p>
 * Package com.smartdx.app is intentionally separate from domain packages to ensure
 * component scanning is limited to bootstrap code. Domain components are loaded
 * via AutoConfiguration.imports mechanism.
 * </p>
 * <p>
 * FlywayAutoConfiguration is excluded because DB schema is managed manually
 * via {@code docs/db/smart_dx_db.sql} (see CLAUDE.md "DB スキーマ管理").
 * 各ドメインの FlywayConfig はサンプルとして残してあり、既定では無効。
 * 有効化は {@code smartdx.flyway.{property|system|retail}.enabled=true} で行う。
 * </p>
 *
 * @see <a href="docs/adr/006-modular-monolith-deployment.md">ADR-006</a>
 */
@SpringBootApplication(
        scanBasePackages = "com.smartdx.app",
        exclude = {
                FlywayAutoConfiguration.class,
                RedisReactiveAutoConfiguration.class,
                RedisReactiveHealthContributorAutoConfiguration.class,
                RedissonAutoConfigurationV2.class
        }
)
@MapperScan({
        "com.smartdx.property.mapper",
        "com.smartdx.system.mapper",
        "com.smartdx.retail.mapper"
})
@EnableCaching
public class SmartDxApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartDxApplication.class, args);
    }
}
