package com.smartdx.system.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ログページVO
 */
@Schema(description = "ログページVO")
@Data
public class LogPageVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "モジュール")
    private String module;

    @Schema(description = "操作タイプ")
    private String actionType;

    @Schema(description = "操作タイトル")
    private String title;

    @Schema(description = "ログ内容")
    private String content;

    @Schema(description = "操作者ID")
    private Long operatorId;

    @Schema(description = "操作者名")
    private String operatorName;

    @Schema(description = "ステータス: 0=失敗 1=成功")
    private Integer status;

    @Schema(description = "リクエストURI")
    private String requestUri;

    @Schema(description = "リクエストメソッド")
    private String requestMethod;

    @Schema(description = "IPアドレス")
    private String ip;

    @Schema(description = "地域")
    private String region;

    @Schema(description = "デバイス")
    private String device;

    @Schema(description = "OS")
    private String os;

    @Schema(description = "ブラウザ")
    private String browser;

    @Schema(description = "実行時間(ミリ秒)")
    private Integer executionTime;

    @Schema(description = "エラーメッセージ")
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
