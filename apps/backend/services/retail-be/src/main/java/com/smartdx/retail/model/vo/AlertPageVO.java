package com.smartdx.retail.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Alert page VO
 */
@Schema(description = "Alert page VO")
@Data
public class AlertPageVO {

    @Schema(description = "Alert ID")
    private Long id;

    @Schema(description = "Store ID")
    private Long storeId;

    @Schema(description = "Store name")
    private String storeName;

    @Schema(description = "Product ID")
    private Long productId;

    @Schema(description = "Product name")
    private String productName;

    @Schema(description = "Product code")
    private String productCode;

    @Schema(description = "Device ID")
    private Long deviceId;

    @Schema(description = "Device name")
    private String deviceName;

    @Schema(description = "Lot number")
    private String lotNumber;

    @Schema(description = "Alert type")
    private String alertType;

    @Schema(description = "Priority")
    private String priority;

    @Schema(description = "Status")
    private String status;

    @Schema(description = "Message")
    private String message;

    @Schema(description = "Threshold value")
    private String thresholdValue;

    @Schema(description = "Current value")
    private String currentValue;

    @Schema(description = "Detected at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime detectedAt;

    @Schema(description = "Acknowledged at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acknowledgedAt;

    @Schema(description = "Resolved at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;

    @Schema(description = "Closed at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closedAt;

    @Schema(description = "Resolution note")
    private String resolutionNote;

    @Schema(description = "Created at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "Updated at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
