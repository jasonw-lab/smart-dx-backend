package com.smartdx.retail.ai.llm;

import lombok.Getter;

/**
 * LLM exception.
 *
 * @author jason.w
 */
@Getter
public class LlmException extends RuntimeException {

    private final LlmErrorCode errorCode;

    public enum LlmErrorCode {
        /**
         * Timeout.
         */
        TIMEOUT,
        /**
         * Rate limited.
         */
        RATE_LIMITED,
        /**
         * Invalid response.
         */
        INVALID_RESPONSE,
        /**
         * API error.
         */
        API_ERROR,
        /**
         * Service unavailable.
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
