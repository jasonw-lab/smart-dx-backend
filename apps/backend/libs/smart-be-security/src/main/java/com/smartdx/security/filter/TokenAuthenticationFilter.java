package com.smartdx.security.filter;

import cn.hutool.core.util.StrUtil;
import com.smartdx.core.result.ResponseWriter;
import com.smartdx.core.result.ResultCode;
import com.smartdx.security.constant.SecurityConstants;
import com.smartdx.security.token.TokenManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Token認証フィルター
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenManager tokenManager;

    public TokenAuthenticationFilter(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 既に認証済みの場合はスキップ
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawToken = resolveToken(request);

        try {
            if (StrUtil.isNotBlank(rawToken)) {
                boolean isValidToken = tokenManager.validateToken(rawToken);
                if (!isValidToken) {
                    ResponseWriter.writeError(response, ResultCode.ACCESS_TOKEN_INVALID);
                    return;
                }

                Authentication authentication = tokenManager.parseToken(rawToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            ResponseWriter.writeError(response, ResultCode.ACCESS_TOKEN_INVALID);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isNotBlank(authorizationHeader)
                && authorizationHeader.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            return authorizationHeader.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length());
        }
        return null;
    }
}
