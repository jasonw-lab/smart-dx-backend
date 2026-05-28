package com.smartdx.tenant.filter;

import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.tenant.TenantProperties;
import com.smartdx.tenant.service.TenantStatusChecker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * テナント状態チェックフィルター
 * <p>
 * テナントが無効（SUSPENDED/CANCELLED）の場合、アクセスを拒否する。
 * TenantStatusCheckerが提供されていない場合は、チェックをスキップする。
 * </p>
 *
 * @author jason.w
 */
public class TenantStatusFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantStatusFilter.class);

    private final TenantStatusChecker statusChecker;
    private final TenantProperties properties;

    public TenantStatusFilter(TenantStatusChecker statusChecker, TenantProperties properties) {
        this.statusChecker = statusChecker;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // force-default モードの場合はチェックをスキップ（単一テナント運用）
        if (properties.isForceDefault()) {
            filterChain.doFilter(request, response);
            return;
        }

        // TenantStatusCheckerが未設定の場合はスキップ
        if (statusChecker == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // テナント状態チェック
        if (!statusChecker.isActive(tenantId)) {
            log.warn("Access denied: Tenant {} is not active", tenantId);
            sendForbiddenResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"TENANT_INACTIVE\",\"message\":\"テナントが無効です\"}");
    }
}
