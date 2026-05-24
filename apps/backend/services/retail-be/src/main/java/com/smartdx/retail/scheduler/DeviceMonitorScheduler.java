package com.smartdx.retail.scheduler;

import com.smartdx.retail.service.DeviceMonitorService;
import com.smartdx.retail.service.PaymentDemoService;
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

    /**
     * デバイス監視処理を12時間毎に実行
     * cron: 0時・12時の0分に実行
     */
    @Scheduled(cron = "0 0 0/12 * * ?")
    public void runDeviceMonitoring() {
        log.info("DeviceMonitorScheduler: Starting scheduled device monitoring...");

        try {
            int totalAlerts = deviceMonitorService.runAllDeviceMonitoring();
            int totalPayments = paymentDemoService.runDemoPayments();
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
        int totalAlerts = deviceMonitorService.runAllDeviceMonitoring();
        int totalPayments = paymentDemoService.runDemoPayments();
        log.info("DeviceMonitorScheduler: Manual execution completed. Generated {} alerts, {} demo payments.", totalAlerts, totalPayments);
        return totalAlerts;
    }
}
