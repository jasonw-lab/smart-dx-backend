package com.smartdx.system.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知詳細VO
 */
@Schema(description = "通知詳細VO")
@Data
public class NoticeDetailVO {

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "タイトル")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "タイプ")
    private Integer type;

    @Schema(description = "発行者名")
    private String publisherName;

    @Schema(description = "優先度 (L: 低, M: 中, H: 高)")
    private String level;

    @Schema(description = "発行状態")
    private Integer publishStatus;

    @Schema(description = "発行日時")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
}
