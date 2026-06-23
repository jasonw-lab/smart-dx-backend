package com.smartdx.retail.ai.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI alert assistant request.
 *
 * @author jason.w
 */
@Schema(description = "AI alert assistant request")
@Data
public class AlertAssistantReq {

    @Schema(description = "User message", example = "今日対応すべき優先アラートは？")
    private String message;
}
