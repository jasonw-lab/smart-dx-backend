package com.smartdx.retail.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Retail domain Flyway configuration (sample, disabled by default).
 * <p>
 * DB スキーマは {@code docs/db/smart_dx_db.sql} で直接管理する運用。
 * このクラスと V1 migration は Flyway 利用が必要になった将来のためにサンプルとして残してある。
 * </p>
 * <p>
 * Disabled by default. Enable with: smartdx.flyway.retail.enabled=true
 * </p>
 */
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(name = "smartdx.flyway.retail.enabled", havingValue = "true", matchIfMissing = false)
public class RetailFlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway retailFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/retail")
                .table("flyway_schema_history_retail")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load();
    }
}
