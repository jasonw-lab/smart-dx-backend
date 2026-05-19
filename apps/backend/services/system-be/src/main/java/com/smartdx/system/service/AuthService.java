package com.smartdx.system.service;

import com.smartdx.system.model.vo.CaptchaVO;
import com.smartdx.security.model.AuthenticationToken;

/**
 * Authentication service interface
 */
public interface AuthService {

    /**
     * Login with username and password
     *
     * @param username Username
     * @param password Password
     * @return Authentication token
     */
    default AuthenticationToken login(String username, String password) {
        return login(username, password, null);
    }

    /**
     * Login with username and password for a specific tenant
     *
     * @param username Username
     * @param password Password
     * @param tenantId Tenant ID (optional, for multi-tenant mode)
     * @return Authentication token
     */
    AuthenticationToken login(String username, String password, Long tenantId);

    /**
     * Logout the current user
     */
    void logout();

    /**
     * Get captcha image
     *
     * @return Captcha information
     */
    CaptchaVO getCaptcha();

    /**
     * Verify captcha code
     *
     * @param captchaId Captcha cache ID
     * @param code      Captcha code
     * @return true if valid
     */
    boolean verifyCaptcha(String captchaId, String code);

    /**
     * Refresh access token
     *
     * @param refreshToken Refresh token
     * @return New authentication token
     */
    AuthenticationToken refreshToken(String refreshToken);
}
