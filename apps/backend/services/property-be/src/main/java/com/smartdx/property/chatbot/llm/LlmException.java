package com.smartdx.property.chatbot.llm;

import lombok.Getter;

/**
 * LLM例外
 */
@Getter
public class LlmException extends RuntimeException {

    private final LlmErrorCode errorCode;

    public enum LlmErrorCode {
        /**
         * タイムアウト
         */
        TIMEOUT,
        /**
         * レート制限
         */
        RATE_LIMITED,
        /**
         * 不正なレスポンス
         */
        INVALID_RESPONSE,
        /**
         * API エラー
         */
        API_ERROR,
        /**
         * サービス利用不可
         */
        UNAVAILABLE
    }

    public LlmException(LlmErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LlmException(LlmErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
