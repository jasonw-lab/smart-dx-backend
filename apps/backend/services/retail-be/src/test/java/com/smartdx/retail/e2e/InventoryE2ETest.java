package com.smartdx.retail.e2e;

import com.smartdx.retail.model.vo.InventoryPageVO;
import com.smartdx.retail.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for inventory list with product/store name resolution.
 */
class InventoryE2ETest extends RetailE2EBase {

    @Autowired
    private InventoryService inventoryService;

    @Test
    void listInventories_returnsInventory_withProductAndStoreName() {
        // Given
        Long storeId = seedStore("S-INV-001", "在庫テスト店", "ONLINE");
        Long categoryId = seedCategory("CAT-INV-001", "テストカテゴリ");
        Long productId = seedProduct("P-INV-001", "テスト商品", categoryId);

        seedInventory(storeId, productId, "LOT-001", 50);

        // When
        List<InventoryPageVO> inventories = inventoryService.listInventories(null, null);

        // Then
        assertThat(inventories).hasSize(1);
        InventoryPageVO inventory = inventories.get(0);
        assertThat(inventory.getStoreName()).isEqualTo("在庫テスト店");
        assertThat(inventory.getProductName()).isEqualTo("テスト商品");
        assertThat(inventory.getProductCode()).isEqualTo("P-INV-001");
        assertThat(inventory.getQuantity()).isEqualTo(50);
        assertThat(inventory.getStatus()).isEqualTo("NORMAL");
        assertThat(inventory.getStatusLabel()).isEqualTo("正常");
    }

    @Test
    void listInventories_returnsLowStockStatus() {
        // Given
        Long storeId = seedStore("S-INV-002", "在庫テスト店2", "ONLINE");
        Long categoryId = seedCategory("CAT-INV-002", "テストカテゴリ2");
        Long productId = seedProduct("P-INV-002", "ローソク商品", categoryId);

        // reorder_point=0, max_stock=0 by default seeding; set reorder_point explicitly
        jdbcTemplate.update(
                "UPDATE retail_product SET reorder_point = 10 WHERE id = ?",
                productId
        );
        seedInventory(storeId, productId, "LOT-002", 5);

        // When
        List<InventoryPageVO> inventories = inventoryService.listInventories(null, null);

        // Then
        InventoryPageVO inventory = inventories.get(0);
        assertThat(inventory.getStatus()).isEqualTo("LOW_STOCK");
        assertThat(inventory.getStatusLabel()).isEqualTo("在庫切れ");
        assertThat(inventory.getMinStock()).isEqualTo(10);
    }
}
