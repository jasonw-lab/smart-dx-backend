package com.smartdx.retail.ai.llm;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * LLM request.
 *
 * @author jason.w
 */
@Getter
@Setter
@Builder
public class LlmRequest {

    /**
     * System prompt.
     */
    private String systemPrompt;

    /**
     * User prompt.
     */
    private String userPrompt;

    /**
     * Conversation history.
     */
    private List<ConversationMessage> conversationHistory;

    /**
     * Maximum output tokens.
     */
    @Builder.Default
    private int maxOutputTokens = 500;

    /**
     * Temperature (0.0 - 1.0).
     */
    @Builder.Default
    private double temperature = 0.1;
}
