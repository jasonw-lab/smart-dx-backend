package com.smartdx.retail.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 入出庫履歴フォーム
 */
@Schema(description = "入出庫履歴フォーム")
@Data
public class InventoryTransactionForm {

    @Schema(description = "在庫ID")
    private Long inventoryId;

    @Schema(description = "店舗ID")
    @NotNull(message = "店舗IDは必須です")
    private Long storeId;

    @Schema(description = "商品ID")
    @NotNull(message = "商品IDは必須です")
    private Long productId;

    @Schema(description = "ロット番号")
    @NotBlank(message = "ロット番号は必須です")
    @Size(max = 50, message = "ロット番号は50文字以内で入力してください")
    private String lotNumber;

    @Schema(description = "操作タイプ（INBOUND, SALE, ADJUSTMENT, DISPOSAL, TRANSFER_IN, TRANSFER_OUT）")
    @NotBlank(message = "操作タイプは必須です")
    private String txnType;

    @Schema(description = "数量変動（正=増加, 負=減少）")
    @NotNull(message = "数量変動は必須です")
    private Integer quantityDelta;

    @Schema(description = "操作元（MANUAL, POS, BATCH）")
    private String sourceType;

    @Schema(description = "参照番号（売上ID、発注番号等）")
    @Size(max = 100, message = "参照番号は100文字以内で入力してください")
    private String referenceNo;

    @Schema(description = "操作日時")
    private LocalDateTime occurredAt;

    @Schema(description = "備考（理由、移動先等）")
    @Size(max = 500, message = "備考は500文字以内で入力してください")
    private String note;
}
