package com.smartdx.system.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * System domain Flyway configuration (sample, disabled by default).
 * <p>
 * DB スキーマは {@code docs/db/smart_dx_db.sql} で直接管理する運用。
 * このクラスは Flyway 移行が必要になった将来のためのサンプルとして残してある。
 * </p>
 * <p>
 * Disabled by default. Enable with: smartdx.flyway.system.enabled=true
 * </p>
 */
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(name = "smartdx.flyway.system.enabled", havingValue = "true", matchIfMissing = false)
public class SystemFlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway systemFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/system")
                .table("flyway_schema_history_system")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load();
    }
}
