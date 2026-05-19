package com.smartdx.property.chatbot.llm.impl;

import com.smartdx.property.chatbot.llm.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Mock LLMクライアント
 *
 * テスト・開発用のモック実装
 * chatbot.llm.provider=mock または未設定の場合に使用
 */
@Component
@ConditionalOnProperty(name = "chatbot.llm.provider", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockLlmClient implements LlmClient {

    @Override
    public LlmResponse complete(LlmRequest request) throws LlmException {
        log.info("MockLlmClient.complete called: userPrompt={}",
                truncate(request.getUserPrompt(), 100));

        // モックレスポンス: 常にRule-Based結果をそのまま返す想定なので
        // LLM補完が必要な曖昧表現に対してサンプルJSONを返す
        String mockJson = generateMockResponse(request.getUserPrompt());

        return LlmResponse.builder()
                .content(mockJson)
                .inputTokens(100)
                .outputTokens(50)
                .latencyMs(100)
                .model("mock-model")
                .finishReason("stop")
                .build();
    }

    @Override
    public CompletableFuture<LlmResponse> completeAsync(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> complete(request));
    }

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * モックレスポンス生成
     */
    private String generateMockResponse(String userPrompt) {
        // 曖昧表現に対する簡易的なモック応答
        if (userPrompt == null) {
            return "{}";
        }

        // "都心"が含まれていたら複数エリアを返す
        if (userPrompt.contains("都心")) {
            return """
                    {
                      "keyword": null,
                      "searchItems": {
                        "area": ["tokyo-shibuya", "tokyo-shinjuku", "tokyo-minato", "tokyo-chiyoda"],
                        "propertyType": null
                      },
                      "sortBy": "priceJpy",
                      "orderBy": "asc"
                    }
                    """;
        }

        // "安い"/"安め"が含まれていたら価格上限を設定
        if (userPrompt.contains("安い") || userPrompt.contains("安め") || userPrompt.contains("なるべく安く")) {
            return """
                    {
                      "keyword": null,
                      "searchItems": {
                        "priceJpyMax": 100000
                      },
                      "sortBy": "priceJpy",
                      "orderBy": "asc"
                    }
                    """;
        }

        // "広め"/"広い"が含まれていたらキーワードを設定
        if (userPrompt.contains("広め") || userPrompt.contains("広い")) {
            return """
                    {
                      "keyword": "広め",
                      "searchItems": {}
                    }
                    """;
        }

        // デフォルト: 空のJSON
        return "{}";
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
