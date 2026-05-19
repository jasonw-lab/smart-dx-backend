package com.smartdx.property.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 物件登録受付レスポンス VO
 * LST-INT-01: 202 Accepted response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "物件登録受付レスポンス")
public class PropertyCreateAcceptedVO {

    @Schema(description = "ジョブ参照ID", example = "single-20260503-0001")
    private String jobRef;

    @Schema(description = "スコープ（固定値: draft）", example = "draft")
    private String scope;

    @Schema(description = "物件キー（UUID v4）", example = "ab12cd34-ef56-4789-abcd-1234567890ab")
    private String propertyKey;

    @Schema(description = "受付件数")
    private ReceivedCountVO receivedCount;

    @Schema(description = "修正再提出時: 受付時点の現行バージョン")
    private Integer expectedVersion;

    @Schema(description = "修正再提出時: 状態遷移情報")
    private StateTransitionVO stateTransition;
}
