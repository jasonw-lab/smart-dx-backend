package com.smartdx.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ユーザー通知エンティティ
 */
@Data
@TableName("sys_user_notice")
public class UserNotice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 通知ID
     */
    private Long noticeId;

    /**
     * ユーザーID
     */
    private Long userId;

    /**
     * 既読状態 (0: 未読, 1: 既読)
     */
    private Integer isRead;

    /**
     * 既読日時
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private Long tenantId;
}
