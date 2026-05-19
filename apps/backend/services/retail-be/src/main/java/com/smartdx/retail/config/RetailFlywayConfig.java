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
 * Uses separate migration location and history table to avoid conflicts
 * with other domains in the modular monolith deployment.
 * </p>
 * <p>
 * Enabled by default. Disable with: smartdx.flyway.retail.enabled=false
 * </p>
 */
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(name = "smartdx.flyway.retail.enabled", havingValue = "true", matchIfMissing = true)
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
