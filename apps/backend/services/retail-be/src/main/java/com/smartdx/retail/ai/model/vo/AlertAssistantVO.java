package com.smartdx.retail.ai.model.vo;

import com.smartdx.retail.model.vo.AlertPageVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI alert assistant response.
 *
 * @author jason.w
 */
@Schema(description = "AI alert assistant response")
@Data
public class AlertAssistantVO {

    @Schema(description = "Generated summary")
    private String summary;

    @Schema(description = "Today's unresolved alerts in priority order")
    private List<AlertPageVO> alerts;

    @Schema(description = "Whether an LLM was used")
    private boolean llmUsed;

    @Schema(description = "LLM model name when used")
    private String llmModel;

    @Schema(description = "Whether the response is a fallback summary")
    private boolean fallback;
}
