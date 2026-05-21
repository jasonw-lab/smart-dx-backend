package com.smartdx.security.config;

import com.smartdx.security.token.JwtTokenManager;
import com.smartdx.security.token.RedisTokenManager;
import com.smartdx.security.token.TokenManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * セキュリティ自動設定
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAutoConfiguration {

    /**
     * JWT Token Manager (default)
     */
    @Bean
    @ConditionalOnProperty(value = "security.session.type", havingValue = "jwt", matchIfMissing = true)
    @ConditionalOnMissingBean(TokenManager.class)
    public TokenManager jwtTokenManager(SecurityProperties securityProperties,
                                        RedisTemplate<String, Object> redisTemplate) {
        return new JwtTokenManager(securityProperties, redisTemplate);
    }

    /**
     * Redis Token Manager
     */
    @Bean
    @ConditionalOnProperty(value = "security.session.type", havingValue = "redis-token")
    @ConditionalOnMissingBean(TokenManager.class)
    public TokenManager redisTokenManager(SecurityProperties securityProperties,
                                          RedisTemplate<String, Object> redisTemplate) {
        return new RedisTokenManager(securityProperties, redisTemplate);
    }

    /**
     * Password Encoder
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
