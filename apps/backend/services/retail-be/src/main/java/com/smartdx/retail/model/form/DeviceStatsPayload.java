package com.smartdx.retail.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * デバイス状態ペイロード
 */
@Data
@Schema(description = "デバイス状態ペイロード")
public class DeviceStatsPayload {

    @Schema(description = "決済端末状態")
    private PaymentTerminalStats paymentTerminal;

    @Schema(description = "プリンター状態")
    private PrinterStats printer;

    @Schema(description = "ネットワーク状態")
    private NetworkStats network;

    /**
     * 決済端末状態
     */
    @Data
    @Schema(description = "決済端末状態")
    public static class PaymentTerminalStats {
        @Schema(description = "ステータス", example = "ONLINE")
        private String status;

        @Schema(description = "カードリーダー接続状態", example = "true")
        private Boolean cardReaderConnected;

        @Schema(description = "最終セルフテスト日時")
        private LocalDateTime lastSelfTestAt;

        @Schema(description = "エラーコード")
        private String errorCode;
    }

    /**
     * プリンター状態
     */
    @Data
    @Schema(description = "プリンター状態")
    public static class PrinterStats {
        @Schema(description = "ステータス", example = "ONLINE")
        private String status;

        @Schema(description = "用紙レベル", example = "OK")
        private String paperLevel;
    }

    /**
     * ネットワーク状態
     */
    @Data
    @Schema(description = "ネットワーク状態")
    public static class NetworkStats {
        @Schema(description = "レイテンシ（ミリ秒）", example = "45")
        private Integer latencyMs;

        @Schema(description = "信号強度", example = "GOOD")
        private String signalStrength;
    }
}
