package com.smartdx.app.config;

import cn.hutool.core.util.ArrayUtil;
import com.smartdx.security.config.SecurityProperties;
import com.smartdx.security.filter.TokenAuthenticationFilter;
import com.smartdx.security.handler.AccessDeniedExceptionHandler;
import com.smartdx.security.handler.AuthenticationEntryPointHandler;
import com.smartdx.security.token.TokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Unified Security Configuration for Modular Monolith
 * <p>
 * Consolidates security settings from all domain modules into a single
 * SecurityFilterChain. This prevents bean conflicts that would arise
 * from multiple domain-specific SecurityConfig classes.
 * </p>
 *
 * @see <a href="docs/adr/006-modular-monolith-deployment.md">ADR-006</a>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class UnifiedSecurityConfig {

    private final TokenManager tokenManager;
    private final SecurityProperties securityProperties;

    @Bean
    @Primary
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> {
                    String[] ignoreUrls = securityProperties.getIgnoreUrls();
                    if (ArrayUtil.isNotEmpty(ignoreUrls)) {
                        auth.requestMatchers(ignoreUrls).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(configurer ->
                        configurer
                                .authenticationEntryPoint(new AuthenticationEntryPointHandler())
                                .accessDeniedHandler(new AccessDeniedExceptionHandler())
                )
                .sessionManagement(configurer ->
                        configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .addFilterBefore(new TokenAuthenticationFilter(tokenManager), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Primary
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> {
            String[] unsecuredUrls = securityProperties.getUnsecuredUrls();
            if (ArrayUtil.isNotEmpty(unsecuredUrls)) {
                web.ignoring().requestMatchers(unsecuredUrls);
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
