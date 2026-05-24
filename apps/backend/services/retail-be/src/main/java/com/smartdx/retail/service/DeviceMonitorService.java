package com.smartdx.retail.service;

/**
 * デバイス監視サービスインターフェース
 * 沈黙監視（Silent Monitoring）のためのデバイス状態チェック機能を提供
 */
public interface DeviceMonitorService {

    /**
     * 通信断検知（COMMUNICATION_DOWN）
     * last_heartbeat が閾値（5分）を超過したデバイスを検出しアラートを生成
     *
     * @return 生成されたアラート数
     */
    int detectCommunicationDownAlerts();

    /**
     * 決済端末停止検知（PAYMENT_TERMINAL_DOWN）
     * deviceStats.paymentTerminal.status = OFFLINE/ERROR のデバイスを検出しアラートを生成
     *
     * @return 生成されたアラート数
     */
    int detectPaymentTerminalDownAlerts();

    /**
     * カードリーダー異常検知（CARD_READER_ERROR）
     * deviceStats.paymentTerminal.cardReaderConnected = false のデバイスを検出しアラートを生成
     *
     * @return 生成されたアラート数
     */
    int detectCardReaderErrorAlerts();

    /**
     * プリンター用紙切れ検知（PRINTER_PAPER_EMPTY）
     * deviceStats.printer.paperLevel = EMPTY のデバイスを検出しアラートを生成
     *
     * @return 生成されたアラート数
     */
    int detectPrinterPaperEmptyAlerts();

    /**
     * 全デバイス監視処理を実行
     * 全ての障害検知メソッドを順次実行
     *
     * @return 生成された総アラート数
     */
    int runAllDeviceMonitoring();
}
