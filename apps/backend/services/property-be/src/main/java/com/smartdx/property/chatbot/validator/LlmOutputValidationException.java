package com.smartdx.property.chatbot.validator;

/**
 * LLM出力バリデーション例外
 */
public class LlmOutputValidationException extends RuntimeException {

    public LlmOutputValidationException(String message) {
        super(message);
    }

    public LlmOutputValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
