package com.smartdx.system.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ユーザー通知ページVO
 */
@Schema(description = "ユーザー通知ページVO")
@Data
public class UserNoticePageVO {

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "タイトル")
    private String title;

    @Schema(description = "タイプ")
    private Integer type;

    @Schema(description = "発行者名")
    private String publisherName;

    @Schema(description = "優先度")
    private String level;

    @Schema(description = "発行日時")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime publishTime;

    @Schema(description = "既読状態 (0: 未読, 1: 既読)")
    private Integer isRead;

    @Schema(description = "既読日時")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime readTime;
}
