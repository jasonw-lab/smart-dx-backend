package com.smartdx.retail.controller;

import com.smartdx.core.result.Result;
import com.smartdx.retail.model.entity.Inventory;
import com.smartdx.retail.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Inventory Controller
 * <p>
 * REST API path: /api/v1/retail/inventories (unchanged from original smart-retail-dx)
 * </p>
 */
@Tag(name = "Inventory API")
@RestController
@RequestMapping("/api/v1/retail/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Get inventories")
    @GetMapping
    public Result<List<Inventory>> listInventories(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long productId) {
        return Result.success(inventoryService.listInventories(storeId, productId));
    }

    @Operation(summary = "Get inventory by ID")
    @GetMapping("/{id}")
    public Result<Inventory> getInventory(@PathVariable Long id) {
        return Result.success(inventoryService.getInventoryById(id));
    }

    @Operation(summary = "Create inventory")
    @PostMapping
    public Result<?> createInventory(@RequestBody Inventory inventory) {
        return Result.judge(inventoryService.createInventory(inventory));
    }

    @Operation(summary = "Update inventory")
    @PutMapping("/{id}")
    public Result<?> updateInventory(@PathVariable Long id, @RequestBody Inventory inventory) {
        return Result.judge(inventoryService.updateInventory(id, inventory));
    }

    @Operation(summary = "Delete inventory")
    @DeleteMapping("/{id}")
    public Result<?> deleteInventory(@PathVariable Long id) {
        return Result.judge(inventoryService.deleteInventory(id));
    }
}
