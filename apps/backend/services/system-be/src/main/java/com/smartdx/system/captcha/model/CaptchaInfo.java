package com.smartdx.system.captcha.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 検証コード情報
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "検証コード情報")
public class CaptchaInfo {

    @Schema(description = "検証コードキャッシュID")
    private String captchaId;

    @Schema(description = "検証コード画像Base64文字列")
    private String captchaBase64;
}
