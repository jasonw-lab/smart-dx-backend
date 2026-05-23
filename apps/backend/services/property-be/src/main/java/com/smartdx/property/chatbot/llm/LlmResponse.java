package com.smartdx.property.chatbot.llm;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * LLMレスポンス
 */
@Getter
@Setter
@Builder
public class LlmResponse {

    /**
     * 生成されたテキスト
     */
    private String content;

    /**
     * 入力トークン数
     */
    private int inputTokens;

    /**
     * 出力トークン数
     */
    private int outputTokens;

    /**
     * レイテンシ（ミリ秒）
     */
    private long latencyMs;

    /**
     * モデル名
     */
    private String model;

    /**
     * 終了理由
     */
    private String finishReason;
}
