package com.smartdx.system.captcha;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Captcha configuration properties
 */
@Component
@ConfigurationProperties(prefix = "captcha")
@Data
public class CaptchaProperties {

    /**
     * Whether captcha is required for login.
     * Set to false only for dev/test profiles.
     */
    private boolean required = true;

    /**
     * Captcha image width
     */
    private int width = 120;

    /**
     * Captcha image height
     */
    private int height = 40;

    /**
     * Captcha code length
     */
    private int codeLength = 4;

    /**
     * Captcha expiration in seconds
     */
    private long expireSeconds = 300;
}
