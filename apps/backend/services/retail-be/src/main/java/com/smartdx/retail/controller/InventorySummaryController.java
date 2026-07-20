package com.smartdx.retail.controller;

import com.smartdx.core.result.Result;
import com.smartdx.retail.model.vo.InventorySummaryVO;
import com.smartdx.retail.service.InventorySummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 在庫集約コントローラー（ADR-011、読取専用）
 * <p>
 * REST API path: /api/v1/retail/inventory-summaries
 * </p>
 */
@Tag(name = "在庫集約API")
@RestController
@RequestMapping("/api/v1/retail/inventory-summaries")
@RequiredArgsConstructor
public class InventorySummaryController {

    private final InventorySummaryService inventorySummaryService;

    @Operation(summary = "在庫集約一覧（店舗×商品単位）")
    @GetMapping
    public Result<List<InventorySummaryVO>> listSummaries(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String status) {
        return Result.success(inventorySummaryService.listSummaries(storeId, productId, productName, status));
    }
}
