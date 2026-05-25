package com.smartdx.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 部門フォーム
 */
@Schema(description = "部門フォーム")
@Data
public class DeptForm {

    @Schema(description = "部門ID")
    private Long id;

    @Schema(description = "親部門ID")
    private Long parentId;

    @Schema(description = "部門名")
    @NotBlank(message = "部門名は必須です")
    private String name;

    @Schema(description = "部門コード")
    private String code;

    @Schema(description = "ソート順")
    private Integer sort;

    @Schema(description = "状態(1:有効 0:無効)")
    private Integer status;
}
