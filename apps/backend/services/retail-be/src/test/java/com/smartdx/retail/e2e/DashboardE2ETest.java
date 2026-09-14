package com.smartdx.retail.e2e;

import com.smartdx.retail.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for dashboard KPI aggregation.
 */
class DashboardE2ETest extends RetailE2EBase {

    @Autowired
    private DashboardService dashboardService;

    @Test
    void getKpi_returnsExpectedKeysAndValues() {
        // Given
        Long store1 = seedStore("S006", "横浜店", "ONLINE");
        Long store2 = seedStore("S007", "神戸店", "OFFLINE");
        Long store3 = seedStore("S008", "広島店", "ONLINE");

        Long categoryId = seedCategory("CAT-003", "スイーツ");
        Long product1 = seedProduct("P-003", "チョコレート", categoryId);
        Long product2 = seedProduct("P-004", "クッキー", categoryId);
        Long product3 = seedProduct("P-005", "アイス", categoryId);

        // Out of stock: product1 has 0, product2 has negative-ish (sum 0)
        seedInventory(store1, product1, "LOT-010", 0);
        seedInventory(store1, product2, "LOT-011", 0);
        // In stock
        seedInventory(store1, product3, "LOT-012", 10);

        // Pending alerts
        seedAlert(store1, product1, "LOW_STOCK", "NEW", "P1");
        seedAlert(store1, product2, "LOW_STOCK", "ACK", "P2");
        seedAlert(store1, product3, "HIGH_STOCK", "RESOLVED", "P3");

        // Today's sales
        seedSales(store1, "ORD-020", "CARD", LocalDateTime.now(), BigDecimal.valueOf(1200));
        // Yesterday's sales
        seedSales(store1, "ORD-021", "CASH", LocalDateTime.now().minusDays(1), BigDecimal.valueOf(800));

        // When
        Map<String, Object> kpi = dashboardService.getKpi();

        // Then
        assertThat(kpi).containsKeys(
                "todaySales",
                "salesGrowthRate",
                "activeStoreCount",
                "totalStoreCount",
                "pendingAlertCount",
                "outOfStockSkuCount"
        );
        assertThat((BigDecimal) kpi.get("todaySales")).isEqualByComparingTo(BigDecimal.valueOf(1200));
        assertThat(kpi.get("activeStoreCount")).isEqualTo(2L);
        assertThat(kpi.get("totalStoreCount")).isEqualTo(3L);
        assertThat(kpi.get("pendingAlertCount")).isEqualTo(2L);
        assertThat(kpi.get("outOfStockSkuCount")).isEqualTo(2L);
        // Growth rate: (1200 - 800) / 800 * 100 = 50.00
        assertThat((BigDecimal) kpi.get("salesGrowthRate"))
                .isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    void getSalesTrend_monthly_returnsTwelveMonths() {
        // Given
        Long store1 = seedStore("S009", "千葉店", "ONLINE");
        seedSales(store1, "ORD-030", "CARD", LocalDateTime.of(2026, 1, 15, 12, 0), BigDecimal.valueOf(10000));
        seedSales(store1, "ORD-031", "CASH", LocalDateTime.of(2026, 2, 15, 12, 0), BigDecimal.valueOf(15000));
        seedSales(store1, "ORD-032", "QR", LocalDateTime.of(2026, 12, 15, 12, 0), BigDecimal.valueOf(20000));

        // When
        List<Map<String, Object>> trend = dashboardService.getSalesTrend(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "month"
        );

        // Then
        assertThat(trend).hasSize(12);
        assertThat(trend.get(0).get("date")).isEqualTo("2026/1");
        assertThat(trend.get(11).get("date")).isEqualTo("2026/12");
        assertThat((BigDecimal) trend.get(0).get("salesAmount")).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat((BigDecimal) trend.get(1).get("salesAmount")).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat((BigDecimal) trend.get(11).get("salesAmount")).isEqualByComparingTo(BigDecimal.valueOf(20000));
    }
}
