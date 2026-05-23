package com.smartdx.system.captcha.exception;

import com.smartdx.core.result.ResultCode;
import lombok.Getter;

/**
 * 検証コード例外
 */
@Getter
public class CaptchaException extends RuntimeException {

    private final ResultCode resultCode;

    public CaptchaException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }
}
