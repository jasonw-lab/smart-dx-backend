package com.smartdx.retail.model.query;

import com.smartdx.core.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 入出庫履歴ページングクエリ
 */
@Schema(description = "入出庫履歴ページングクエリ")
@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryTransactionPageQuery extends BaseQuery {

    @Schema(description = "店舗ID")
    private Long storeId;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "ロット番号")
    private String lotNumber;

    @Schema(description = "操作タイプ（INBOUND, SALE, ADJUSTMENT, DISPOSAL, TRANSFER_IN, TRANSFER_OUT）")
    private String txnType;

    @Schema(description = "操作元（MANUAL, POS, BATCH）")
    private String sourceType;

    @Schema(description = "操作日時（開始）")
    private LocalDateTime occurredAtStart;

    @Schema(description = "操作日時（終了）")
    private LocalDateTime occurredAtEnd;

    @Schema(description = "参照番号（売上ID、発注番号等）")
    private String referenceNo;
}
