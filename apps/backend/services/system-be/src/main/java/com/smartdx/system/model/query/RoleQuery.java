package com.smartdx.system.model.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartdx.core.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 角色分页查询参数
 */
@Schema(description = "角色分页查询参数")
@Getter
@Setter
public class RoleQuery extends BaseQuery {

    @Schema(description = "关键字(角色名称/角色编码)")
    private String keywords;

    @Schema(description = "开始日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime startDate;

    @Schema(description = "结束日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime endDate;
}
