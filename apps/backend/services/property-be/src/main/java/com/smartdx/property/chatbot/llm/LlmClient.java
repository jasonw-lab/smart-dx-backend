package com.smartdx.property.chatbot.llm;

import java.util.concurrent.CompletableFuture;

/**
 * LLMクライアントインターフェース
 *
 * プロバイダー差し替え可能な抽象化レイヤー
 */
public interface LlmClient {

    /**
     * プロンプトを送信し、レスポンスを取得
     *
     * @param request LLMリクエスト
     * @return LLMレスポンス
     * @throws LlmException LLM呼び出しに失敗した場合
     */
    LlmResponse complete(LlmRequest request) throws LlmException;

    /**
     * 非同期でプロンプトを送信
     *
     * @param request LLMリクエスト
     * @return CompletableFuture<LlmResponse>
     */
    CompletableFuture<LlmResponse> completeAsync(LlmRequest request);

    /**
     * プロバイダー名を取得
     *
     * @return プロバイダー名（openai, anthropic, gemini, mock）
     */
    String getProviderName();

    /**
     * ヘルスチェック
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();
}
