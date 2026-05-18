package com.smartdx.redis;

import com.smartdx.tenant.TenantContextHolder;

/**
 * テナント対応 Redis キー生成器
 * <p>
 * キー形式: {service}:{tenantId}:{businessKey}
 * 例: property:1:search:condition:userId-123
 * </p>
 */
public class TenantRedisKeyGenerator {

    private final String serviceName;

    public TenantRedisKeyGenerator(String serviceName) {
        this.serviceName = serviceName;
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
            tenantId = 1L; // デフォルトテナント
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
            tenantId = 1L;
        }
        return String.format("%s:%d:*", serviceName, tenantId);
    }
}
