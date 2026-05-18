package com.smartdx.tenant.mybatis;

import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.tenant.TenantProperties;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * MyBatis-Plus マルチテナントハンドラ
 * <p>
 * SQLに自動的に tenant_id 条件を付与する
 * </p>
 */
public class TenantLineHandler implements com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler {

    private static final Logger log = LoggerFactory.getLogger(TenantLineHandler.class);

    private static final Set<String> SYSTEM_TABLES = Set.of(
            "tables",
            "columns",
            "all_tables",
            "all_tab_comments",
            "all_objects",
            "all_tab_columns",
            "all_col_comments",
            "all_cons_columns",
            "all_constraints"
    );

    private final TenantProperties tenantProperties;

    public TenantLineHandler(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    @Override
    public Expression getTenantId() {
        log.debug("getTenantId() called");

        // force-default モードの場合、常にデフォルトテナントIDを使用
        if (tenantProperties.isForceDefault()) {
            Long defaultId = tenantProperties.getDefaultTenantId();
            log.debug("Force default mode: using default tenant ID: {}", defaultId);
            return new LongValue(defaultId);
        }

        Long tenantId = TenantContextHolder.getTenantId();
        log.debug("Got tenant ID from TenantContextHolder: {}", tenantId);

        if (tenantId == null) {
            // デフォルトテナントIDにフォールバック
            Long defaultId = tenantProperties.getDefaultTenantId();
            if (defaultId != null) {
                log.debug("TenantId is null, falling back to default: {}", defaultId);
                return new LongValue(defaultId);
            }
            throw new IllegalStateException(
                    "TenantId is required but was null. Ensure TenantContextHolder is set (e.g., via token) before DB access."
            );
        }

        return new LongValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return tenantProperties.getColumn();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (tableName == null) {
            return false;
        }

        // システムテーブルは無視
        if (SYSTEM_TABLES.contains(tableName.toLowerCase())) {
            return true;
        }

        // @IgnoreTenant フラグがセットされている場合は無視
        if (TenantContextHolder.isIgnoreTenant()) {
            return true;
        }

        // 設定で指定されたテーブルは無視
        List<String> ignoreTables = tenantProperties.getIgnoreTables();
        if (ignoreTables == null || ignoreTables.isEmpty()) {
            return false;
        }

        return ignoreTables.stream()
                .anyMatch(ignoreTable -> ignoreTable.equalsIgnoreCase(tableName));
    }
}
