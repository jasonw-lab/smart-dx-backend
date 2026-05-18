package com.smartdx.tenant.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * X-Tenant-Id ヘッダーからテナントを解決
 */
@Component
public class HeaderTenantResolver implements TenantResolver {

    private static final Logger log = LoggerFactory.getLogger(HeaderTenantResolver.class);
    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public Long resolve(HttpServletRequest request) {
        String tenantIdStr = request.getHeader(TENANT_HEADER);
        if (StringUtils.hasText(tenantIdStr)) {
            try {
                Long tenantId = Long.parseLong(tenantIdStr);
                log.debug("Resolved tenant ID from header {}: {}", TENANT_HEADER, tenantId);
                return tenantId;
            } catch (NumberFormatException e) {
                log.warn("Invalid tenant ID in header {}: {}", TENANT_HEADER, tenantIdStr);
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 200; // JWT の後
    }
}
