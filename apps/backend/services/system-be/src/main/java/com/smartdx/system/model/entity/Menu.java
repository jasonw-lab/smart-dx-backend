package com.smartdx.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * メニューエンティティ
 */
@Data
@TableName(value = "sys_menu", autoResultMap = true)
public class Menu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String name;

    /**
     * メニュータイプ (C-ディレクトリ M-メニュー B-ボタン)
     */
    private String type;

    private String routeName;

    private String routePath;

    private String component;

    /**
     * 権限識別子
     */
    private String perm;

    /**
     * 表示状態 (1:表示 0:非表示)
     */
    private Integer visible;

    /**
     * メニュー範囲 (1=プラットフォーム 2=テナント)
     */
    private Integer scope;

    private Integer sort;

    private String icon;

    private String redirect;

    private String treePath;

    /**
     * ページキャッシュ (1:有効 0:無効)
     */
    private Integer keepAlive;

    /**
     * 子ルートが1つでも常に表示 (1:はい 0:いいえ)
     */
    private Integer alwaysShow;

    @TableField(updateStrategy = FieldStrategy.ALWAYS, typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> params;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Long tenantId;
}
