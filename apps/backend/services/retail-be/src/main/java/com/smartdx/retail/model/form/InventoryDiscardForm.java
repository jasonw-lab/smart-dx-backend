package com.smartdx.retail.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 在庫廃棄フォーム（ADR-011）
 */
@Schema(description = "在庫廃棄フォーム")
@Data
public class InventoryDiscardForm {

    @Schema(description = "廃棄数量（1以上、対象ロットの現在数量以下）")
    @NotNull(message = "廃棄数量は必須です")
    @Min(value = 1, message = "廃棄数量は1以上で入力してください")
    private Integer quantity;

    @Schema(description = "廃棄理由（1〜100文字、監査履歴に記録）")
    @NotBlank(message = "廃棄理由は必須です")
    @Size(max = 100, message = "廃棄理由は100文字以内で入力してください")
    private String reason;

    @Schema(description = "備考（任意、最大500文字）")
    @Size(max = 500, message = "備考は500文字以内で入力してください")
    private String remarks;
}
