package com.smartdx.tenant.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.smartdx.tenant.TenantProperties;
import com.smartdx.tenant.aspect.TenantAspect;
import com.smartdx.tenant.mybatis.TenantLineHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * マルチテナント自動設定
 * <p>
 * smart-be-tenant.enabled=true (デフォルト) で有効化
 * </p>
 */
@Configuration
@EnableConfigurationProperties(TenantProperties.class)
@ConditionalOnProperty(prefix = "smart-be-tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
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

    /**
     * MyBatis-Plus インターセプターにテナントハンドラを追加
     * <p>
     * 既存の MybatisPlusInterceptor がない場合のみ作成。
     * 既存がある場合は、アプリ側で TenantLineHandler を追加する必要がある。
     * </p>
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnClass(MybatisPlusInterceptor.class)
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantLineHandler tenantLineHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
        log.info("Created MybatisPlusInterceptor with TenantLineInnerInterceptor");
        return interceptor;
    }
}
