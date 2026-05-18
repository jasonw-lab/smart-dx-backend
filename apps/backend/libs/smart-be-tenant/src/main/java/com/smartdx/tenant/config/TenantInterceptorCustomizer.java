package com.smartdx.tenant.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.smartdx.tenant.mybatis.TenantLineHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 既存の MybatisPlusInterceptor に TenantLineInnerInterceptor を追加するカスタマイザ
 * <p>
 * アプリ側で独自の MybatisPlusInterceptor を定義している場合でも、
 * tenant interceptor が確実に登録されるようにする。
 * </p>
 */
@Component
public class TenantInterceptorCustomizer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptorCustomizer.class);

    private final TenantLineHandler tenantLineHandler;
    private final ObjectProvider<MybatisPlusInterceptor> interceptorProvider;

    public TenantInterceptorCustomizer(TenantLineHandler tenantLineHandler) {
        this.tenantLineHandler = tenantLineHandler;
        this.interceptorProvider = null;
    }

    public TenantInterceptorCustomizer(TenantLineHandler tenantLineHandler,
                                       ObjectProvider<MybatisPlusInterceptor> interceptorProvider) {
        this.tenantLineHandler = tenantLineHandler;
        this.interceptorProvider = interceptorProvider;
    }

    @Override
    public void afterPropertiesSet() {
        if (interceptorProvider == null) {
            return;
        }

        MybatisPlusInterceptor interceptor = interceptorProvider.getIfAvailable();
        if (interceptor == null) {
            return;
        }

        // 既に TenantLineInnerInterceptor が登録されているか確認
        List<InnerInterceptor> innerInterceptors = interceptor.getInterceptors();
        boolean hasTenantInterceptor = innerInterceptors.stream()
                .anyMatch(i -> i instanceof TenantLineInnerInterceptor);

        if (!hasTenantInterceptor) {
            // 先頭に追加 (tenant filter は最初に適用されるべき)
            TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(tenantLineHandler);
            innerInterceptors.add(0, tenantInterceptor);
            log.info("Added TenantLineInnerInterceptor to existing MybatisPlusInterceptor");
        } else {
            log.debug("TenantLineInnerInterceptor already registered");
        }
    }
}
