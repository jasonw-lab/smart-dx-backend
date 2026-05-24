package com.smartdx.retail.controller;

import com.smartdx.core.result.Result;
import com.smartdx.retail.model.form.HeartbeatPayload;
import com.smartdx.retail.service.DeviceService;
import com.smartdx.retail.service.HeartbeatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Heartbeat Controller
 * <p>
 * REST API path: /api/v1/retail/heartbeat (unchanged from original smart-retail-dx)
 * </p>
 */
@Tag(name = "Heartbeat API")
@RestController
@RequestMapping("/api/v1/retail/heartbeat")
@RequiredArgsConstructor
public class HeartbeatController {

    private final DeviceService deviceService;
    private final HeartbeatService heartbeatService;

    @Operation(summary = "Receive device heartbeat (simple)")
    @PostMapping
    public Result<?> receiveHeartbeat(@RequestBody Map<String, Object> payload) {
        String deviceCode = (String) payload.get("deviceCode");
        if (deviceCode == null || deviceCode.isBlank()) {
            return Result.failed("Device code is required");
        }
        boolean success = deviceService.updateHeartbeat(deviceCode);
        if (!success) {
            return Result.failed("Device not found: " + deviceCode);
        }
        return Result.success();
    }

    @Operation(summary = "Receive store heartbeat with device stats", description = "店舗全体のHeartbeatを受信し、各デバイス状態を更新する")
    @PostMapping("/store")
    public Result<?> receiveStoreHeartbeat(@RequestBody @Valid HeartbeatPayload payload) {
        boolean success = heartbeatService.receiveHeartbeat(payload);
        if (!success) {
            return Result.failed("Failed to process heartbeat for store: " + payload.getStoreId());
        }
        return Result.success();
    }
}
