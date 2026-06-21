package com.smartdx.retail.e2e;

import com.smartdx.retail.model.vo.AlertPageVO;
import com.smartdx.retail.service.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for alert list with product/store name resolution.
 */
class AlertE2ETest extends RetailE2EBase {

    @Autowired
    private AlertService alertService;

    @Test
    void listAlerts_returnsAlerts_withStoreAndProductName() {
        // Given
        Long storeId = seedStore("S-ALERT-001", "アラートテスト店", "ONLINE");
        Long categoryId = seedCategory("CAT-ALERT-001", "アラートカテゴリ");
        Long productId = seedProduct("P-ALERT-001", "アラート商品", categoryId);

        seedAlert(storeId, productId, "LOW_STOCK", "NEW", "P1");

        // When
        List<AlertPageVO> alerts = alertService.listAlerts(null, null);

        // Then
        assertThat(alerts).hasSize(1);
        AlertPageVO alert = alerts.get(0);
        assertThat(alert.getStoreName()).isEqualTo("アラートテスト店");
        assertThat(alert.getProductName()).isEqualTo("アラート商品");
        assertThat(alert.getProductCode()).isEqualTo("P-ALERT-001");
        assertThat(alert.getAlertType()).isEqualTo("LOW_STOCK");
        assertThat(alert.getStatus()).isEqualTo("NEW");
    }

    @Test
    void getAlertById_returnsAlert_withResolvedNames() {
        // Given
        Long storeId = seedStore("S-ALERT-002", "アラートテスト店2", "ONLINE");
        Long categoryId = seedCategory("CAT-ALERT-002", "アラートカテゴリ2");
        Long productId = seedProduct("P-ALERT-002", "アラート商品2", categoryId);

        seedAlert(storeId, productId, "HIGH_STOCK", "ACK", "P2");
        Long alertId = jdbcTemplate.queryForObject(
                "SELECT id FROM retail_alert WHERE product_id = ? AND alert_type = ?",
                Long.class,
                productId, "HIGH_STOCK"
        );

        // When
        AlertPageVO alert = alertService.getAlertById(alertId);

        // Then
        assertThat(alert).isNotNull();
        assertThat(alert.getStoreName()).isEqualTo("アラートテスト店2");
        assertThat(alert.getProductName()).isEqualTo("アラート商品2");
        assertThat(alert.getStatus()).isEqualTo("ACK");
    }
}
