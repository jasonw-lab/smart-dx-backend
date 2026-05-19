package com.smartdx.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartdx.core.result.PageResult;
import com.smartdx.core.result.Result;
import com.smartdx.system.model.entity.Tenant;
import com.smartdx.system.model.query.TenantQuery;
import com.smartdx.system.model.vo.TenantVO;
import com.smartdx.system.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Tenant management controller
 */
@Tag(name = "03. Tenant Management")
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @Operation(summary = "Get tenant by ID")
    @GetMapping("/{id}")
    public Result<TenantVO> getTenantById(@PathVariable Long id) {
        return Result.success(tenantService.getTenantById(id));
    }

    @Operation(summary = "List tenants with pagination")
    @GetMapping
    public PageResult<TenantVO> listTenants(TenantQuery query) {
        IPage<TenantVO> page = tenantService.listTenants(query);
        return PageResult.success(page.getRecords(), page.getTotal());
    }

    @Operation(summary = "Create tenant")
    @PostMapping
    public Result<Long> createTenant(@RequestBody Tenant tenant) {
        return Result.success(tenantService.createTenant(tenant));
    }

    @Operation(summary = "Update tenant")
    @PutMapping("/{id}")
    public Result<Void> updateTenant(@PathVariable Long id, @RequestBody Tenant tenant) {
        tenant.setId(id);
        tenantService.updateTenant(tenant);
        return Result.success();
    }

    @Operation(summary = "Delete tenant")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return Result.success();
    }
}
