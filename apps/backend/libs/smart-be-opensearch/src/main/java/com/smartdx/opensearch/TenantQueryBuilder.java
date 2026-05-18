package com.smartdx.opensearch;

import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.tenant.TenantProperties;
import org.springframework.stereotype.Component;

/**
 * テナント対応 OpenSearch クエリビルダー
 * <p>
 * 検索クエリに自動的に tenant_id フィルタを追加
 * </p>
 */
@Component
public class TenantQueryBuilder {

    private static final String TENANT_ID_FIELD = "tenant_id";

    private final TenantProperties tenantProperties;

    public TenantQueryBuilder(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    /**
     * テナントフィルタ条件を生成 (JSON形式)
     *
     * @return テナントフィルタJSON
     */
    public String getTenantFilter() {
        Long tenantId = resolveTenantId();
        return String.format("{\"term\":{\"%s\":%d}}", TENANT_ID_FIELD, tenantId);
    }

    /**
     * テナントID値を取得
     *
     * @return テナントID
     */
    public Long getTenantId() {
        return resolveTenantId();
    }

    /**
     * テナントフィルタが必要か判定
     *
     * @return true: フィルタ必要, false: 不要 (@IgnoreTenant時)
     */
    public boolean requiresTenantFilter() {
        return !TenantContextHolder.isIgnoreTenant();
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        if (tenantProperties != null && tenantProperties.getDefaultTenantId() != null) {
            return tenantProperties.getDefaultTenantId();
        }
        return 1L;
    }
}
