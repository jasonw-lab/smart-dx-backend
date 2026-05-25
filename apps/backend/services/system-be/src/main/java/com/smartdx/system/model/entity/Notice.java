package com.smartdx.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知エンティティ
 */
@Data
@TableName("sys_notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * タイトル
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * タイプ
     */
    private Integer type;

    /**
     * 発行者ID
     */
    private Long publisherId;

    /**
     * 優先度 (L: 低, M: 中, H: 高)
     */
    private String level;

    /**
     * 対象タイプ (1: 全員, 2: 指定)
     */
    private Integer targetType;

    /**
     * 対象ユーザーIDリスト
     */
    private String targetUserIds;

    /**
     * 発行状態 (0: 未発行, 1: 発行済み, -1: 取り消し済み)
     */
    private Integer publishStatus;

    /**
     * 発行日時
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    /**
     * 取り消し日時
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime revokeTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

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
