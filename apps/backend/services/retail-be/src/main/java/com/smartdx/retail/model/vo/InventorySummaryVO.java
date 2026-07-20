package com.smartdx.retail.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 在庫集約VO（店舗×商品単位、ADR-011）
 */
@Schema(description = "在庫集約VO（店舗×商品単位）")
@Data
public class InventorySummaryVO {

    @Schema(description = "集約キー（{storeId}-{productId}）")
    private String summaryKey;

    @Schema(description = "店舗ID")
    private Long storeId;

    @Schema(description = "店舗名")
    private String storeName;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品コード")
    private String productCode;

    @Schema(description = "商品名")
    private String productName;

    @Schema(description = "合計数量（ロット数量の合計）")
    private Integer totalQuantity;

    @Schema(description = "発注点（Product.reorderPoint）")
    private Integer reorderPoint;

    @Schema(description = "在庫上限（Product.maxStock）")
    private Integer upperLimit;

    @Schema(description = "最古の消費期限（ロット expiryDate の最小）")
    private LocalDate oldestExpiryDate;

    @Schema(description = "集約ステータス（EXPIRED > LOW_STOCK > EXPIRY_SOON > HIGH_STOCK > NORMAL の重大度最大）")
    private String status;

    @Schema(description = "ロット内訳")
    private List<SummaryLotVO> lots;

    @Schema(description = "在庫回転率（要件未確定のため常に null）")
    private BigDecimal turnoverRate;

    /**
     * ロット内訳VO
     */
    @Schema(description = "ロット内訳VO")
    @Data
    public static class SummaryLotVO {

        @Schema(description = "在庫ID（ロット単位のレコードID）")
        private Long id;

        @Schema(description = "ロット番号")
        private String lotNumber;

        @Schema(description = "数量")
        private Integer quantity;

        @Schema(description = "消費期限")
        private LocalDate expiryDate;
    }
}
