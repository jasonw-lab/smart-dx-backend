package com.smartdx.tenant.resolver;

import com.smartdx.security.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SecurityContext (JWT) からテナントIDを解決
 * <p>
 * JWTトークンのclaimsに含まれるtenant_idを取得。
 * TokenAuthenticationFilterの後に実行されることを前提とする。
 * </p>
 *
 * @author jason.w
 */
@Component
public class SecurityContextTenantResolver implements TenantResolver {

    private static final Logger log = LoggerFactory.getLogger(SecurityContextTenantResolver.class);

    @Override
    public Long resolve(HttpServletRequest request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId != null) {
            log.debug("Resolved tenant ID from SecurityContext (JWT): {}", tenantId);
            return tenantId;
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 100; // HeaderTenantResolverより優先
    }
}
