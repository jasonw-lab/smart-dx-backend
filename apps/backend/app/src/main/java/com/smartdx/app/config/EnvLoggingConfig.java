package com.smartdx.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Logs environment configuration at application startup.
 * Sensitive values (passwords, secrets, keys) are masked.
 */
@Component
public class EnvLoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(EnvLoggingConfig.class);

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "PASSWORD", "SECRET", "KEY", "TOKEN", "CREDENTIAL"
    );

    private final Environment env;

    public EnvLoggingConfig(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logEnvironmentVariables() {
        Map<String, String> envVars = new LinkedHashMap<>();

        // Server
        envVars.put("SERVER_PORT", env.getProperty("server.port", "8080"));

        // Database
        envVars.put("MYSQL_HOST", env.getProperty("MYSQL_HOST", "localhost"));
        envVars.put("MYSQL_PORT", env.getProperty("MYSQL_PORT", "3306"));
        envVars.put("MYSQL_USER", env.getProperty("MYSQL_USER", "root"));
        envVars.put("MYSQL_PASSWORD", env.getProperty("MYSQL_PASSWORD", ""));

        // Redis
        envVars.put("REDIS_HOST", env.getProperty("REDIS_HOST", "localhost"));
        envVars.put("REDIS_PORT", env.getProperty("REDIS_PORT", "6379"));
        envVars.put("REDIS_PASSWORD", env.getProperty("REDIS_PASSWORD", ""));

        // MinIO
        envVars.put("MINIO_ENDPOINT", env.getProperty("MINIO_ENDPOINT", ""));
        envVars.put("MINIO_ACCESS_KEY", env.getProperty("MINIO_ACCESS_KEY", ""));
        envVars.put("MINIO_SECRET_KEY", env.getProperty("MINIO_SECRET_KEY", ""));

        // OpenSearch
        envVars.put("OPENSEARCH_HOST", env.getProperty("OPENSEARCH_HOST", "localhost"));
        envVars.put("OPENSEARCH_PORT", env.getProperty("OPENSEARCH_PORT", "9200"));

        // JWT
        envVars.put("JWT_SECRET_KEY", env.getProperty("JWT_SECRET_KEY", ""));

        // Spring Profile
        envVars.put("SPRING_PROFILES_ACTIVE", env.getProperty("spring.profiles.active", "default"));

        StringBuilder sb = new StringBuilder();
        sb.append("\n========== Environment Configuration ==========\n");
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(String.format("  %-25s : %s%n", key, maskIfSensitive(key, value)));
        }
        sb.append("================================================");

        log.info(sb.toString());
    }

    private String maskIfSensitive(String key, String value) {
        if (value == null || value.isEmpty()) {
            return "(not set)";
        }
        for (String sensitive : SENSITIVE_KEYS) {
            if (key.toUpperCase().contains(sensitive)) {
                return "***";
            }
        }
        return value;
    }
}
