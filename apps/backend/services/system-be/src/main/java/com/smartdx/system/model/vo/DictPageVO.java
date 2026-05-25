package com.smartdx.system.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 辞書ページVO
 */
@Schema(description = "辞書ページVO")
@Data
public class DictPageVO {

    @Schema(description = "辞書ID")
    private Long id;

    @Schema(description = "辞書コード")
    private String code;

    @Schema(description = "辞書名")
    private String name;

    @Schema(description = "状態")
    private Integer status;

    @Schema(description = "備考")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
