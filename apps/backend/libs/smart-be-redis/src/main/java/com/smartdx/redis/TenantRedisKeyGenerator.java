package com.smartdx.redis;

import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.tenant.TenantProperties;

/**
 * テナント対応 Redis キー生成器
 * <p>
 * キー形式: {service}:{tenantId}:{businessKey}
 * 例: property:1:search:condition:userId-123
 * </p>
 */
public class TenantRedisKeyGenerator {

    private final String serviceName;
    private final TenantProperties tenantProperties;

    public TenantRedisKeyGenerator(String serviceName, TenantProperties tenantProperties) {
        this.serviceName = serviceName;
        this.tenantProperties = tenantProperties;
    }

    /**
     * テナント付きキーを生成
     *
     * @param businessKey 業務キー
     * @return 完全なキー
     */
    public String generate(String businessKey) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = getDefaultTenantId();
        }
        return String.format("%s:%d:%s", serviceName, tenantId, businessKey);
    }

    /**
     * テナントを指定してキーを生成
     *
     * @param tenantId    テナントID
     * @param businessKey 業務キー
     * @return 完全なキー
     */
    public String generate(Long tenantId, String businessKey) {
        return String.format("%s:%d:%s", serviceName, tenantId, businessKey);
    }

    /**
     * テナントプレフィックスを取得 (パターンマッチ用)
     *
     * @return テナントプレフィックス (例: "property:1:*")
     */
    public String getTenantPrefix() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = getDefaultTenantId();
        }
        return String.format("%s:%d:*", serviceName, tenantId);
    }

    private Long getDefaultTenantId() {
        if (tenantProperties != null && tenantProperties.getDefaultTenantId() != null) {
            return tenantProperties.getDefaultTenantId();
        }
        return 1L;
    }
}
