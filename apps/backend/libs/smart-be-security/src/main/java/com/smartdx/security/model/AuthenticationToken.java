package com.smartdx.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 認証トークン
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationToken {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Integer expiresIn;
}
