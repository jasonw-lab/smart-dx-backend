package com.smartdx.opensearch;

import com.smartdx.tenant.TenantContextHolder;

/**
 * テナント対応 OpenSearch クエリビルダー
 * <p>
 * 検索クエリに自動的に tenant_id フィルタを追加
 * </p>
 */
public class TenantQueryBuilder {

    private static final String TENANT_ID_FIELD = "tenant_id";

    /**
     * テナントフィルタ条件を生成 (JSON形式)
     *
     * @return テナントフィルタJSON
     */
    public static String getTenantFilter() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = 1L; // デフォルト
        }
        return String.format("{\"term\":{\"%s\":%d}}", TENANT_ID_FIELD, tenantId);
    }

    /**
     * テナントID値を取得
     *
     * @return テナントID
     */
    public static Long getTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId != null ? tenantId : 1L;
    }

    /**
     * テナントフィルタが必要か判定
     *
     * @return true: フィルタ必要, false: 不要 (@IgnoreTenant時)
     */
    public static boolean requiresTenantFilter() {
        return !TenantContextHolder.isIgnoreTenant();
    }
}
