package com.smartdx.system.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 辞書項目ページVO
 */
@Schema(description = "辞書項目ページVO")
@Data
public class DictItemPageVO {

    @Schema(description = "辞書項目ID")
    private Long id;

    @Schema(description = "辞書コード")
    private String dictCode;

    @Schema(description = "値")
    private String value;

    @Schema(description = "ラベル")
    private String label;

    @Schema(description = "タグタイプ")
    private String tagType;

    @Schema(description = "ソート順")
    private Integer sort;

    @Schema(description = "状態")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
