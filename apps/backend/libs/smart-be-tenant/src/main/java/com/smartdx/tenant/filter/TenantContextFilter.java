package com.smartdx.tenant.filter;

import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.tenant.TenantProperties;
import com.smartdx.tenant.resolver.TenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * テナントコンテキストフィルター
 * <p>
 * リクエストからテナントIDを解決し、TenantContextHolderに設定。
 * リクエスト終了時に必ずクリア。
 * </p>
 */
@Order(10)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    private final List<TenantResolver> resolvers;
    private final TenantProperties properties;

    public TenantContextFilter(List<TenantResolver> resolvers, TenantProperties properties) {
        this.resolvers = resolvers != null
                ? resolvers.stream().sorted(Comparator.comparingInt(TenantResolver::getOrder)).toList()
                : List.of();
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Long tenantId = resolveTenantId(request);
            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
                log.debug("TenantContextFilter set tenantId: {}", tenantId);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private Long resolveTenantId(HttpServletRequest request) {
        // force-default モードの場合、常にデフォルトテナント
        if (properties.isForceDefault()) {
            Long defaultId = properties.getDefaultTenantId();
            log.debug("Force default mode: using tenant ID {}", defaultId);
            return defaultId;
        }

        // 各 Resolver を順に試行
        for (TenantResolver resolver : resolvers) {
            Long tenantId = resolver.resolve(request);
            if (tenantId != null) {
                return tenantId;
            }
        }

        // デフォルトにフォールバック (設定されている場合のみ)
        Long defaultId = properties.getDefaultTenantId();
        if (defaultId != null) {
            log.debug("No resolver found tenant, falling back to default: {}", defaultId);
            return defaultId;
        }

        log.warn("Could not resolve tenant ID from request");
        return null;
    }
}
