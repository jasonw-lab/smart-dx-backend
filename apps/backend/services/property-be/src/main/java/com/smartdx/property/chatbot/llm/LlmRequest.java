package com.smartdx.property.chatbot.llm;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * LLMリクエスト
 */
@Getter
@Setter
@Builder
public class LlmRequest {

    /**
     * システムプロンプト
     */
    private String systemPrompt;

    /**
     * ユーザープロンプト
     */
    private String userPrompt;

    /**
     * 会話履歴
     */
    private List<ConversationMessage> conversationHistory;

    /**
     * 最大出力トークン数
     */
    @Builder.Default
    private int maxOutputTokens = 500;

    /**
     * 温度パラメータ（0.0-1.0）
     */
    @Builder.Default
    private double temperature = 0.1;

    /**
     * タイムアウト（ミリ秒）
     */
    @Builder.Default
    private long timeoutMs = 3000;
}
