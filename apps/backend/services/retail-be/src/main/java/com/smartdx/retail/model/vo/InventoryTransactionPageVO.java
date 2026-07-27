package com.smartdx.retail.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 入出庫履歴ページングVO
 */
@Schema(description = "入出庫履歴ページングVO")
@Data
public class InventoryTransactionPageVO {

    @Schema(description = "入出庫ID")
    private Long id;

    @Schema(description = "在庫ID")
    private Long inventoryId;

    @Schema(description = "店舗ID")
    private Long storeId;

    @Schema(description = "店舗名")
    private String storeName;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名")
    private String productName;

    @Schema(description = "商品コード")
    private String productCode;

    @Schema(description = "ロット番号")
    private String lotNumber;

    @Schema(description = "操作タイプ（INBOUND, SALE, ADJUSTMENT, DISPOSAL, TRANSFER_IN, TRANSFER_OUT）")
    private String txnType;

    @Schema(description = "数量変動（正=増加, 負=減少）")
    private Integer quantityDelta;

    @Schema(description = "操作元（MANUAL, POS, BATCH）")
    private String sourceType;

    @Schema(description = "理由（廃棄理由等）")
    private String reason;

    @Schema(description = "参照番号（売上ID、発注番号等）")
    private String referenceNo;

    @Schema(description = "操作日時")
    private LocalDateTime occurredAt;

    @Schema(description = "備考（理由、移動先等）")
    private String note;

    @Schema(description = "作成時間")
    private LocalDateTime createTime;

    @Schema(description = "更新時間")
    private LocalDateTime updateTime;
}
