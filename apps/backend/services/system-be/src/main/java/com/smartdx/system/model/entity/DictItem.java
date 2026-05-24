package com.smartdx.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 辞書項目エンティティ
 */
@Data
@TableName("sys_dict_item")
public class DictItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("dict_code")
    private String dictCode;

    private String value;

    private String label;

    private String tagType;

    private Integer sort;

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
