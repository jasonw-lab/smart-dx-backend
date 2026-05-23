package com.smartdx.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartdx.system.model.entity.Tenant;
import com.smartdx.system.model.query.TenantQuery;
import com.smartdx.system.model.vo.TenantVO;

import java.util.List;

/**
 * Tenant service interface
 */
public interface TenantService {

    /**
     * Get tenant by ID
     */
    TenantVO getTenantById(Long id);

    /**
     * Get tenant ID by domain
     */
    Long getTenantIdByDomain(String domain);

    /**
     * List tenants with pagination
     */
    IPage<TenantVO> listTenants(TenantQuery query);

    /**
     * Create tenant
     */
    Long createTenant(Tenant tenant);

    /**
     * Update tenant
     */
    void updateTenant(Tenant tenant);

    /**
     * Delete tenant
     */
    void deleteTenant(Long id);

    /**
     * Check if current user has tenant switch permission
     */
    boolean hasTenantSwitchPermission();

    /**
     * Check if user can access a specific tenant
     */
    boolean canAccessTenant(Long userId, Long tenantId);
}
