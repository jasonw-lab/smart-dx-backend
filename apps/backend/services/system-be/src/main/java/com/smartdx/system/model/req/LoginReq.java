package com.smartdx.system.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login request
 */
@Schema(description = "Login request")
@Data
public class LoginReq {

    @Schema(description = "Username", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotBlank(message = "Username is required")
    private String username;

    @Schema(description = "Password", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "Password is required")
    private String password;

    @Schema(description = "Captcha cache ID", example = "captcha_id_123")
    private String captchaId;

    @Schema(description = "Captcha code", example = "1234")
    private String captchaCode;

    @Schema(description = "Tenant ID (optional for multi-tenant mode)", example = "1")
    private Long tenantId;
}
