package com.smartdx.system.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色分页VO
 */
@Schema(description = "角色分页VO")
@Data
public class RolePageVO {

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "角色状态")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "数据权限")
    private Integer dataScope;

    @Schema(description = "数据权限名称")
    private String dataScopeLabel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
