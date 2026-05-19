package com.smartdx.retail.controller;

import com.smartdx.core.result.Result;
import com.smartdx.retail.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Dashboard Controller
 * <p>
 * REST API path: /api/v1/retail/dashboard (unchanged from original smart-retail-dx)
 * </p>
 */
@Tag(name = "Dashboard API")
@RestController
@RequestMapping("/api/v1/retail/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get dashboard KPIs")
    @GetMapping("/kpi")
    public Result<Map<String, Object>> getKpi() {
        return Result.success(dashboardService.getKpi());
    }
}
