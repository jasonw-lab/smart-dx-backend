package com.smartdx.tenant.service;

/**
 * テナント状態チェックインターフェース
 * <p>
 * TenantStatusFilterから使用される。
 * 実装はサービス層（retail-be等）で提供する。
 * </p>
 *
 * @author jason.w
 */
public interface TenantStatusChecker {

    /**
     * テナントがアクティブ状態かどうかを確認
     *
     * @param tenantId テナントID
     * @return true: アクティブ、false: 非アクティブ（SUSPENDED/CANCELLED）
     */
    boolean isActive(Long tenantId);
}
