package com.smartdx.security.token;

import com.smartdx.security.model.AuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * トークン管理インターフェース
 */
public interface TokenManager {

    AuthenticationToken generateToken(Authentication authentication);

    Authentication parseToken(String token);

    boolean validateToken(String token);

    boolean validateRefreshToken(String refreshToken);

    void invalidateToken(String token);

    void invalidateUserSessions(Long userId);

    AuthenticationToken refreshToken(String refreshToken);
}
