package com.smartdx.system.captcha.config;

import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import com.smartdx.system.captcha.CaptchaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.awt.*;

/**
 * 検証コード自動設定
 */
@Configuration
public class CaptchaConfig {

    @Bean
    public CodeGenerator codeGenerator(CaptchaProperties captchaProperties) {
        String codeType = captchaProperties.getCodeType();
        int codeLength = captchaProperties.getCodeLength();
        if ("math".equalsIgnoreCase(codeType)) {
            return new MathGenerator(codeLength, false);
        } else if ("random".equalsIgnoreCase(codeType)) {
            return new RandomGenerator(codeLength);
        } else {
            return new RandomGenerator(codeLength);
        }
    }

    @Bean
    public Font captchaFont(CaptchaProperties captchaProperties) {
        String fontName = captchaProperties.getFontName();
        int fontSize = captchaProperties.getFontSize();
        int fontWeight = captchaProperties.getFontWeight();
        return new Font(fontName, fontWeight, fontSize);
    }
}
