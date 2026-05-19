package com.smartdx.system.controller;

import com.smartdx.system.captcha.CaptchaProperties;
import com.smartdx.system.model.req.LoginReq;
import com.smartdx.system.model.vo.CaptchaVO;
import com.smartdx.system.service.AuthService;
import com.smartdx.core.result.Result;
import com.smartdx.security.model.AuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller
 */
@Tag(name = "01. Authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CaptchaProperties captchaProperties;

    @Operation(summary = "Get captcha image")
    @GetMapping("/captcha")
    public Result<CaptchaVO> getCaptcha() {
        CaptchaVO captcha = authService.getCaptcha();
        return Result.success(captcha);
    }

    @Operation(summary = "Login with username and password")
    @PostMapping("/login")
    public Result<AuthenticationToken> login(@RequestBody @Valid LoginReq request) {
        // Captcha validation based on configuration
        boolean hasCaptcha = request.getCaptchaId() != null && request.getCaptchaCode() != null;

        if (captchaProperties.isRequired()) {
            // Captcha is required - must be provided and valid
            if (!hasCaptcha) {
                return Result.failed("Captcha is required");
            }
            boolean valid = authService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
            if (!valid) {
                return Result.failed("Invalid captcha");
            }
        } else if (hasCaptcha) {
            // Captcha is optional but provided - still validate it
            boolean valid = authService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
            if (!valid) {
                return Result.failed("Invalid captcha");
            }
        }

        AuthenticationToken token = authService.login(
                request.getUsername(),
                request.getPassword(),
                request.getTenantId()
        );
        return Result.success(token);
    }

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh-token")
    public Result<AuthenticationToken> refreshToken(
            @Parameter(description = "Refresh token", example = "xxx.xxx.xxx")
            @RequestParam String refreshToken
    ) {
        AuthenticationToken token = authService.refreshToken(refreshToken);
        return Result.success(token);
    }
}
