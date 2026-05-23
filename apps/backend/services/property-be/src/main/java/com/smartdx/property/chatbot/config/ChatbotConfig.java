package com.smartdx.property.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Chatbot設定
 */
@Configuration
@ConfigurationProperties(prefix = "chatbot")
@Getter
@Setter
public class ChatbotConfig {

    /**
     * LLM設定
     */
    private LlmConfig llm = new LlmConfig();

    @Getter
    @Setter
    public static class LlmConfig {

        /**
         * LLM有効フラグ
         */
        private boolean enabled = false;

        /**
         * LLMプロバイダー: openai, anthropic, gemini, mock
         */
        private String provider = "mock";

        /**
         * APIキー
         */
        private String apiKey;

        /**
         * APIエンドポイント
         */
        private String apiEndpoint;

        /**
         * モデル名
         */
        private String model = "gpt-4o-mini";

        /**
         * タイムアウト（ミリ秒）
         */
        private long timeoutMs = 3000;

        /**
         * 日次呼び出し上限
         */
        private int dailyLimit = 100;
    }
}
