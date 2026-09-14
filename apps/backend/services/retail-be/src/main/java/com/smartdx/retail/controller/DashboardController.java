package com.smartdx.retail.controller;

import com.smartdx.core.result.Result;
import com.smartdx.retail.model.vo.AlertPageVO;
import com.smartdx.retail.service.AlertService;
import com.smartdx.retail.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
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
    private final AlertService alertService;

    @Operation(summary = "Get dashboard KPIs")
    @GetMapping("/kpi")
    public Result<Map<String, Object>> getKpi() {
        return Result.success(dashboardService.getKpi());
    }

    @Operation(summary = "Get recent alerts for dashboard")
    @GetMapping("/alerts")
    public Result<List<AlertPageVO>> getAlerts(@RequestParam(required = false) Integer limit) {
        List<AlertPageVO> alerts = alertService.listAlerts(null, null);
        if (limit != null && limit > 0 && alerts.size() > limit) {
            alerts = alerts.subList(0, limit);
        }
        return Result.success(alerts);
    }

    @Operation(summary = "Get sales trend")
    @GetMapping("/sales-trend")
    public Result<List<Map<String, Object>>> getSalesTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "day") String interval) {
        return Result.success(dashboardService.getSalesTrend(startDate, endDate, interval));
    }
}
