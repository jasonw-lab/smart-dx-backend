package com.smartdx.system.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * システム設定ページVO
 */
@Schema(description = "システム設定ページVO")
@Data
public class ConfigPageVO {

    @Schema(description = "設定ID")
    private Long id;

    @Schema(description = "設定名")
    private String configName;

    @Schema(description = "設定キー")
    private String configKey;

    @Schema(description = "設定値")
    private String configValue;

    @Schema(description = "備考")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
