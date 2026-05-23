package com.smartdx.system.captcha.service;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.smartdx.core.constant.RedisConstants;
import com.smartdx.core.enums.CaptchaTypeEnum;
import com.smartdx.core.result.ResultCode;
import com.smartdx.system.captcha.CaptchaProperties;
import com.smartdx.system.captcha.exception.CaptchaException;
import com.smartdx.system.captcha.model.CaptchaInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * 検証コードサービス
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CaptchaProperties captchaProperties;
    private final CodeGenerator codeGenerator;
    private final Font captchaFont;

    /**
     * 検証コードを生成
     */
    public CaptchaInfo generate() {
        String captchaType = captchaProperties.getType();
        int width = captchaProperties.getWidth();
        int height = captchaProperties.getHeight();
        int interfereCount = captchaProperties.getInterfereCount();
        int codeLength = captchaProperties.getCodeLength();

        AbstractCaptcha captcha;
        if (CaptchaTypeEnum.CIRCLE.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createCircleCaptcha(width, height, codeLength, interfereCount);
        } else if (CaptchaTypeEnum.GIF.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createGifCaptcha(width, height, codeLength);
        } else if (CaptchaTypeEnum.LINE.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createLineCaptcha(width, height, codeLength, interfereCount);
        } else if (CaptchaTypeEnum.SHEAR.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createShearCaptcha(width, height, codeLength, interfereCount);
        } else {
            captcha = CaptchaUtil.createLineCaptcha(width, height, codeLength, interfereCount);
        }

        captcha.setGenerator(codeGenerator);
        captcha.setTextAlpha(captchaProperties.getTextAlpha());
        captcha.setFont(captchaFont);

        String captchaCode = captcha.getCode();
        String imageBase64Data = captcha.getImageBase64Data();

        String captchaId = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(
                StrUtil.format(RedisConstants.Captcha.IMAGE_CODE, captchaId),
                captchaCode,
                captchaProperties.getExpireSeconds(),
                TimeUnit.SECONDS
        );

        return CaptchaInfo.builder()
                .captchaId(captchaId)
                .captchaBase64(imageBase64Data)
                .build();
    }

    /**
     * 検証コードを検証（失敗時例外をスロー）
     *
     * @param captchaId   検証コードID
     * @param captchaCode ユーザー入力の検証コード
     * @throws CaptchaException 検証コードエラーまたは期限切れ
     */
    public void validate(String captchaId, String captchaCode) {
        if (StrUtil.isBlank(captchaId) || StrUtil.isBlank(captchaCode)) {
            throw new CaptchaException(ResultCode.USER_VERIFICATION_CODE_ERROR);
        }

        String cacheKey = StrUtil.format(RedisConstants.Captcha.IMAGE_CODE, captchaId);
        String cachedCode = (String) redisTemplate.opsForValue().get(cacheKey);
        if (cachedCode == null) {
            throw new CaptchaException(ResultCode.USER_VERIFICATION_CODE_EXPIRED);
        }

        if (!codeGenerator.verify(cachedCode, captchaCode)) {
            throw new CaptchaException(ResultCode.USER_VERIFICATION_CODE_ERROR);
        }

        redisTemplate.delete(cacheKey);
    }
}
