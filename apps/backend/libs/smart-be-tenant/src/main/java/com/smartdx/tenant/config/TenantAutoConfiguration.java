package com.smartdx.tenant.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.smartdx.tenant.TenantProperties;
import com.smartdx.tenant.aspect.TenantAspect;
import com.smartdx.tenant.filter.TenantContextFilter;
import com.smartdx.tenant.filter.TenantStatusFilter;
import com.smartdx.tenant.mybatis.TenantLineHandler;
import com.smartdx.tenant.resolver.HeaderTenantResolver;
import com.smartdx.tenant.resolver.SecurityContextTenantResolver;
import com.smartdx.tenant.resolver.TenantResolver;
import com.smartdx.tenant.service.TenantStatusChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * マルチテナント自動設定
 * <p>
 * smart-be-tenant.enabled=true (デフォルト) で有効化
 * </p>
 */
@Configuration
@EnableConfigurationProperties(TenantProperties.class)
@ConditionalOnProperty(prefix = "tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TenantAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TenantAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public TenantLineHandler tenantLineHandler(TenantProperties tenantProperties) {
        log.info("Creating TenantLineHandler with column: {}, forceDefault: {}",
                tenantProperties.getColumn(), tenantProperties.isForceDefault());
        return new TenantLineHandler(tenantProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantAspect tenantAspect() {
        return new TenantAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    public HeaderTenantResolver headerTenantResolver() {
        return new HeaderTenantResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.smartdx.security.util.SecurityUtils")
    public SecurityContextTenantResolver securityContextTenantResolver() {
        log.info("Registered SecurityContextTenantResolver for JWT tenant resolution");
        return new SecurityContextTenantResolver();
    }

    /**
     * テナント状態チェックフィルター
     * <p>
     * TenantStatusCheckerが提供されている場合のみ有効。
     * force-defaultモード時はチェックをスキップ。
     * </p>
     */
    @Bean
    public FilterRegistrationBean<TenantStatusFilter> tenantStatusFilter(
            ObjectProvider<TenantStatusChecker> statusCheckerProvider,
            TenantProperties tenantProperties) {
        TenantStatusChecker statusChecker = statusCheckerProvider.getIfAvailable();
        TenantStatusFilter filter = new TenantStatusFilter(statusChecker, tenantProperties);

        FilterRegistrationBean<TenantStatusFilter> registration = new FilterRegistrationBean<>(filter);
        // SecurityFilter の後に実行 (JWT → SecurityContext → tenant 解決の順)
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 20);
        registration.addUrlPatterns("/*");
        log.info("Registered TenantStatusFilter (statusChecker: {})", statusChecker != null ? "available" : "not available");
        return registration;
    }

    /**
     * テナントコンテキストフィルター
     */
    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter(
            ObjectProvider<List<TenantResolver>> resolversProvider,
            TenantProperties tenantProperties) {
        List<TenantResolver> resolvers = resolversProvider.getIfAvailable(List::of);
        TenantContextFilter filter = new TenantContextFilter(resolvers, tenantProperties);

        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>(filter);
        // SecurityFilter の後に実行 (SecurityContextTenantResolver が JWT 由来の tenantId を解決できるように)
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 10);
        registration.addUrlPatterns("/*");
        log.info("Registered TenantContextFilter with {} resolvers", resolvers.size());
        return registration;
    }

    /**
     * MyBatis-Plus TenantLineInnerInterceptor を既存の MybatisPlusInterceptor に追加するカスタマイザ
     */
    @Bean
    @ConditionalOnClass(MybatisPlusInterceptor.class)
    public TenantInterceptorCustomizer tenantInterceptorCustomizer(
            TenantLineHandler tenantLineHandler,
            ObjectProvider<MybatisPlusInterceptor> interceptorProvider) {
        return new TenantInterceptorCustomizer(tenantLineHandler, interceptorProvider);
    }

    /**
     * MybatisPlusInterceptor がない場合のデフォルト作成
     */
    @Bean
    @ConditionalOnClass(MybatisPlusInterceptor.class)
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantLineHandler tenantLineHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
        log.info("Created MybatisPlusInterceptor with TenantLineInnerInterceptor");
        return interceptor;
    }
}
