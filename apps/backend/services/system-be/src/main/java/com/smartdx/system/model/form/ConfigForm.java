package com.smartdx.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * システム設定フォーム
 */
@Schema(description = "システム設定フォーム")
@Data
public class ConfigForm {

    @Schema(description = "設定ID")
    private Long id;

    @Schema(description = "設定名")
    @NotBlank(message = "設定名は必須です")
    private String configName;

    @Schema(description = "設定キー")
    @NotBlank(message = "設定キーは必須です")
    private String configKey;

    @Schema(description = "設定値")
    @NotBlank(message = "設定値は必須です")
    private String configValue;

    @Schema(description = "備考")
    private String remark;
}
