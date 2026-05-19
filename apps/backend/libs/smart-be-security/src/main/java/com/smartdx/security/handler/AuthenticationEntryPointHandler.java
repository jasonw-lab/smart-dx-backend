package com.smartdx.security.handler;

import com.smartdx.core.result.ResponseWriter;
import com.smartdx.core.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * 認証失敗ハンドラー
 */
@Slf4j
public class AuthenticationEntryPointHandler implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.warn("認証失敗: {} - {}", authException.getMessage(), request.getRequestURI());
        ResponseWriter.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
    }
}
