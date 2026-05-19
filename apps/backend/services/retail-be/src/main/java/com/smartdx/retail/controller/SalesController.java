package com.smartdx.retail.controller;

import com.smartdx.core.result.Result;
import com.smartdx.retail.model.entity.Sales;
import com.smartdx.retail.service.SalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Sales Controller
 * <p>
 * REST API path: /api/v1/retail/sales (unchanged from original smart-retail-dx)
 * </p>
 */
@Tag(name = "Sales API")
@RestController
@RequestMapping("/api/v1/retail/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @Operation(summary = "Get sales")
    @GetMapping
    public Result<List<Sales>> listSales(@RequestParam(required = false) Long storeId) {
        return Result.success(salesService.listSales(storeId));
    }

    @Operation(summary = "Get sales by ID")
    @GetMapping("/{id}")
    public Result<Sales> getSales(@PathVariable Long id) {
        return Result.success(salesService.getSalesById(id));
    }

    @Operation(summary = "Create sales")
    @PostMapping
    public Result<?> createSales(@RequestBody Sales sales) {
        return Result.judge(salesService.createSales(sales));
    }
}
