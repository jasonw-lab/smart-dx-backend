package com.smartdx.retail.ai.llm.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kimi レスポンス DTO の JSON マッピングテスト。
 */
class KimiLlmClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void openAiCompatibleResponseMapsSnakeCaseFields() throws Exception {
        String json = """
                {
                  "choices": [
                    {
                      "message": {"role": "assistant", "content": "ok"},
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 12,
                    "completion_tokens": 8,
                    "total_tokens": 20
                  }
                }
                """;

        KimiLlmClient.OpenAiChatResponse response =
                objectMapper.readValue(json, KimiLlmClient.OpenAiChatResponse.class);

        assertThat(response.getChoices().get(0).getFinishReason()).isEqualTo("stop");
        assertThat(response.getUsage().getPromptTokens()).isEqualTo(12);
        assertThat(response.getUsage().getCompletionTokens()).isEqualTo(8);
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(20);
    }

    @Test
    void anthropicCompatibleResponseMapsSnakeCaseFields() throws Exception {
        String json = """
                {
                  "id": "msg_001",
                  "type": "message",
                  "role": "assistant",
                  "model": "kimi-for-coding",
                  "stop_reason": "end_turn",
                  "content": [{"type": "text", "text": "ok"}],
                  "usage": {
                    "input_tokens": 10,
                    "output_tokens": 7,
                    "total_tokens": 17
                  }
                }
                """;

        KimiLlmClient.AnthropicMessagesResponse response =
                objectMapper.readValue(json, KimiLlmClient.AnthropicMessagesResponse.class);

        assertThat(response.getStopReason()).isEqualTo("end_turn");
        assertThat(response.getUsage().getInputTokens()).isEqualTo(10);
        assertThat(response.getUsage().getOutputTokens()).isEqualTo(7);
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(17);
    }
}
