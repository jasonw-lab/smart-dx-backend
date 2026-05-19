package com.smartdx.retail.controller;

import com.smartdx.core.result.Result;
import com.smartdx.retail.model.entity.Alert;
import com.smartdx.retail.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Alert Controller
 * <p>
 * REST API path: /api/v1/retail/alerts (unchanged from original smart-retail-dx)
 * </p>
 */
@Tag(name = "Alert API")
@RestController
@RequestMapping("/api/v1/retail/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "Get alerts")
    @GetMapping
    public Result<List<Alert>> listAlerts(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String status) {
        return Result.success(alertService.listAlerts(storeId, status));
    }

    @Operation(summary = "Get alert by ID")
    @GetMapping("/{id}")
    public Result<Alert> getAlert(@PathVariable Long id) {
        return Result.success(alertService.getAlertById(id));
    }

    @Operation(summary = "Create alert")
    @PostMapping
    public Result<?> createAlert(@RequestBody Alert alert) {
        return Result.judge(alertService.createAlert(alert));
    }

    @Operation(summary = "Update alert status")
    @PatchMapping("/{id}/status")
    public Result<?> updateAlertStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String resolutionNote = body.get("resolutionNote");
        return Result.judge(alertService.updateAlertStatus(id, status, resolutionNote));
    }

    @Operation(summary = "Delete alert")
    @DeleteMapping("/{id}")
    public Result<?> deleteAlert(@PathVariable Long id) {
        return Result.judge(alertService.deleteAlert(id));
    }
}
