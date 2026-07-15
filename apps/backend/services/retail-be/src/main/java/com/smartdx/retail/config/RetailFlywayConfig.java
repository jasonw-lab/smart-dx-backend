package com.smartdx.retail.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Retail domain Flyway configuration.
 * <p>
 * retail_* テーブルの DDL 正本は {@code db/migration/retail/} の migration。
 * 統合アプリ (smart-dx-app) では {@code smartdx.flyway.retail.enabled=true} が既定で、
 * 起動時に単一 datasource (smart_dx_db) へ retail スキーマを適用する。
 * retail-be 単体起動 (DEMO) では retail_db に対して spring.flyway (標準自動設定) を使用する。
 * </p>
 * <p>
 * Enable/disable with: smartdx.flyway.retail.enabled
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
