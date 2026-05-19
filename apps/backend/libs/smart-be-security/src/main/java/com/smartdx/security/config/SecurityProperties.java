package com.smartdx.security.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * セキュリティ設定プロパティ
 */
@Data
@Validated
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private SessionConfig session;

    @NotEmpty
    private String[] ignoreUrls;

    @NotEmpty
    private String[] unsecuredUrls;

    @Data
    public static class SessionConfig {
        @NotNull
        private String type;

        @Min(-1)
        private Integer accessTokenTimeToLive = 3600;

        @Min(-1)
        private Integer refreshTokenTimeToLive = 604800;

        private JwtConfig jwt;

        private RedisTokenConfig redisToken;
    }

    @Data
    public static class JwtConfig {
        @NotNull
        private String secretKey;
    }

    @Data
    public static class RedisTokenConfig {
        private Boolean allowMultiLogin = true;
    }
}
