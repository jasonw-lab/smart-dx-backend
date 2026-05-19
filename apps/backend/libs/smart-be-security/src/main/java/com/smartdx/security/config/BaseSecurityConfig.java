package com.smartdx.security.config;

import cn.hutool.core.util.ArrayUtil;
import com.smartdx.security.filter.TokenAuthenticationFilter;
import com.smartdx.security.handler.AccessDeniedExceptionHandler;
import com.smartdx.security.handler.AuthenticationEntryPointHandler;
import com.smartdx.security.token.TokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 基本セキュリティ設定
 */
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public abstract class BaseSecurityConfig {

    protected final TokenManager tokenManager;
    protected final SecurityProperties securityProperties;

    @Bean
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

        configureAdditionalFilters(http);

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> {
            String[] unsecuredUrls = securityProperties.getUnsecuredUrls();
            if (ArrayUtil.isNotEmpty(unsecuredUrls)) {
                web.ignoring().requestMatchers(unsecuredUrls);
            }
        };
    }

    /**
     * サブクラスで追加フィルターを設定するためのフックメソッド
     */
    protected void configureAdditionalFilters(HttpSecurity http) throws Exception {
        // サブクラスでオーバーライド可能
    }
}
