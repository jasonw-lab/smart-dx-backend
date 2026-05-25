package com.smartdx.system.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部門VO
 */
@Schema(description = "部門VO")
@Data
public class DeptVO {

    @Schema(description = "部門ID")
    private Long id;

    @Schema(description = "親部門ID")
    private Long parentId;

    @Schema(description = "部門名")
    private String name;

    @Schema(description = "部門コード")
    private String code;

    @Schema(description = "ソート順")
    private Integer sort;

    @Schema(description = "状態")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "子部門")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DeptVO> children;
}
