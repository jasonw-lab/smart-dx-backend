package com.smartdx.system.captcha;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.awt.*;

/**
 * 検証コード設定プロパティ
 */
@Component
@ConfigurationProperties(prefix = "captcha")
@Data
public class CaptchaProperties {

    /**
     * ログイン時に検証コードが必要かどうか
     */
    private boolean required = true;

    /**
     * 検証コードタイプ: CIRCLE, GIF, LINE, SHEAR
     */
    private String type = "LINE";

    /**
     * 検証コード画像幅
     */
    private int width = 120;

    /**
     * 検証コード画像高さ
     */
    private int height = 40;

    /**
     * 検証コード長さ
     */
    private int codeLength = 4;

    /**
     * 検証コードタイプ: math, random
     */
    private String codeType = "random";

    /**
     * 干渉要素数
     */
    private int interfereCount = 20;

    /**
     * テキスト透明度 (0.0 - 1.0)
     */
    private float textAlpha = 0.8f;

    /**
     * フォント名
     */
    private String fontName = "Arial";

    /**
     * フォントサイズ
     */
    private int fontSize = 28;

    /**
     * フォントウェイト
     */
    private int fontWeight = Font.BOLD;

    /**
     * 検証コード有効期限（秒）
     */
    private long expireSeconds = 300;
}
