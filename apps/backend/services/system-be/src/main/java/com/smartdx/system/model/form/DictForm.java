package com.smartdx.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 辞書フォーム
 */
@Schema(description = "辞書フォーム")
@Data
public class DictForm {

    @Schema(description = "辞書ID")
    private Long id;

    @Schema(description = "辞書コード")
    @NotBlank(message = "辞書コードは必須です")
    private String code;

    @Schema(description = "辞書名")
    @NotBlank(message = "辞書名は必須です")
    private String name;

    @Schema(description = "状態(1:有効 0:無効)")
    private Integer status;

    @Schema(description = "備考")
    private String remark;
}
