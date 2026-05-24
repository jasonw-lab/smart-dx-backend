package com.smartdx.retail.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 決済結果受信ペイロード
 */
@Data
@Schema(description = "決済結果受信ペイロード")
public class PaymentPayload {

    @Schema(description = "店舗ID", example = "1")
    @NotNull(message = "店舗IDは必須です")
    private Long storeId;

    @Schema(description = "注文番号", example = "ORD-20260211-001")
    private String orderNumber;

    @Schema(description = "合計金額", example = "1500.00")
    @NotNull(message = "合計金額は必須です")
    private BigDecimal totalAmount;

    @Schema(description = "支払方法（CASH, CARD, QR, OTHER）", example = "CARD")
    @NotNull(message = "支払方法は必須です")
    private String paymentMethod;

    @Schema(description = "決済プロバイダ", example = "PayPay")
    private String paymentProvider;

    @Schema(description = "決済参照ID", example = "TXN-123456")
    private String paymentReferenceId;

    @Schema(description = "売上日時")
    @NotNull(message = "売上日時は必須です")
    private LocalDateTime saleTimestamp;

    @Schema(description = "決済明細")
    @NotEmpty(message = "決済明細は必須です")
    @Valid
    private List<PaymentItemPayload> items;
}
