package com.smartdx.retail.controller;

import com.smartdx.core.result.Result;
import com.smartdx.retail.model.entity.Device;
import com.smartdx.retail.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Device Controller
 * <p>
 * REST API path: /api/v1/retail/devices (unchanged from original smart-retail-dx)
 * </p>
 */
@Tag(name = "Device API")
@RestController
@RequestMapping("/api/v1/retail/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "Get devices")
    @GetMapping
    public Result<List<Device>> listDevices(@RequestParam(required = false) Long storeId) {
        return Result.success(deviceService.listDevices(storeId));
    }

    @Operation(summary = "Get device by ID")
    @GetMapping("/{id}")
    public Result<Device> getDevice(@PathVariable Long id) {
        return Result.success(deviceService.getDeviceById(id));
    }

    @Operation(summary = "Create device")
    @PostMapping
    public Result<?> createDevice(@RequestBody Device device) {
        return Result.judge(deviceService.createDevice(device));
    }

    @Operation(summary = "Update device")
    @PutMapping("/{id}")
    public Result<?> updateDevice(@PathVariable Long id, @RequestBody Device device) {
        return Result.judge(deviceService.updateDevice(id, device));
    }

    @Operation(summary = "Delete device")
    @DeleteMapping("/{id}")
    public Result<?> deleteDevice(@PathVariable Long id) {
        return Result.judge(deviceService.deleteDevice(id));
    }
}
