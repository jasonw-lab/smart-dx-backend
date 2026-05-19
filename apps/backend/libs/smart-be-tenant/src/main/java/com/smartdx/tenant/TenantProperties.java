package com.smartdx.tenant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * マルチテナント設定プロパティ
 */
@Data
@ConfigurationProperties(prefix = "tenant")
public class TenantProperties {

    /**
     * マルチテナント機能の有効/無効
     */
    private boolean enabled = true;

    /**
     * テナントIDカラム名
     */
    private String column = "tenant_id";

    /**
     * デフォルトテナントID（forceDefault=true 時に使用）
     */
    private Long defaultTenantId = 1L;

    /**
     * リクエストヘッダーからテナントIDを取得するヘッダー名
     */
    private String headerName = "X-Tenant-Id";

    /**
     * 強制デフォルトモード
     * true: 全リクエストで defaultTenantId を使用（retail-be 向け）
     * false: ヘッダーまたはSecurityContextから取得（property-be 向け）
     */
    private boolean forceDefault = false;

    /**
     * テナントフィルタ対象外のテーブル名リスト
     */
    private List<String> ignoreTables = new ArrayList<>();

}

