package com.smartdx.retail.scheduler;

import com.smartdx.retail.service.DeviceMonitorService;
import com.smartdx.retail.service.PaymentDemoService;
import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.tenant.TenantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * デバイス監視スケジューラー
 * 沈黙監視（Silent Monitoring）を定期実行
 *
 * 検知項目:
 * - COMMUNICATION_DOWN: Heartbeat未受信（5分超過）
 * - PAYMENT_TERMINAL_DOWN: 決済端末停止（status = OFFLINE/ERROR）
 * - CARD_READER_ERROR: カードリーダー異常（cardReaderConnected = false）
 * - PRINTER_PAPER_EMPTY: プリンター用紙切れ（paperLevel = EMPTY）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceMonitorScheduler {

    private final DeviceMonitorService deviceMonitorService;
    private final PaymentDemoService paymentDemoService;
    private final TenantProperties tenantProperties;

    /**
     * デバイス監視処理を12時間毎に実行
     * cron: 0時・12時の0分に実行
     */
    @Scheduled(cron = "0 0 0/12 * * ?")
    public void runDeviceMonitoring() {
        log.info("DeviceMonitorScheduler: Starting scheduled device monitoring...");

        try {
            int totalAlerts = runWithTenantContext(deviceMonitorService::runAllDeviceMonitoring);
            int totalPayments = runWithTenantContext(paymentDemoService::runDemoPayments);
            log.info("DeviceMonitorScheduler: Completed. Generated {} alerts, {} demo payments.", totalAlerts, totalPayments);
        } catch (Exception e) {
            log.error("DeviceMonitorScheduler: Error during device monitoring", e);
        }
    }

    /**
     * 手動実行用メソッド（テスト・デバッグ用）
     *
     * @return 生成されたアラート数
     */
    public int runManually() {
        log.info("DeviceMonitorScheduler: Manual execution started...");
        int totalAlerts = runWithTenantContext(deviceMonitorService::runAllDeviceMonitoring);
        int totalPayments = runWithTenantContext(paymentDemoService::runDemoPayments);
        log.info("DeviceMonitorScheduler: Manual execution completed. Generated {} alerts, {} demo payments.", totalAlerts, totalPayments);
        return totalAlerts;
    }

    /**
     * スケジューラはリクエスト外で動くためテナントコンテキストが無い。
     * retail は default テナント (sys_tenant id=1) 固定運用のため明示的に設定して実行する。
     */
    private int runWithTenantContext(java.util.function.IntSupplier task) {
        Long previous = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(tenantProperties.getDefaultTenantId());
            return task.getAsInt();
        } finally {
            if (previous != null) {
                TenantContextHolder.setTenantId(previous);
            } else {
                TenantContextHolder.clear();
            }
        }
    }
}
