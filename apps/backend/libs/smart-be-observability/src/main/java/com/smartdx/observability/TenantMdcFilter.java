package com.smartdx.observability;

import com.smartdx.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ログに tenant_id を自動付与する MDC フィルタ
 */
@Component
@Order(0) // TenantContextFilter より後に実行
public class TenantMdcFilter extends OncePerRequestFilter {

    private static final String MDC_TENANT_ID = "tenantId";
    private static final String MDC_REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // テナントID を MDC に設定
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                MDC.put(MDC_TENANT_ID, tenantId.toString());
            }

            // リクエストID を MDC に設定
            String requestId = request.getHeader("X-Request-Id");
            if (requestId == null) {
                requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
            }
            MDC.put(MDC_REQUEST_ID, requestId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}
