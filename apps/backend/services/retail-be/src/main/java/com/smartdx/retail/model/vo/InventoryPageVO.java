package com.smartdx.retail.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Inventory page VO
 */
@Schema(description = "Inventory page VO")
@Data
public class InventoryPageVO {

    @Schema(description = "Inventory ID")
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

    @Schema(description = "Lot number")
    private String lotNumber;

    @Schema(description = "Current quantity")
    private Integer quantity;

    @Schema(description = "Minimum stock threshold")
    private Integer minStock;

    @Schema(description = "Maximum stock threshold")
    private Integer maxStock;

    @Schema(description = "Expiry date")
    private LocalDate expiryDate;

    @Schema(description = "Storage location")
    private String location;

    @Schema(description = "Status: NORMAL, LOW_STOCK, HIGH_STOCK, EXPIRED, EXPIRY_SOON")
    private String status;

    @Schema(description = "Status label")
    private String statusLabel;

    @Schema(description = "Status color")
    private String statusColor;

    @Schema(description = "Has expired lot")
    private Boolean hasExpiredLot;

    @Schema(description = "Days until expiry")
    private Integer daysUntilExpiry;

    @Schema(description = "Last count date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCountDate;

    @Schema(description = "Remarks")
    private String remarks;

    @Schema(description = "Created at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "Updated at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
