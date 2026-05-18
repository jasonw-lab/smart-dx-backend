package com.smartdx.core.exception;

import com.smartdx.core.result.IResultCode;
import org.slf4j.helpers.MessageFormatter;

/**
 * 業務例外
 */
public class BusinessException extends RuntimeException {

    private IResultCode resultCode;

    public BusinessException(IResultCode errorCode) {
        super(errorCode.getMsg());
        this.resultCode = errorCode;
    }

    public BusinessException(IResultCode errorCode, String message) {
        super(message);
        this.resultCode = errorCode;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }

    public BusinessException(String message, Object... args) {
        super(formatMessage(message, args));
    }

    private static String formatMessage(String message, Object... args) {
        return MessageFormatter.arrayFormat(message, args).getMessage();
    }

    public IResultCode getResultCode() {
        return resultCode;
    }
}
