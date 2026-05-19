package com.smartdx.property.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 審査情報
 */
@Getter
@Setter
@Schema(description = "審査情報")
public class PropertyReviewVO {

    @Schema(description = "審査ステータス: PENDING, APPROVED, REJECTED")
    private String status;

    @Schema(description = "推奨ランク（APPROVED 時のみ必須）")
    private String priorityRank;

    @Schema(description = "審査コメント")
    private String comment;

    @Schema(description = "審査者ユーザー ID")
    private String reviewerId;

    @Schema(description = "審査者表示名")
    private String reviewerName;

    @Schema(description = "審査日時（ISO 8601）")
    private String reviewedAt;
}
