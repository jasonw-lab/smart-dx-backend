package com.smartdx.system.tenant;

import com.smartdx.system.mapper.TenantMapper;
import com.smartdx.system.model.entity.Tenant;
import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.tenant.service.TenantStatusChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * sys_tenant をマスタとした {@link TenantStatusChecker} 実装。
 * <p>
 * tenant マスタの正本である system-be が status を判定する。
 * retail-be 等のドメインモジュールは libs 経由 (TenantStatusChecker bean) でのみ利用する。
 * </p>
 *
 * @author jason.w
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemTenantStatusChecker implements TenantStatusChecker {

    /** sys_tenant.status: 1 = enabled */
    private static final int STATUS_ENABLED = 1;

    private final TenantMapper tenantMapper;

    @Override
    @Cacheable(value = "tenantStatus", key = "#tenantId", unless = "#result == false")
    public boolean isActive(Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        boolean previous = TenantContextHolder.isIgnoreTenant();
        try {
            TenantContextHolder.setIgnoreTenant(true);
            Tenant tenant = tenantMapper.selectById(tenantId);
            return tenant != null
                    && tenant.getStatus() != null
                    && tenant.getStatus() == STATUS_ENABLED;
        } finally {
            TenantContextHolder.setIgnoreTenant(previous);
        }
    }
}
