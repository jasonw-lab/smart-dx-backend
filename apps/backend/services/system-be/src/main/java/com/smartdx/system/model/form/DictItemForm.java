package com.smartdx.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 辞書項目フォーム
 */
@Schema(description = "辞書項目フォーム")
@Data
public class DictItemForm {

    @Schema(description = "辞書項目ID")
    private Long id;

    @Schema(description = "辞書コード")
    @NotBlank(message = "辞書コードは必須です")
    private String dictCode;

    @Schema(description = "値")
    @NotBlank(message = "値は必須です")
    private String value;

    @Schema(description = "ラベル")
    @NotBlank(message = "ラベルは必須です")
    private String label;

    @Schema(description = "タグタイプ")
    private String tagType;

    @Schema(description = "ソート順")
    private Integer sort;

    @Schema(description = "状態(1:有効 0:無効)")
    private Integer status;

    @Schema(description = "備考")
    private String remark;
}
