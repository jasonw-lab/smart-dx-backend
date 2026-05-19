package com.smartdx.retail.controller;

import com.smartdx.core.result.Result;
import com.smartdx.retail.model.entity.Store;
import com.smartdx.retail.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Store Controller
 * <p>
 * REST API path: /api/v1/retail/stores (unchanged from original smart-retail-dx)
 * </p>
 */
@Tag(name = "Store API")
@RestController
@RequestMapping("/api/v1/retail/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @Operation(summary = "Get all stores")
    @GetMapping
    public Result<List<Store>> listStores() {
        return Result.success(storeService.listStores());
    }

    @Operation(summary = "Get store by ID")
    @GetMapping("/{id}")
    public Result<Store> getStore(@PathVariable Long id) {
        return Result.success(storeService.getStoreById(id));
    }

    @Operation(summary = "Create store")
    @PostMapping
    public Result<?> createStore(@RequestBody Store store) {
        return Result.judge(storeService.createStore(store));
    }

    @Operation(summary = "Update store")
    @PutMapping("/{id}")
    public Result<?> updateStore(@PathVariable Long id, @RequestBody Store store) {
        return Result.judge(storeService.updateStore(id, store));
    }

    @Operation(summary = "Delete store")
    @DeleteMapping("/{id}")
    public Result<?> deleteStore(@PathVariable Long id) {
        return Result.judge(storeService.deleteStore(id));
    }
}
