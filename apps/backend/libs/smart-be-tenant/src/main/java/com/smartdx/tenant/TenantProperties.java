package com.smartdx.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * マルチテナント設定プロパティ
 * <p>
 * application.yml での設定例:
 * <pre>
 * smart-be-tenant:
 *   enabled: true
 *   column: tenant_id
 *   default-tenant-id: 1
 *   force-default: false
 *   ignore-tables:
 *     - sys_tenant
 *     - sys_config
 * </pre>
 */
@ConfigurationProperties(prefix = "smart-be-tenant")
public class TenantProperties {

    /**
     * マルチテナント機能の有効/無効
     */
    private boolean enabled = true;

    /**
     * テナントIDカラム名 (デフォルト: tenant_id)
     */
    private String column = "tenant_id";

    /**
     * デフォルトテナントID (force-default=true 時、または未認証時に使用)
     */
    private Long defaultTenantId = 1L;

    /**
     * 強制デフォルトモード (true: 全リクエストをdefaultTenantIdで処理)
     * retail-be のような1テナント固定運用に使用
     */
    private boolean forceDefault = false;

    /**
     * テナントフィルタを無視するテーブル名リスト
     */
    private List<String> ignoreTables = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public Long getDefaultTenantId() {
        return defaultTenantId;
    }

    public void setDefaultTenantId(Long defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    public boolean isForceDefault() {
        return forceDefault;
    }

    public void setForceDefault(boolean forceDefault) {
        this.forceDefault = forceDefault;
    }

    public List<String> getIgnoreTables() {
        return ignoreTables;
    }

    public void setIgnoreTables(List<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }
}
