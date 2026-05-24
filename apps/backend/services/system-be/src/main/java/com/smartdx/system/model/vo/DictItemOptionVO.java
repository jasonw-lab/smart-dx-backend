package com.smartdx.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 辞書項目オプションVO
 */
@Schema(description = "辞書項目オプションVO")
@Data
public class DictItemOptionVO {

    @Schema(description = "値")
    private String value;

    @Schema(description = "ラベル")
    private String label;

    @Schema(description = "タグタイプ")
    private String tagType;
}
