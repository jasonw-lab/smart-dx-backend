package com.smartdx.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Captcha response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Captcha information")
public class CaptchaVO {

    @Schema(description = "Captcha cache ID")
    private String captchaId;

    @Schema(description = "Captcha image as Base64")
    private String captchaBase64;
}
