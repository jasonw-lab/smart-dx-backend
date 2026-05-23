package com.smartdx.property.support;

import cn.hutool.core.util.StrUtil;
import com.smartdx.security.model.UserDetails;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.property.model.req.PropertyLookupReq;

import java.util.Set;

public final class PropertySearchSupport {

    private static final Set<String> SCOPES = Set.of("published", "draft", "all");
    private static final Set<String> ORDER_BY = Set.of("asc", "desc");

    private PropertySearchSupport() {
    }

    public static void normalize(PropertyLookupReq req) {
        if (StrUtil.isBlank(req.getScope())) {
            req.setScope("published");
        }
        if (!SCOPES.contains(req.getScope())) {
            throw new IllegalArgumentException("Unsupported scope: " + req.getScope());
        }
        if (StrUtil.isBlank(req.getSortBy())) {
            req.setSortBy("listedDate");
        }
        if (StrUtil.isBlank(req.getOrderBy())) {
            req.setOrderBy("desc");
        }
        req.setOrderBy(req.getOrderBy().toLowerCase());
        if (!ORDER_BY.contains(req.getOrderBy())) {
            throw new IllegalArgumentException("Unsupported orderBy: " + req.getOrderBy());
        }
    }

    public static Long currentTenantId() {
        return SecurityUtils.getUser().map(UserDetails::getTenantId).orElse(null);
    }

    public static Long resolveUserId(String raw) {
        if ("me".equalsIgnoreCase(raw)) {
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                throw new IllegalArgumentException("Authenticated user is required");
            }
            return userId;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("registrantId must be numeric or me", e);
        }
    }
}
