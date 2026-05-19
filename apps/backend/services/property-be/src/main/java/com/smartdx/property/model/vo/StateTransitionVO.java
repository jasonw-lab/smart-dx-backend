package com.smartdx.property.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 状態遷移 VO（修正再提出時）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "状態遷移情報")
public class StateTransitionVO {

    @Schema(description = "遷移元状態", example = "REJECTED")
    private String from;

    @Schema(description = "遷移先状態", example = "PENDING")
    private String to;
}
