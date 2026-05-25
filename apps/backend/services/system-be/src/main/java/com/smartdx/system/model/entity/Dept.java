package com.smartdx.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部門エンティティ
 */
@Data
@TableName("sys_dept")
public class Dept {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String name;

    private String code;

    private Integer sort;

    private Integer status;

    private String treePath;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    @TableLogic
    private Integer isDeleted;

    private Long tenantId;
}
