package com.smartdx.retail.ai.llm;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * LLM response.
 *
 * @author jason.w
 */
@Getter
@Setter
@Builder
public class LlmResponse {

    /**
     * Generated text.
     */
    private String content;

    /**
     * Input token count.
     */
    private int inputTokens;

    /**
     * Output token count.
     */
    private int outputTokens;

    /**
     * Latency in milliseconds.
     */
    private long latencyMs;

    /**
     * Model name.
     */
    private String model;

    /**
     * Finish reason.
     */
    private String finishReason;
}
