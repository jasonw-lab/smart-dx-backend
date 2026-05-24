package com.smartdx.retail.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Heartbeat受信ペイロード
 */
@Data
@Schema(description = "Heartbeat受信ペイロード")
public class HeartbeatPayload {

    @Schema(description = "店舗ID", example = "1")
    @NotNull(message = "店舗IDは必須です")
    private Long storeId;

    @Schema(description = "タイムスタンプ")
    @NotNull(message = "タイムスタンプは必須です")
    private LocalDateTime timestamp;

    @Schema(description = "デバイス状態")
    private DeviceStatsPayload deviceStats;
}
