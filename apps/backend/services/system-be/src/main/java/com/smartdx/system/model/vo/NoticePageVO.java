package com.smartdx.system.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知ページVO
 */
@Schema(description = "通知ページVO")
@Data
public class NoticePageVO {

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "タイトル")
    private String title;

    @Schema(description = "発行状態")
    private Integer publishStatus;

    @Schema(description = "タイプ")
    private Integer type;

    @Schema(description = "発行者名")
    private String publisherName;

    @Schema(description = "優先度")
    private String level;

    @Schema(description = "発行日時")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime publishTime;

    @Schema(description = "既読状態")
    private Integer isRead;

    @Schema(description = "対象タイプ")
    private Integer targetType;

    @Schema(description = "作成日時")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createTime;

    @Schema(description = "取り消し日時")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime revokeTime;
}
