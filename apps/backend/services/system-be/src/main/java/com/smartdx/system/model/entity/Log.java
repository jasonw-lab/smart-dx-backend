package com.smartdx.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * システムログエンティティ
 */
@Data
@TableName("sys_log")
public class Log {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * モジュール
     */
    private String module;

    /**
     * 操作タイプ
     */
    @TableField("action_type")
    private String actionType;

    /**
     * 操作タイトル
     */
    private String title;

    /**
     * ログ内容
     */
    private String content;

    /**
     * 操作者ID
     */
    private Long operatorId;

    /**
     * 操作者名
     */
    private String operatorName;

    /**
     * リクエストURI
     */
    private String requestUri;

    /**
     * リクエストメソッド
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * IPアドレス
     */
    private String ip;

    /**
     * 都道府県
     */
    private String province;

    /**
     * 市区町村
     */
    private String city;

    /**
     * デバイス
     */
    private String device;

    /**
     * OS
     */
    private String os;

    /**
     * ブラウザ
     */
    private String browser;

    /**
     * ステータス: 0=失敗 1=成功
     */
    private Integer status;

    /**
     * エラーメッセージ
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 実行時間(ミリ秒)
     */
    private Integer executionTime;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private Long tenantId;
}
