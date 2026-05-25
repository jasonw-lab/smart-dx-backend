package com.smartdx.retail.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 決済明細ペイロード
 */
@Data
@Schema(description = "決済明細ペイロード")
public class PaymentItemPayload {

    @Schema(description = "商品ID", example = "1")
    @NotNull(message = "商品IDは必須です")
    private Long productId;

    @Schema(description = "販売数量", example = "2")
    @NotNull(message = "販売数量は必須です")
    @Min(value = 1, message = "販売数量は1以上である必要があります")
    private Integer quantity;

    @Schema(description = "単価", example = "500.00")
    @NotNull(message = "単価は必須です")
    private BigDecimal unitPrice;
}
