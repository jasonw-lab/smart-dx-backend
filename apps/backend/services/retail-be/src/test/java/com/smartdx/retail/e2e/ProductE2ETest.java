package com.smartdx.retail.e2e;

import com.smartdx.core.result.PageResult;
import com.smartdx.retail.model.query.ProductPageQuery;
import com.smartdx.retail.model.vo.ProductPageVO;
import com.smartdx.retail.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for product page stock/sales aggregation.
 */
class ProductE2ETest extends RetailE2EBase {

    @Autowired
    private ProductService productService;

    @Test
    void getProductPage_returnsStockAndSales() {
        // Given
        Long storeId = seedStore("S005", "札幌店", "ONLINE");
        Long categoryId = seedCategory("CAT-001", "飲料");
        Long productId = seedProduct("P-001", "プレミアムコーヒー", categoryId);

        // Stock: 2 lots with 30 and 20 => total 50
        seedInventory(storeId, productId, "LOT-001", 30);
        seedInventory(storeId, productId, "LOT-002", 20);

        // Sales: 2 details with 5 and 3 => total 8
        seedSales(storeId, "ORD-010", "CARD", LocalDateTime.now(), BigDecimal.valueOf(1000));
        Long salesId = jdbcTemplate.queryForObject(
                "SELECT id FROM retail_sales WHERE order_number = ?",
                Long.class,
                "ORD-010"
        );
        seedSalesDetail(salesId, productId, "LOT-001", 5, BigDecimal.valueOf(100));
        seedSalesDetail(salesId, productId, "LOT-002", 3, BigDecimal.valueOf(100));

        // When
        ProductPageQuery query = new ProductPageQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        PageResult<ProductPageVO> result = productService.getProductPage(query);

        // Then
        List<ProductPageVO> list = result.getData().getList();
        assertThat(list).hasSize(1);
        ProductPageVO product = list.get(0);
        assertThat(product.getStock()).isEqualTo(50);
        assertThat(product.getSales()).isEqualTo(8);
    }

    @Test
    void getProductPage_returnsZeroForProductWithoutInventoryOrSales() {
        // Given
        Long categoryId = seedCategory("CAT-002", "食品");
        seedProduct("P-002", "新商品", categoryId);

        // When
        ProductPageQuery query = new ProductPageQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        PageResult<ProductPageVO> result = productService.getProductPage(query);

        // Then
        List<ProductPageVO> list = result.getData().getList();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStock()).isZero();
        assertThat(list.get(0).getSales()).isZero();
    }
}
