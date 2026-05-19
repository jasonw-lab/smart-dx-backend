package com.smartdx.retail.controller;

import com.smartdx.core.result.Result;
import com.smartdx.retail.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(summary = "Receive device heartbeat")
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
}
