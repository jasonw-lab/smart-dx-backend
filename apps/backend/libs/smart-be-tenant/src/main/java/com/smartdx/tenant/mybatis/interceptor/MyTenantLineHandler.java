package com.smartdx.tenant.mybatis.interceptor;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.tenant.TenantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * MyBatis-Plus 多租户处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MyTenantLineHandler implements TenantLineHandler {

    private final TenantProperties tenantProperties;

    @Override
    public Expression getTenantId() {
        log.debug("getTenantId() 被调用");

        // force-default モードの場合、常にデフォルトテナントIDを使用
        if (tenantProperties.isForceDefault()) {
            Long defaultId = tenantProperties.getDefaultTenantId();
            log.debug("Force default mode: using default tenant ID: {}", defaultId);
            return new LongValue(defaultId);
        }

        Long tenantId = TenantContextHolder.getTenantId();
        log.debug("从 TenantContextHolder 获取租户ID: {}", tenantId);

        if (tenantId == null) {
            // デフォルトテナントIDにフォールバック
            Long defaultId = tenantProperties.getDefaultTenantId();
            if (defaultId != null) {
                log.debug("TenantId is null, falling back to default: {}", defaultId);
                return new LongValue(defaultId);
            }
            throw new IllegalStateException("TenantId is required but was null. Ensure TenantContextHolder is set (e.g., via token) before DB access.");
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

        Set<String> systemTables = Set.of(
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
        if (systemTables.contains(tableName.toLowerCase())) {
            return true;
        }

        if (TenantContextHolder.isIgnoreTenant()) {
            return true;
        }

        List<String> ignoreTables = tenantProperties.getIgnoreTables();
        if (ignoreTables == null || ignoreTables.isEmpty()) {
            return false;
        }

        return ignoreTables.stream()
                .anyMatch(ignoreTable -> ignoreTable.equalsIgnoreCase(tableName));
    }
}
