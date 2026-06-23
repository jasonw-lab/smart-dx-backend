package com.smartdx.retail.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI Alert Assistant configuration.
 *
 * @author jason.w
 */
@Configuration
@ConfigurationProperties(prefix = "retail.ai")
@Getter
@Setter
public class AlertAssistantConfig {

    /**
     * LLM settings.
     */
    private LlmConfig llm = new LlmConfig();

    @Getter
    @Setter
    public static class LlmConfig {

        /**
         * Whether the LLM integration is enabled.
         */
        private boolean enabled = true;

        /**
         * Provider: openai, anthropic, gemini, mock.
         */
        private String provider = "gemini";

        /**
         * API key.
         */
        private String apiKey;

        /**
         * Model name.
         */
        private String model = "gemini-2.0-flash-001";

        /**
         * Timeout in milliseconds.
         */
        private long timeoutMs = 5000;
    }
}
