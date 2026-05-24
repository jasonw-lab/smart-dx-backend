package com.smartdx.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 辞書エンティティ
 */
@Data
@TableName("sys_dict")
public class Dict {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("dict_code")
    private String code;

    private String name;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    private Long tenantId;
}
