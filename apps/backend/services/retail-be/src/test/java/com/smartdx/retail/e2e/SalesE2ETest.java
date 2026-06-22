package com.smartdx.retail.e2e;

import com.smartdx.retail.model.entity.Sales;
import com.smartdx.retail.service.SalesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for sales/payment history.
 */
class SalesE2ETest extends RetailE2EBase {

    @Autowired
    private SalesService salesService;

    @Test
    void listSales_returnsSalesWithinDateRange_withStoreName() {
        // Given
        Long storeId = seedStore("S001", "東京本店", "ONLINE");
        LocalDateTime today = LocalDateTime.now();
        seedSales(storeId, "ORD-001", "CARD", today, BigDecimal.valueOf(1500));
        seedSales(storeId, "ORD-002", "CASH", today.minusDays(1), BigDecimal.valueOf(800));

        // When
        List<Sales> salesList = salesService.listSales(null);

        // Then
        assertThat(salesList).hasSize(2);
        assertThat(salesList.get(0).getStoreName()).isEqualTo("東京本店");
        assertThat(salesList.get(1).getStoreName()).isEqualTo("東京本店");
    }

    @Test
    void getSalesById_returnsSale_withStoreName() {
        // Given
        Long storeId = seedStore("S002", "大阪店", "ONLINE");
        seedSales(storeId, "ORD-003", "QR", LocalDateTime.now(), BigDecimal.valueOf(2500));
        Long salesId = jdbcTemplate.queryForObject(
                "SELECT id FROM retail_sales WHERE order_number = ?",
                Long.class,
                "ORD-003"
        );

        // When
        Sales sales = salesService.getSalesById(salesId);

        // Then
        assertThat(sales).isNotNull();
        assertThat(sales.getStoreName()).isEqualTo("大阪店");
        assertThat(sales.getOrderNumber()).isEqualTo("ORD-003");
    }
}
